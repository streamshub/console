package com.github.streamshub.console.api.security;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.UriBuilder;

import org.jboss.logging.Logger;

import com.github.streamshub.console.api.support.Holder;
import com.github.streamshub.console.config.ConsoleConfig;

import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.quarkus.oidc.runtime.OidcUtils;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;

/**
 * Quarkus {@link OidcResponseFilter} that processes every OIDC discovery
 * ({@code /.well-known/openid-configuration}) response received by this
 * application, whether triggered by the server-side OIDC tenant (for browser
 * login) or by a REST client making its own OIDC discovery call.
 *
 * <p>Two responsibilities:
 * <ol>
 *   <li><b>End-session endpoint capture</b> — when the response belongs to the
 *       global Console security OIDC tenant and contains an
 *       {@code end_session_endpoint} field, the value is stored in
 *       {@link #endSessionEndpoint} so that the logout flow can redirect the
 *       user's browser to the IdP to terminate their session there.  When the
 *       field is absent the holder is reset to empty.</li>
 *   <li><b>Endpoint override</b> — when
 *       {@link com.github.streamshub.console.config.security.OidcConfig#isOverrideDiscoveredEndpoints()}
 *       is {@code true} (set automatically when Dex is the OIDC provider), the
 *       {@code token_endpoint} and {@code jwks_uri} fields in the discovery
 *       document are rewritten so that their scheme, host, and port match the
 *       configured {@code authServerUrl}.  This lets the Console API reach Dex
 *       over its internal cluster-DNS name (e.g.
 *       {@code https://console-1-dex-service.ns.svc.cluster.local:5556/dex})
 *       regardless of what Dex itself advertises in those fields.</li>
 * </ol>
 *
 * <p>Both responsibilities are applied only when the response's content type
 * is {@code application/json} and the request belongs to the global Console
 * security OIDC tenant ({@link #isGlobalSecurityTenant}).  Responses produced
 * by REST-client OIDC discovery calls for other tenants are therefore ignored.
 *
 * @see <a href=
 *      "https://openid.net/specs/openid-connect-rpinitiated-1_0.html#OPMetadata">OpenID
 *      Provider Discovery Metadata — end_session_endpoint</a>
 */
@ApplicationScoped
@OidcEndpoint(value = OidcEndpoint.Type.DISCOVERY)
public class OidcDiscoveryResponseFilter implements OidcResponseFilter {

    public static final String OIDC_END_SESSION_ENDPOINT = "oidcEndSessionEndpoint";

    @Inject
    Logger logger;

    @Inject
    ConsoleConfig consoleConfig;

    Holder<String> endSessionEndpoint = Holder.empty();

    /**
     * Produces a {@link Supplier} of the current {@link #endSessionEndpoint} holder
     * rather than producing the holder directly. CDI producers capture the value at
     * injection time; if the {@link Holder} instance itself were produced, consumers
     * would always see the initial {@code Holder.empty()} value and would never
     * observe updates written by {@link #filter}. Wrapping in a {@link Supplier}
     * defers the read to call time, so consumers always get the most-recently
     * assigned holder.
     */
    @Produces
    @ApplicationScoped
    @Named(OIDC_END_SESSION_ENDPOINT)
    Supplier<Holder<String>> endSessionEndpointSupplier() {
        return () -> this.endSessionEndpoint;
    }

    @Override
    public Uni<Void> filter(OidcResponseFilterContext responseContext) {
        String contentType = responseContext.responseHeaders().get("Content-Type");

        if ("application/json".equals(contentType) && isGlobalSecurityTenant(responseContext)) {
            var discovery = getDiscoveryData(responseContext);
            var endpoint = discovery.getString("end_session_endpoint");

            if (endpoint != null) {
                logger.debugf("Using OIDC end_session_endpoint: %s", endpoint);
                this.endSessionEndpoint = Holder.of(endpoint);
            } else {
                logger.debug("OIDC end_session_endpoint not available");
                this.endSessionEndpoint = Holder.empty();
            }
        }

        return Uni.createFrom().voidItem();
    }

    /**
     * Parses the discovery response body and, when
     * {@link com.github.streamshub.console.config.security.OidcConfig#isOverrideDiscoveredEndpoints()}
     * is enabled, rewrites the {@code token_endpoint} and {@code jwks_uri} fields
     * in-place so they share the scheme, userinfo, host, and port of the configured
     * {@code authServerUrl}.  The rewritten buffer is pushed back into
     * {@code responseContext} so Quarkus OIDC uses the overridden URLs for all
     * subsequent token and key-set requests.
     */
    private JsonObject getDiscoveryData(OidcResponseFilterContext responseContext) {
        var oidc = consoleConfig.getSecurity().getOidc();
        JsonObject discovery = responseContext.responseBody().toJsonObject();

        if (Boolean.TRUE.equals(oidc.isOverrideDiscoveredEndpoints())) {
            var authServerUrl = URI.create(oidc.getAuthServerUrl());

            for (String key : List.of("token_endpoint", "jwks_uri")) {
                String discoveredValue = discovery.getString(key);

                if (discoveredValue == null) {
                    // Not all providers advertise every endpoint; nothing to override
                    logger.debugf("OIDC discovery document has no '%s' to override", key);
                    continue;
                }

                var discoveredUri = URI.create(discoveredValue);
                var overriddenUri = UriBuilder
                        .fromUri(discoveredUri)
                        .scheme(authServerUrl.getScheme())
                        .userInfo(authServerUrl.getUserInfo())
                        .host(authServerUrl.getHost())
                        .port(authServerUrl.getPort())
                        .build()
                        .toString();

                logger.debugf("Discovered %s OIDC endpoint '%s' overridden to '%s'",
                        key, discoveredUri, overriddenUri);

                discovery.put(key, overriddenUri);
            }

            responseContext.responseBody(Buffer.newInstance(discovery.toBuffer()));
        }

        return discovery;
    }

    /**
     * Returns {@code true} only when the response belongs to the global Console
     * OIDC tenant, identified by comparing the {@link OidcUtils#TENANT_ID_ATTRIBUTE}
     * on the request context against the tenant ID configured in
     * {@link com.github.streamshub.console.config.security.OidcConfig#getTenantId()}.
     * This guards both responsibilities (end-session capture and endpoint override)
     * so that REST-client OIDC discovery calls for unrelated tenants are left
     * untouched.
     */
    private boolean isGlobalSecurityTenant(OidcResponseFilterContext responseContext) {
        var oidc = consoleConfig.getSecurity().getOidc();

        if (oidc == null) {
            return false;
        }

        var tenant = responseContext.requestProperties().get(OidcUtils.TENANT_ID_ATTRIBUTE);
        return Objects.equals(tenant, oidc.getTenantId());
    }
}
