package com.github.eyefloaters.console.api;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.service.KafkaClusterService;

import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationCustom;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationOAuth;
import io.strimzi.api.kafka.model.status.ListenerStatus;

@ApplicationScoped
public class ClientFactory {

    private static final String BEARER = "Bearer ";
    private static final String SASL_OAUTH_CONFIG_TEMPLATE = OAuthBearerLoginModule.class.getName()
            + " required"
            + " oauth.access.token=\"%s\";";

    @Inject
    Logger log;

    @Inject
    SharedIndexInformer<Kafka> kafkaInformer;

    @Inject
    HttpHeaders headers;

    @Inject
    UriInfo requestUri;

    /**
     * An inject-able function to produce an Admin client for a given
     * configuration map. This is used in order to allow tests to provide
     * an overridden function to supply a mocked/spy Admin instance.
     */
    @Produces
    @ApplicationScoped
    @Named("kafkaAdminBuilder")
    Function<Map<String, Object>, Admin> kafkaAdminBuilder = Admin::create;

    @Produces
    @RequestScoped
    public Supplier<Admin> adminClientSupplier(Function<Map<String, Object>, Admin> adminBuilder) {
        Map<String, Object> config = buildConfiguration();

        config.put(AdminClientConfig.METADATA_MAX_AGE_CONFIG, "30000");
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "10000");

        if (log.isDebugEnabled()) {
            String msg = config.entrySet()
                .stream()
                .map(entry -> "\t%s = %s".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n", "AdminClient configuration:\n", ""));
            log.debug(msg);
        }

        Admin client = adminBuilder.apply(config);
        return () -> client;
    }

    public void adminClientDisposer(@Disposes Supplier<Admin> client) {
        client.get().close();
    }

    @Produces
    @RequestScoped
    public Supplier<Consumer<byte[], byte[]>> consumerSupplier() {
        Map<String, Object> config = buildConfiguration();

        config.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 50_000);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        @SuppressWarnings("resource") // No leak, it will be closed by the disposer
        Consumer<byte[], byte[]> consumer = new KafkaConsumer<>(config);
        return () -> consumer;
    }

    public void consumerDisposer(@Disposes Supplier<Consumer<byte[], byte[]>> consumer) {
        consumer.get().close();
    }

    Map<String, Object> buildConfiguration() {
        String clusterId = requestUri.getPathParameters().getFirst("clusterId");

        if (clusterId == null) {
            throw new IllegalStateException("Admin client was accessed, "
                    + "but the requested operation does not provide a Kafka cluster ID");
        }

        Supplier<NotFoundException> noSuchKafka =
            () -> new NotFoundException("No such Kafka cluster: " + clusterId);

        Kafka cluster = KafkaClusterService.findCluster(kafkaInformer, clusterId)
            .orElseThrow(noSuchKafka);

        return KafkaClusterService.externalListeners(cluster)
            .findFirst()
            .map(l -> buildConfiguration(cluster, l))
            .orElseThrow(noSuchKafka);
    }

    Map<String, Object> buildConfiguration(Kafka cluster, ListenerStatus listenerStatus) {
        Map<String, Object> config = new HashMap<>();
        var listenerAuthentication = KafkaClusterService.getAuthentication(cluster, listenerStatus);
        String authType = listenerAuthentication.map(auth -> auth.getType()).orElse("");
        boolean saslEnabled;

        if (authType.isBlank()) {
            log.debug("Broker authentication/SASL disabled");
            saslEnabled = false;
        } else if (KafkaListenerAuthenticationOAuth.TYPE_OAUTH.equals(authType)) {
            saslEnabled = true;
            configureOAuthBearer(config);
        } else if (KafkaListenerAuthenticationCustom.TYPE_CUSTOM.equals(authType)) {
            saslEnabled = listenerAuthentication
                .map(KafkaListenerAuthenticationCustom.class::cast)
                .filter(KafkaListenerAuthenticationCustom::isSasl)
                .map(KafkaListenerAuthenticationCustom::getListenerConfig)
                .map(listenerConfig -> listenerConfig.get("sasl.enabled.mechanisms"))
                .filter(mechanisms -> mechanisms.toString().contains("oauthbearer"))
                .map(unused -> {
                    configureOAuthBearer(config);
                    return true;
                })
                .orElseThrow(() ->
                    new IllegalStateException("Kafka cluster with 'custom' authentication without SASL/OAUTHBEARER is not supported"));
        } else {
            throw new IllegalStateException("Kafka cluster with '%s' authentication is not supported".formatted(authType));
        }

        StringBuilder protocol = new StringBuilder();

        if (saslEnabled) {
            protocol.append("SASL_");
        }

        List<String> certificates = Optional.ofNullable(listenerStatus.getCertificates()).orElseGet(Collections::emptyList);

        if (!certificates.isEmpty()) {
            protocol.append(SecurityProtocol.SSL.name);
            config.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
            config.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, String.join("\n", certificates).trim());
        } else {
            protocol.append(SecurityProtocol.PLAINTEXT.name);
        }

        config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, listenerStatus.getBootstrapServers());
        config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, protocol.toString());

        return config;
    }

    void configureOAuthBearer(Map<String, Object> config) {
        log.debug("SASL/OAUTHBEARER enabled");
        config.put(SaslConfigs.SASL_MECHANISM, "OAUTHBEARER");
        config.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, "io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler");
        // Do not attempt token refresh ahead of expiration (ExpiringCredentialRefreshingLogin)
        // May still cause warnings to be logged when token will expired in less than SASL_LOGIN_REFRESH_MIN_PERIOD_SECONDS.
        config.put(SaslConfigs.SASL_LOGIN_REFRESH_BUFFER_SECONDS, "0");

        final String accessToken = Optional.ofNullable(headers.getHeaderString(HttpHeaders.AUTHORIZATION))
                .filter(header -> header.regionMatches(true, 0, BEARER, 0, BEARER.length()))
                .map(header -> header.substring(BEARER.length()))
                .orElseThrow(() -> new NotAuthorizedException(BEARER.trim()));

        config.put(SaslConfigs.SASL_JAAS_CONFIG, String.format(SASL_OAUTH_CONFIG_TEMPLATE, accessToken));
    }
}
