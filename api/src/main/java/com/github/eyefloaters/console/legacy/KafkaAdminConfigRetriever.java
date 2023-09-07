package com.github.eyefloaters.console.legacy;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * KafkaAdminConfigRetriever class gets configuration from envvars
 */
@Singleton
public class KafkaAdminConfigRetriever {

    protected final Logger log = Logger.getLogger(KafkaAdminConfigRetriever.class);

    private static final String PREFIX = "kafka.admin.";
    private static final String OAUTHBEARER = "OAUTHBEARER";

    public static final String BOOTSTRAP_SERVERS = PREFIX + "bootstrap.servers";
    public static final String API_TIMEOUT_MS_CONFIG = PREFIX + "api.timeout.ms.config";
    public static final String REQUEST_TIMEOUT_MS_CONFIG = PREFIX + "request.timeout.ms.config";
    public static final String BASIC_ENABLED = PREFIX + "basic.enabled";
    public static final String OAUTH_ENABLED = PREFIX + "oauth.enabled";
    public static final String OAUTH_TRUSTED_CERT = PREFIX + "oauth.trusted.cert";
    public static final String OAUTH_JWKS_ENDPOINT_URI = PREFIX + "oauth.jwks.endpoint.uri";
    public static final String OAUTH_TOKEN_ENDPOINT_URI = PREFIX + "oauth.token.endpoint.uri";

    public static final String BROKER_TLS_ENABLED = PREFIX + "broker.tls.enabled";
    public static final String BROKER_TRUSTED_CERT = PREFIX + "broker.trusted.cert";

    public static final String ACL_RESOURCE_OPERATIONS = PREFIX + "acl.resource.operations";

    @Inject
    @ConfigProperty(name = BOOTSTRAP_SERVERS)
    String bootstrapServers;

    @Inject
    @ConfigProperty(name = API_TIMEOUT_MS_CONFIG, defaultValue = "30000")
    String apiTimeoutMsConfig;

    @Inject
    @ConfigProperty(name = REQUEST_TIMEOUT_MS_CONFIG, defaultValue = "10000")
    String requestTimeoutMsConfig;

    @Inject
    @ConfigProperty(name = BASIC_ENABLED, defaultValue = "false")
    boolean basicEnabled;

    @Inject
    @ConfigProperty(name = OAUTH_ENABLED, defaultValue = "false")
    boolean oauthEnabled;

    @Inject
    @ConfigProperty(name = BROKER_TLS_ENABLED, defaultValue = "false")
    boolean brokerTlsEnabled;

    @Inject
    @ConfigProperty(name = BROKER_TRUSTED_CERT)
    Optional<String> brokerTrustedCert;

    @Inject
    @ConfigProperty(name = ACL_RESOURCE_OPERATIONS, defaultValue = "{}")
    String aclResourceOperations;

    Map<String, Object> acConfig;

    @PostConstruct
    public void initialize() {
        acConfig = envVarsToAdminClientConfig();
        logConfiguration();
    }

    private Map<String, Object> envVarsToAdminClientConfig() {
        Map<String, Object> adminClientConfig = new HashMap<>();

        adminClientConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        boolean saslEnabled;

        // oAuth
        if (oauthEnabled) {
            log.info("OAuth enabled");
            saslEnabled = true;
            adminClientConfig.put(SaslConfigs.SASL_MECHANISM, OAUTHBEARER);
            adminClientConfig.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, "io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler");
            // Do not attempt token refresh ahead of expiration (ExpiringCredentialRefreshingLogin)
            // May still cause warnings to be logged when token will expired in less than SASL_LOGIN_REFRESH_MIN_PERIOD_SECONDS.
            adminClientConfig.put(SaslConfigs.SASL_LOGIN_REFRESH_BUFFER_SECONDS, "0");
        } else if (basicEnabled) {
            log.info("SASL/PLAIN from HTTP Basic authentication enabled");
            saslEnabled = true;
            adminClientConfig.put(SaslConfigs.SASL_MECHANISM, "PLAIN");
        } else {
            log.info("Broker authentication/SASL disabled");
            saslEnabled = false;
        }

        StringBuilder protocol = new StringBuilder();

        if (saslEnabled) {
            protocol.append("SASL_");
        }

        if (brokerTlsEnabled) {
            protocol.append(SecurityProtocol.SSL.name);

            brokerTrustedCert.ifPresent(certConfig -> {
                String certContent = getBrokerTrustedCertificate(certConfig);

                if (certContent != null && !certContent.isBlank()) {
                    adminClientConfig.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, certContent);
                    adminClientConfig.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
                }
            });
        } else {
            protocol.append(SecurityProtocol.PLAINTEXT.name);
        }

        adminClientConfig.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, protocol.toString());

        return adminClientConfig;
    }

    private void logConfiguration() {
        log.info("AdminClient configuration:");
        acConfig.entrySet().forEach(entry -> {
            log.infof("\t%s = %s", entry.getKey(), entry.getValue());
        });
    }

    public boolean isBasicEnabled() {
        return basicEnabled;
    }

    public boolean isOauthEnabled() {
        return oauthEnabled;
    }

    public Map<String, Object> getAcConfig() {
        Map<String, Object> config = new HashMap<>(acConfig);
        config.put(AdminClientConfig.METADATA_MAX_AGE_CONFIG, "30000");
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeoutMsConfig);
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, apiTimeoutMsConfig);

        return config;
    }

    public Map<String, Object> getConsumerConfig() {
        return new HashMap<>(acConfig);
    }

    public Map<String, Object> getProducerConfig() {
        return new HashMap<>(acConfig);
    }

    public String getBrokerTrustedCertificate(final String certConfig) {
        try {
            final Path certPath = Path.of(certConfig);

            if (Files.isReadable(certPath)) {
                return Files.readString(certPath);
            }
        } catch (InvalidPathException e) {
            log.debugf("Value of %s was not a valid Path: %s", BROKER_TRUSTED_CERT, e.getMessage());
        } catch (Exception e) {
            log.warnf("Exception loading value of %s as a file: %s", BROKER_TRUSTED_CERT, e.getMessage());
        }

        String value = certConfig;

        try {
            value = new String(Base64.getDecoder().decode(certConfig), StandardCharsets.UTF_8);
            log.debug("Successfully decoded base-64 cert config value");
        } catch (IllegalArgumentException e) {
            log.debugf("Cert config value was not base-64 encoded: %s", e.getMessage());
        }

        return value;
    }

    public String getAclResourceOperations() {
        return aclResourceOperations;
    }
}

