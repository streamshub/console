package com.github.streamshub.console.api.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.logging.Logger;

import com.github.streamshub.console.api.support.Holder;

import io.quarkus.arc.Unremovable;
import io.quarkus.oidc.common.OidcEndpoint;
import io.quarkus.oidc.common.OidcResponseFilter;

/**
 * This filter will be invoked when the OIDC discovery end point response is
 * returned from the identity provider. If the discovery response includes an
 * end session end point, it will be set in the {@link endSessionEndpoint}
 * holder so that it can be used during the logout process to terminate the
 * user's session with the IdP.
 *
 * @see <a href=
 *      "https://openid.net/specs/openid-connect-rpinitiated-1_0.html#OPMetadata">OpenID
 *      Provider Discovery Metadata</a>
 */
@ApplicationScoped
@Unremovable
@OidcEndpoint(value = OidcEndpoint.Type.DISCOVERY)
public class OidcDiscoveryResponseFilter implements OidcResponseFilter {

    @Inject
    Logger logger;

    @Produces
    @RequestScoped
    @Named("oidcEndSessionEndpoint")
    Holder<String> endSessionEndpoint = Holder.empty();

    @Override
    public void filter(OidcResponseContext responseContext) {
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
    }
}
