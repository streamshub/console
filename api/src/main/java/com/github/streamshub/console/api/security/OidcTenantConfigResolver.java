package com.github.streamshub.console.api.security;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.github.streamshub.console.api.support.TrustStoreSupport;
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
    Logger logger;

    @Inject
    TrustStoreSupport trustStores;

    @Inject
    ConsoleConfig consoleConfig;

    OidcTenantConfig oidcConfig;

    @PostConstruct
    void initialize() {
        var oidc = consoleConfig.getSecurity().getOidc();

        final var builder = OidcTenantConfig.builder()
                .tenantId(oidc.getTenantId())
                .discoveryEnabled(true)
                .authServerUrl(oidc.getAuthServerUrl())
                .roles()
                    .roleClaimPath(oidc.getRoleClaimPath())
                .end();

        if (oidc.getIssuer() != null) {
            builder.token()
                .issuer(oidc.getIssuer())
                .end();
        }

        trustStores.configureTruststoreFile(oidc, null, builder);
        oidcConfig = builder.build();
    }

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext routingContext,
            OidcRequestContext<OidcTenantConfig> requestContext) {
        return Uni.createFrom().item(oidcConfig);
    }

}
