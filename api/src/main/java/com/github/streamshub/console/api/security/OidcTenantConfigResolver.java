package com.github.streamshub.console.api.security;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.github.streamshub.console.config.ConsoleConfig;

import io.quarkus.oidc.OidcRequestContext;
import io.quarkus.oidc.OidcTenantConfig;
import io.quarkus.oidc.TenantConfigResolver;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
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
    @ConfigProperty(name = "console.work-path", defaultValue = "${java.io.tmpdir}")
    String workPath;

    @Inject
    TlsConfigurationRegistry tlsRegistry;

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

        getTlsConfiguration().map(TlsConfiguration::getTrustStore).ifPresent(truststore -> {
            File file = new File(workPath + "/" + UUID.randomUUID().toString() + "-truststore." + truststore.getType());
            String secret = UUID.randomUUID().toString();

            try (OutputStream out = new FileOutputStream(file)) {
                truststore.store(out, secret.toCharArray());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            oidcConfig.tls.setTrustStoreFile(file.toPath());
            oidcConfig.tls.setTrustStorePassword(secret);
        });
    }

    Optional<TlsConfiguration> getTlsConfiguration() {
        String dotSeparatedSource = "oidc.provider.trust";
        String dashSeparatedSource = "oidc-provider-trust";
        return tlsRegistry.get(dotSeparatedSource).or(() -> tlsRegistry.get(dashSeparatedSource));
    }

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext routingContext,
            OidcRequestContext<OidcTenantConfig> requestContext) {
        return Uni.createFrom().item(oidcConfig);
    }

}
