package com.github.streamshub.console.api.security;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.KeyStore;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

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
    Logger logger;

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

        getTlsConfiguration().map(TlsConfiguration::getTrustStore).ifPresentOrElse(
            this::configureTruststore,
            () -> logger.infof("No truststore configured for OIDC provider")
        );
    }

    Optional<TlsConfiguration> getTlsConfiguration() {
        String dotSeparatedSource = "oidc.provider.trust";
        String dashSeparatedSource = "oidc-provider-trust";
        return tlsRegistry.get(dotSeparatedSource).or(() -> tlsRegistry.get(dashSeparatedSource));
    }

    /**
     * The OIDC subsystem takes the path to a truststore, so we need to write the
     * one from the TLS registry to a working file to provide to OIDC. This should
     * no longer be necessary in the next Quarkus LTS where OIDC is aware of the TLS
     * registry.
     */
    void configureTruststore(KeyStore truststore) {
        Path truststorePath;
        File truststoreFile;

        try {
            truststorePath = Files.createTempFile(
                    "oidc-provider-trust",
                    "." + truststore.getType(),
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            truststoreFile = truststorePath.toFile();
            truststoreFile.deleteOnExit();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String secret = UUID.randomUUID().toString();

        try (OutputStream out = new FileOutputStream(truststoreFile)) {
            truststore.store(out, secret.toCharArray());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // No default provided, set to empty to avoid NPE
        oidcConfig.tls.trustStoreProvider = Optional.empty();
        oidcConfig.tls.setTrustStoreFile(truststorePath);
        oidcConfig.tls.setTrustStorePassword(secret);
        // Future: map the certificate alias if provided
        // oidcConfig.tls.setTrustStoreCertAlias(null);
    }

    @Override
    public Uni<OidcTenantConfig> resolve(RoutingContext routingContext,
            OidcRequestContext<OidcTenantConfig> requestContext) {
        return Uni.createFrom().item(oidcConfig);
    }

}
