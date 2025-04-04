package com.github.streamshub.console.api.support;

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
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.github.streamshub.console.config.Authenticated;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.PrometheusConfig;
import com.github.streamshub.console.config.SchemaRegistryConfig;
import com.github.streamshub.console.config.Trustable;
import com.github.streamshub.console.config.security.OidcConfig;

import io.quarkus.oidc.common.runtime.OidcCommonConfig;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;

@ApplicationScoped
public class TrustStoreSupport {

    private static final Logger LOGGER = Logger.getLogger(TrustStoreSupport.class);
    private static final String TRUST_PREFIX_METRICS = "metrics-source:";
    private static final String TRUST_PREFIX_SCHEMA_REGISTRY = "schema-registry:";
    private static final String TRUST_PREFIX_OIDC_PROVIDER = "oidc-provider:";

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @Inject
    Vertx vertx;

    /*
     * public for testing only
     */
    public static String trustConfigName(Trustable trustable, Trustable context) {
        Objects.requireNonNull(trustable);

        if (context != null) {
            return trustConfigName(context, null) + ':' + trustable.getName();
        } else if (trustable instanceof PrometheusConfig) {
            return TRUST_PREFIX_METRICS + trustable.getName();
        } else if (trustable instanceof SchemaRegistryConfig) {
            return TRUST_PREFIX_SCHEMA_REGISTRY + trustable.getName();
        } else if (trustable instanceof OidcConfig) {
            return TRUST_PREFIX_OIDC_PROVIDER + trustable.getName();
        }

        throw new IllegalArgumentException("Unknown trustable: " + trustable);
    }

    public Optional<TlsConfiguration> getTlsConfiguration(Trustable trustable, Trustable context) {
        return tlsRegistry.get(trustConfigName(trustable, context));
    }

    /**
     * Extract trust store configuration and register with the TLS registry.
     */
    public ConsoleConfig registerTrustStores(ConsoleConfig config) {
        registerTrustStores(config.getMetricsSources(), null);
        registerTrustStores(config.getSchemaRegistries(), null);

        var oidcConfig = config.getSecurity().getOidc();
        if (oidcConfig != null) {
            registerTrustStores(List.of(oidcConfig), null);
        }

        return config;
    }

    private void registerTrustStores(List<? extends Trustable> trustables, Trustable context) {
        for (var source : trustables) {
            var trustStore = source.getTrustStore();

            if (trustStore != null) {
                String name = trustConfigName(source, context);
                var tlsConfig = new TrustedTlsConfiguration(name, vertx, trustStore);
                this.tlsRegistry.register(name, tlsConfig);
            }

            if (source instanceof Authenticated auth
                    && auth.getAuthentication() instanceof Trustable trustableAuth) {
                registerTrustStores(List.of(trustableAuth), context);
            }
        }
    }

    public void configureTruststoreFile(Trustable trustable, Trustable context, OidcCommonConfig.Tls tls) {
        final String bucketName = trustConfigName(trustable, context);

        getTlsConfiguration(trustable, context).map(TlsConfiguration::getTrustStore).ifPresentOrElse(
            trustStore -> configureTruststore(bucketName, trustStore, tls),
            () -> LOGGER.infof("No truststore found for bucket '" + bucketName + "'")
        );
    }

    /**
     * The OIDC subsystem takes the path to a truststore, so we need to write the
     * one from the TLS registry to a working file to provide to OIDC. This should
     * no longer be necessary in the next Quarkus LTS where OIDC is aware of the TLS
     * registry.
     */
    private void configureTruststore(String bucketName, KeyStore truststore, OidcCommonConfig.Tls tls) {
        Path truststorePath;
        File truststoreFile;

        try {
            truststorePath = Files.createTempFile(
                    bucketName.replaceAll("[^a-zA-Z0-9]", "_"),
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
        tls.trustStoreProvider = Optional.empty();
        tls.setTrustStoreFile(truststorePath);
        tls.setTrustStorePassword(secret);
    }
}
