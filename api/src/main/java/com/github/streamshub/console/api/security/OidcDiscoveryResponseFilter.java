package com.github.streamshub.console.api.security;

import java.util.function.Supplier;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.logging.Logger;

import com.github.streamshub.console.api.support.Holder;

import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcResponseFilter;
import io.smallrye.mutiny.Uni;

/**
 * This filter will be invoked when the OIDC discovery end point response is
 * returned from the identity provider. If the discovery response includes an
 * end session end point, it will be set in the {@link #endSessionEndpoint}
 * holder so that it can be used during the logout process to terminate the
 * user's session with the IdP.
 *
 * @see <a href=
 *      "https://openid.net/specs/openid-connect-rpinitiated-1_0.html#OPMetadata">OpenID
 *      Provider Discovery Metadata</a>
 */
@ApplicationScoped
@OidcEndpoint(value = OidcEndpoint.Type.DISCOVERY)
public class OidcDiscoveryResponseFilter implements OidcResponseFilter {

    public static final String OIDC_END_SESSION_ENDPOINT = "oidcEndSessionEndpoint";

    @Inject
    Logger logger;

    Holder<String> endSessionEndpoint = Holder.empty();

    /**
     * Produces a {@link Supplier} rather than the {@link Holder} directly, so that
     * every call to {@link Supplier#get()} re-reads the current field value from
     * this bean. If the {@link Holder} itself were produced, CDI would capture the
     * initial {@code Holder.empty()} instance at injection time and subsequent
     * field reassignments in {@link #filter} would never be visible to consumers.
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

        if ("application/json".equals(contentType)) {
            var endpoint = responseContext.responseBody().toJsonObject().getString("end_session_endpoint");

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
}
