package com.github.streamshub.console.api.security;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.jwt.JsonWebToken;

import com.github.streamshub.console.config.ConsoleConfig;

import io.quarkus.oidc.OidcSession;
import io.quarkus.security.identity.SecurityIdentity;
import io.smallrye.mutiny.Uni;

@Path("session")
public class SessionResource {

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    OidcSession oidcSession;

    @Inject
    SecurityIdentity identity;

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
        return Response.seeOther(redirectValidator.safeRedirectUri(redirectUri, uriInfo)).build();
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
            fullName = Optional.ofNullable(identity.getAttribute(IdentitySupport.FULL_NAME));
        }

        // this is not the `name` claim when the principal is a JWT
        properties.put("username", principal.getName());
        fullName.ifPresent(fn -> properties.put(IdentitySupport.FULL_NAME, String.valueOf(fn)));
        properties.put("anonymous", identity.isAnonymous());

        return Response.ok(properties).build();
    }

    /**
     * Logout of the application and redirect to the root URL. Note,
     * this endpoint is overridden by the OIDC RP-initiated logout handled
     * by Quarkus OIDC when the OIDC provider advertises an `end_session_endpoint`
     * URI.
     */
    @GET
    @Path("logout")
    public Uni<Response> logout(UriInfo uriInfo) {
        var response = Response
                .seeOther(redirectValidator.safeRedirectUri("/", uriInfo))
                .build();

        if (oidcEnabled()) {
            return oidcSession.logout().replaceWith(response);
        }

        return Uni.createFrom().item(response);
    }

    private Optional<String> nameClaim(JsonWebToken token) {
        return token.claim("name");
    }
}
