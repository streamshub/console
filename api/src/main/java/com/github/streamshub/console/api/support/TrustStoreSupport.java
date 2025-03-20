package com.github.streamshub.console.api.support;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.Trustable;

import io.quarkus.tls.TlsConfigurationRegistry;
import io.vertx.core.Vertx;

@ApplicationScoped
public class TrustStoreSupport {

    public static final String TRUST_PREFIX_METRICS = "metrics-source:";
    public static final String TRUST_PREFIX_SCHEMA_REGISTRY = "schema-registry:";
    public static final String TRUST_PREFIX_OIDC_PROVIDER = "oidc-provider:";

    @Inject
    TlsConfigurationRegistry tlsRegistry;

    @Inject
    Vertx vertx;

    /**
     * Extract trust store configuration and register with the TLS registry.
     */
    public ConsoleConfig registerTrustStores(ConsoleConfig config) {
        registerTrustStores(TRUST_PREFIX_METRICS, config.getMetricsSources());
        registerTrustStores(TRUST_PREFIX_SCHEMA_REGISTRY, config.getSchemaRegistries());

        var oidcConfig = config.getSecurity().getOidc();
        if (oidcConfig != null) {
            registerTrustStores(TRUST_PREFIX_OIDC_PROVIDER, List.of(oidcConfig));
        }

        return config;
    }

    private void registerTrustStores(String prefix, List<? extends Trustable> trustables) {
        for (var source : trustables) {
            var trustStore = source.getTrustStore();
            if (trustStore != null) {
                String name = prefix + source.getName();
                var tlsConfig = new TrustedTlsConfiguration(name, vertx, trustStore);
                this.tlsRegistry.register(name, tlsConfig);
            }
        }
    }
}
