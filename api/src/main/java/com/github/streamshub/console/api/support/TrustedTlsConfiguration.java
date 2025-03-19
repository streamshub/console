package com.github.streamshub.console.api.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.github.streamshub.console.config.TrustStoreConfig;
import com.github.streamshub.console.config.TrustStoreConfig.Type;
import com.github.streamshub.console.config.Value;

import io.quarkus.tls.BaseTlsConfiguration;
import io.quarkus.tls.runtime.CertificateRecorder;
import io.quarkus.tls.runtime.TrustStoreAndTrustOptions;
import io.quarkus.tls.runtime.config.JKSTrustStoreConfig;
import io.quarkus.tls.runtime.config.P12TrustStoreConfig;
import io.quarkus.tls.runtime.config.PemCertsConfig;
import io.quarkus.tls.runtime.config.TrustStoreCredentialProviderConfig;
import io.vertx.core.Vertx;
import io.vertx.core.net.TrustOptions;

/**
 * TLS configuration to be used by various clients in the console, e.g.
 * Prometheus and Apicurio registry.
 *
 * This class is based on the Quarkus
 * {@link io.quarkus.tls.runtime.VertxCertificateHolder VertxCertificateHolder},
 * but with support only for trust stores. Instances are created based on the
 * console configuration YAML and registered with the Quarkus
 * {@link io.quarkus.tls.TlsConfigurationRegistry TlsConfigurationRegistry} at
 * runtime.
 */
public class TrustedTlsConfiguration extends BaseTlsConfiguration {

    private final Vertx vertx;
    private TrustOptions trustOptions;
    private KeyStore trustStore;

    public TrustedTlsConfiguration(String name, Vertx vertx, TrustStoreConfig config) {
        this.vertx = vertx;
        var trustStoreResult = load(name, vertx, config);
        trustOptions = trustStoreResult != null ? trustStoreResult.options : null;
        trustStore = trustStoreResult != null ? trustStoreResult.trustStore : null;
    }

    TrustStoreAndTrustOptions load(String name, Vertx vertx, TrustStoreConfig config) {
        return CertificateRecorder.verifyTrustStore(new CommonTrustStoreConfig(config), vertx, name);
    }

    @Override
    public synchronized TrustOptions getTrustStoreOptions() {
        return trustOptions;
    }

    @Override
    public synchronized KeyStore getTrustStore() {
        return trustStore;
    }

    @Override
    public synchronized SSLContext createSSLContext() throws Exception {
        TrustManagerFactory trustManagerFactory;
        TrustManager[] trustManagers = null;

        if (trustOptions != null) {
            trustManagerFactory = trustOptions.getTrustManagerFactory(vertx);
            trustManagers = trustManagerFactory.getTrustManagers();
        }

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, new SecureRandom());

        return sslContext;
    }

    private static class CommonTrustStoreConfig implements io.quarkus.tls.runtime.config.TrustStoreConfig,
            JKSTrustStoreConfig, P12TrustStoreConfig, PemCertsConfig {
        private final TrustStoreConfig config;
        private final TrustStoreConfig.Type type;
        private final Value content;

        CommonTrustStoreConfig(TrustStoreConfig config) {
            this.config = config;
            this.type = config.getType();
            this.content = config.getContent();
        }

        @Override
        public TrustStoreCredentialProviderConfig credentialsProvider() {
            return null;
        }

        // Methods for PEM trust stores

        @Override
        public Optional<PemCertsConfig> pem() {
            return type == Type.PEM ? Optional.of(this) : Optional.empty();
        }

        @Override
        public Optional<List<Path>> certs() {
            return Optional.of(List.of(Path.of(content.getValueFrom())));
        }

        // Methods for JKS and P12 trust stores

        @Override
        public Optional<JKSTrustStoreConfig> jks() {
            return type == Type.JKS ? Optional.of(this) : Optional.empty();
        }

        @Override
        public Optional<P12TrustStoreConfig> p12() {
            return type == Type.PKCS12 ? Optional.of(this) : Optional.empty();
        }

        @Override
        public Path path() {
            return Path.of(content.getValueFrom());
        }

        @Override
        public Optional<String> password() {
            return Optional.ofNullable(config.getPassword())
                    .map(p -> Optional.ofNullable(p.getValue()).orElseGet(() -> {
                        try {
                            return Files.readString(Path.of(p.getValueFrom()));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }));
        }

        @Override
        public Optional<String> alias() {
            return Optional.ofNullable(config.getAlias());
        }

        @Override
        public Optional<String> provider() {
            return Optional.empty();
        }
    }
}
