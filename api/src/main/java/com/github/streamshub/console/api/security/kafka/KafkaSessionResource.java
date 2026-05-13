package com.github.streamshub.console.api.security.kafka;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.github.streamshub.console.api.security.IdentitySupport;
import com.github.streamshub.console.api.security.RedirectUriValidator;

import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.FormAuthenticationMechanism;

@Path("kafkas/{clusterId}/session")
public class KafkaSessionResource {

    @Parameter(description = "Cluster identifier")
    @PathParam("clusterId")
    String clusterId;

    @Inject
    SecurityIdentity identity;

    @Inject
    RedirectUriValidator redirectValidator;

    @GET
    @Path("user")
    public Response getCurrentKafkaUser() {
        Map<String, Object> properties = LinkedHashMap.newLinkedHashMap(3);
        var principal = identity.getPrincipal();
        Optional<String> fullName = Optional
                .ofNullable(identity.getAttribute(IdentitySupport.FULL_NAME));

        // this is not the `name` claim when the principal is a JWT
        properties.put("username", principal.getName());
        fullName.ifPresent(fn -> properties.put(IdentitySupport.FULL_NAME, String.valueOf(fn)));
        properties.put("anonymous", identity.isAnonymous());

        return Response.ok(properties).build();
    }

    @GET
    @Path("logout")
    public Response kafkaLogout(@QueryParam("redirect_uri") String redirectUri, UriInfo uriInfo) {
        // Validate and sanitize the redirect URI to prevent open redirect vulnerabilities
        URI safeRedirectUri = safeRedirectUri(redirectUri, uriInfo);
        FormAuthenticationMechanism.logout(identity);
        return Response.seeOther(safeRedirectUri).build();
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
