package com.github.streamshub.console.api.security;

import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.streamshub.console.config.ConsoleConfig;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

/**
 * This class is discovered and used by the Quarkus OIDC framework. The purpose
 * is to create an OIDC tenant from the ConsoleConfig (sourced from YAML) that
 * is provided to the console by the user directly or via the operator.
 */
@ApplicationScoped
public class OidcTenantConfigResolver implements TenantConfigResolver {

    @Inject
    ConsoleConfig consoleConfig;

    OidcTenantConfig oidcConfig;

    @PostConstruct
    void initialize() {
        oidcConfig = new OidcTenantConfig();
        var oidc = consoleConfig.getSecurity().getOidc();

        oidcConfig.setTenantId(oidc.getTenantId());
        oidcConfig.setDiscoveryEnabled(true);
        oidcConfig.setAuthServerUrl(oidc.getAuthServerUrl());
        oidcConfig.setRoles(OidcTenantConfig.Roles.fromClaimPath(List.of("groups")));

        if (oidc.getIssuer() != null) {
            oidcConfig.getToken().setIssuer(oidc.getIssuer());
        }
    }

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext routingContext,
            OidcRequestContext<OidcTenantConfig> requestContext) {
        return Uni.createFrom().item(oidcConfig);
    }

}
