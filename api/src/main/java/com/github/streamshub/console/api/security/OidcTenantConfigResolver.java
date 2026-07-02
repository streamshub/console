package com.github.streamshub.console.api.security;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.logging.Logger;

import com.github.streamshub.console.api.support.Holder;
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

    @Inject
    @Named(OidcDiscoveryResponseFilter.OIDC_END_SESSION_ENDPOINT)
    Supplier<Holder<String>> endSessionEndpoint;

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
                    .forceRedirectHttpsScheme(Boolean.TRUE.equals(oidc.isForceRedirectHttpsScheme()))
                    .restorePathAfterRedirect(true)
                    // Do not redirect to the IdP for JavaScript requests. They are identified by
                    // requests from the React UI with header `X-Requested-With: JavaScript`.
                    .javaScriptAutoRedirect(false)
                    .addOpenidScope(false)
                    .scopes(Optional
                            .ofNullable(oidc.getScopes())
                            .map(scopes -> scopes.split("\\s+"))
                            .map(Arrays::asList)
                            .orElseGet(() -> List.of("openid", "email", "profile", "groups")))
                    .pkceRequired(!Boolean.FALSE.equals(oidc.isPkceRequired())) // true by default
                    .stateSecret(oidc.getStateSecret())
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

        OidcTenantConfig config;

        if (endSessionEndpoint.get().isPresent()) {
            /*
             * When the provider supports RP-initiated logout, configure
             * Quarkus OIDC to use it. Note, in this case our SessionResource logout
             * endpoint is NOT used, although the URL path is the same. This makes
             * the back-end logout functionality opaque to the front-end.
             */
            config = OidcTenantConfig.builder(oidcConfig)
                .logout()
                    .path("/api/session/logout")
                    .postLogoutPath("/")
                .end()
                .build();
        } else {
            config = oidcConfig;
        }

        return Uni.createFrom().item(config);
    }

}
