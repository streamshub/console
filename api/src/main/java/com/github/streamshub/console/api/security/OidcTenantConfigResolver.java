package com.github.streamshub.console.api.security;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.github.streamshub.console.api.support.TrustStoreSupport;
import com.github.streamshub.console.config.ConsoleConfig;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.oidc.runtime.OidcTenantConfig.ApplicationType;
import io.quarkus.oidc.runtime.OidcTenantConfig.TokenStateManager.Strategy;
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
                .clientId(oidc.getClientId())
                .credentials()
                    .clientSecret(oidc.getClientSecret())
                .end()
                .applicationType(ApplicationType.HYBRID)
                .authServerUrl(oidc.getAuthServerUrl())
                .discoveryEnabled(true)
                .authentication()
                    .redirectPath("/")
                    .restorePathAfterRedirect(true)
                    // Do not redirect to the IdP for JavaScript requests. They are identified by
                    // requests from the React UI with header `X-Requested-With: JavaScript`.
                    .javaScriptAutoRedirect(false)
                    .scopes(Optional
                            .ofNullable(oidc.getScopes())
                            .map(scopes -> scopes.split("\\s+"))
                            .map(Arrays::asList)
                            .orElseGet(() -> List.of("openid", "email", "profile", "groups")))
                    .pkceRequired(true) // make configurable (?)
                    .stateSecret(null) // configurable (?)
                .end()
                .token()
                    .refreshExpired(true)
                    .issuer(oidc.getIssuer())
                .end()
                .tokenStateManager()
                    .strategy(Strategy.ID_REFRESH_TOKENS)
                .end();

        trustStores.configureTruststoreFile(oidc, null, builder);
        oidcConfig = builder.build();
    }

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext routingContext,
            OidcRequestContext<OidcTenantConfig> requestContext) {
        return Uni.createFrom().item(oidcConfig);
    }

}
