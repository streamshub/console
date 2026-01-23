package com.github.streamshub.console.api.support;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaConnectConfig;
import com.github.streamshub.console.config.PrometheusConfig;
import com.github.streamshub.console.config.SchemaRegistryConfig;
import com.github.streamshub.console.config.Trustable;
import com.github.streamshub.console.config.authentication.Authenticated;
import com.github.streamshub.console.config.security.OidcConfig;

import io.quarkus.oidc.common.runtime.config.OidcCommonConfigBuilder;
import io.quarkus.tls.TlsConfiguration;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;

@ApplicationScoped
public class TrustStoreSupport {

    private static final Logger LOGGER = Logger.getLogger(TrustStoreSupport.class);
    private static final String TRUST_PREFIX_KAFKA_CONNECT = "kafka-connect:";
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
        } else if (trustable instanceof KafkaConnectConfig) {
            return TRUST_PREFIX_KAFKA_CONNECT + trustable.getName();
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
        registerTrustStores(config.getKafkaConnectClusters(), null);
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

            if (source instanceof Authenticated auth) {
                auth.getTrustableAuthentication()
                    .ifPresent(trustableAuth -> registerTrustStores(List.of(trustableAuth), source));
            }
        }
    }

    public void configureTruststoreFile(Trustable trustable, Trustable context, OidcCommonConfigBuilder<?> builder) {
        if (trustable.getTrustStore() != null) {
            final String bucketName = trustConfigName(trustable, context);

            tlsRegistry.get(bucketName).ifPresentOrElse(
                tlsBucket -> builder.tlsConfigurationName(bucketName),
                () -> LOGGER.warnf("No truststore found for bucket '" + bucketName + "'")
            );
        }
    }
}
