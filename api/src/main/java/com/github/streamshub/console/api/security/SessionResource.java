package com.github.streamshub.console.api.security;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import com.github.streamshub.console.api.support.Holder;
import com.github.streamshub.console.config.ConsoleConfig;

import io.quarkus.oidc.OidcSession;
import io.quarkus.security.identity.SecurityIdentity;

@Path("session")
public class SessionResource {

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    OidcSession oidcSession;

    @Inject
    SecurityIdentity identity;

    @Inject
    @Named("oidcEndSessionEndpoint")
    Holder<String> endSessionEndpoint;

    boolean oidcEnabled() {
        return Objects.nonNull(consoleConfig.getSecurity().getOidc());
    }

    /**
     * Login end point invoked by the UI when it detects that a session is required
     * via special HTTP status 499.
     *
     * This end point is protected, so accessing it triggers OIDC flow. After
     * successful authentication, it redirects back to React application.
     *
     * Note that the OIDC flow is only triggered when the API is accessed not by a
     * JavaScript fetch. This is configured by the {@link OidcTenantConfigResolver}.
     */
    @GET
    @Path("login")
    public Response login(@QueryParam("redirect_uri") String redirectUri) {
        return Response.seeOther(URI.create(redirectUri)).build();
    }

    @GET
    @Path("user")
    public Response getCurrentUser() {
        Map<String, Object> properties = LinkedHashMap.newLinkedHashMap(2);
        Optional<String> fullName;

        if (oidcEnabled()) {
            var token = oidcSession.getIdToken();
            properties.put("username", token.getName()); // this is not the `name` claim
            fullName = token.claim("name");
        } else {
            properties.put("username", identity.getPrincipal().getName());
            fullName = Optional.ofNullable(identity.getAttribute(ConsoleAuthenticationMechanism.FULL_NAME));
        }

        fullName.ifPresent(fn -> properties.put(ConsoleAuthenticationMechanism.FULL_NAME, String.valueOf(fn)));
        properties.put("anonymous", identity.isAnonymous());

        return Response.ok(properties).build();
    }

    @GET
    @Path("logout")
    public Response logout(@QueryParam("redirect_uri") String redirectUri) {
        if (oidcEnabled()) {
            oidcSession.logout().await().indefinitely();
        }

        URI logoutUri = endSessionEndpoint
            .map(endpoint -> {
                /*
                 * If the end_session_endpoint is available, redirect to allow the IdP
                 * to terminate the session fully.
                 */
                String redirectEncoded = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);
                var builder = UriBuilder.fromUri(endpoint);
                builder.queryParam("post_logout_redirect_uri", redirectEncoded);
                builder.queryParam("id_token_hint", oidcSession.getIdToken().getRawToken());
                return builder.build();
            })
            .orElseGet(() -> URI.create(redirectUri));

        return Response.seeOther(logoutUri).build();
    }
}
