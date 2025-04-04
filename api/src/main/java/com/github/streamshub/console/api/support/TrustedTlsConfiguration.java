package com.github.streamshub.console.api.support;

import java.nio.file.Path;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
import io.quarkus.tls.runtime.config.KeyStoreConfig;
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
class TrustedTlsConfiguration extends BaseTlsConfiguration {

    private final Vertx vertx;
    private final TrustOptions trustOptions;
    private final KeyStore trustStore;

    public TrustedTlsConfiguration(String name, Vertx vertx, TrustStoreConfig config) {
        this.vertx = vertx;
        var trustStoreResult = load(name, vertx, config);
        Objects.requireNonNull(trustStoreResult, "trustStoreResult is unexpectedly null");
        trustOptions = trustStoreResult.options;
        trustStore = trustStoreResult.trustStore;
    }

    TrustStoreAndTrustOptions load(String name, Vertx vertx, TrustStoreConfig config) {
        return CertificateRecorder.getTrustStore(new BucketConfig(config), vertx, name);
    }

    @Override
    public TrustOptions getTrustStoreOptions() {
        return trustOptions;
    }

    @Override
    public KeyStore getTrustStore() {
        return trustStore;
    }

    @Override
    public synchronized SSLContext createSSLContext() throws Exception {
        TrustManagerFactory trustManagerFactory = trustOptions.getTrustManagerFactory(vertx);
        TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, new SecureRandom());
        return sslContext;
    }

    private static class BucketConfig implements io.quarkus.tls.runtime.config.TlsBucketConfig {
        private final Optional<io.quarkus.tls.runtime.config.TrustStoreConfig> tsConfig;

        BucketConfig(TrustStoreConfig config) {
            this.tsConfig = Optional.of(new CommonTrustStoreConfig(config));
        }

        @Override
        public Optional<KeyStoreConfig> keyStore() {
            return Optional.empty();
        }

        @Override
        public Optional<io.quarkus.tls.runtime.config.TrustStoreConfig> trustStore() {
            return tsConfig;
        }

        @Override
        public Optional<List<String>> cipherSuites() {
            return Optional.empty();
        }

        @Override
        public Set<String> protocols() {
            return Set.of("TLSv1.3", "TLSv1.2");
        }

        @Override
        public Duration handshakeTimeout() {
            return Duration.ofSeconds(10);
        }

        @Override
        public boolean alpn() {
            return true;
        }

        @Override
        public Optional<List<Path>> certificateRevocationList() {
            return Optional.empty();
        }

        @Override
        public boolean trustAll() {
            return false;
        }

        @Override
        public Optional<String> hostnameVerificationAlgorithm() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> reloadPeriod() {
            return Optional.empty();
        }
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

        @Override
        public CertificateExpiryPolicy certificateExpirationPolicy() {
            return CertificateExpiryPolicy.WARN;
        }

        private <C> Optional<C> optionalType(Type type, Class<C> target) {
            return this.type != type ? Optional.empty() : Optional.of(target.cast(this));
        }

        // Methods for PEM trust stores

        @Override
        public Optional<PemCertsConfig> pem() {
            return optionalType(Type.PEM, PemCertsConfig.class);
        }

        @Override
        public Optional<List<Path>> certs() {
            return Optional.of(List.of(Path.of(content.getValueFrom())));
        }

        // Methods for JKS and P12 trust stores

        @Override
        public Optional<JKSTrustStoreConfig> jks() {
            return optionalType(Type.JKS, JKSTrustStoreConfig.class);
        }

        @Override
        public Optional<P12TrustStoreConfig> p12() {
            return optionalType(Type.PKCS12, P12TrustStoreConfig.class);
        }

        @Override
        public Path path() {
            return Path.of(content.getValueFrom());
        }

        @Override
        public Optional<String> password() {
            return UncheckedIO.call(
                    () -> Value.getOptionalValue(config.getPassword()),
                    () -> "Exception reading TLS truststore password");
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
