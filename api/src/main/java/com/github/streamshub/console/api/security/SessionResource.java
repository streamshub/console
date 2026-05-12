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
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.jwt.JsonWebToken;

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

    @Inject
    RedirectUriValidator redirectValidator;

    boolean oidcEnabled() {
        return Objects.nonNull(consoleConfig.getSecurity().getOidc());
    }

    /**
     * Login end point invoked by the UI when it detects that a session is required
     * via special HTTP status 499.
     *
     * This end point is protected, so accessing it triggers OIDC flow. After
     * successful authentication, Quarkus will redirect the client to this endpoint
     * with the original request (restorePathAfterRedirect=true in the tenant 
     * configuration) and this operation redirects them back to a path handled by
     * the React application.
     *
     * Note that the OIDC flow is only triggered when the API is accessed not by a
     * JavaScript fetch. This is configured by the {@link OidcTenantConfigResolver}.
     */
    @GET
    @Path("login")
    public Response login(@QueryParam("redirect_uri") String redirectUri, UriInfo uriInfo) {
        return Response.seeOther(safeRedirectUri(redirectUri, uriInfo)).build();
    }

    @GET
    @Path("user")
    public Response getCurrentUser() {
        Map<String, Object> properties = LinkedHashMap.newLinkedHashMap(3);
        var principal = identity.getPrincipal();
        Optional<String> fullName;

        if (oidcEnabled()) {
            fullName = Optional.of(principal)
                .filter(JsonWebToken.class::isInstance)
                .map(JsonWebToken.class::cast)
                .flatMap(this::nameClaim)
                .or(() -> nameClaim(oidcSession.getIdToken()));
        } else {
            fullName = Optional.ofNullable(identity.getAttribute(ConsoleAuthenticationMechanism.FULL_NAME));
        }

        // this is not the `name` claim when the principal is a JWT
        properties.put("username", principal.getName());
        fullName.ifPresent(fn -> properties.put(ConsoleAuthenticationMechanism.FULL_NAME, String.valueOf(fn)));
        properties.put("anonymous", identity.isAnonymous());

        return Response.ok(properties).build();
    }

    @GET
    @Path("logout")
    public Response logout(@QueryParam("redirect_uri") String redirectUri, UriInfo uriInfo) {
        // Validate and sanitize the redirect URI to prevent open redirect vulnerabilities
        URI safeRedirectUri = safeRedirectUri(redirectUri, uriInfo);

        if (oidcEnabled()) {
            oidcSession.logout().await().indefinitely();
        }

        URI logoutUri = endSessionEndpoint
            .map(endpoint -> {
                /*
                 * If the end_session_endpoint is available, redirect to allow the IdP
                 * to terminate the session fully.
                 */
                String redirectEncoded = URLEncoder.encode(
                        safeRedirectUri.toString(),
                        StandardCharsets.UTF_8
                );
                var builder = UriBuilder.fromUri(endpoint);
                builder.queryParam("post_logout_redirect_uri", redirectEncoded);
                // ID token won't be present for service calls, only web-app interactions
                // where session is maintained via cookie.
                Optional.ofNullable(oidcSession.getIdToken().getRawToken())
                    .ifPresent(t -> builder.queryParam("id_token_hint", t));
                return builder.build();
            })
            .orElse(safeRedirectUri);

        return Response.seeOther(logoutUri).build();
    }

    private Optional<String> nameClaim(JsonWebToken token) {
        return token.claim("name");
    }

    private URI safeRedirectUri(String redirectUri, UriInfo uriInfo) {
        // Validate and sanitize the redirect URI to prevent open redirect vulnerabilities
        String safePath = redirectValidator.validateAndSanitize(redirectUri);

        // Use the sanitized path together with the scheme, host, and port of the
        // request to this resource.
        return UriBuilder.fromUri(uriInfo.getRequestUri())
            .replacePath(safePath)
            .replaceQuery(null)
            .fragment(null)
            .build();
    }
}
