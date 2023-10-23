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
import io.strimzi.api.kafka.model.status.ListenerStatus;

/**
 * The ClientFactory is responsible for managing the life-cycles of Kafka clients
 * - the {@linkplain Admin} client and the {@linkplain Consumer}. The factory
 * will lazily create a per-request client when accessed by
 * {@linkplain com.github.eyefloaters.console.api.service service code} which
 * will be usable for the duration of the request and closed by the disposer
 * methods in this class upon completion of the request.
 *
 * <p>Construction of a client is dependent on the presence of a {@code clusterId}
 * path parameter being present in the request URL as well as the existence of a
 * matching Strimzi {@linkplain Kafka} CR in the watch cache available to the
 * application's service account.
 */
@ApplicationScoped
public class ClientFactory {

    private static final String BEARER = "Bearer ";
    private static final String OAUTHBEARER = "OAUTHBEARER";
    private static final String SASL_OAUTH_CONFIG_TEMPLATE = OAuthBearerLoginModule.class.getName()
            + " required"
            + " oauth.access.token=\"%s\";";
    private static final String NO_COMPATIBLE_LISTENER = """
            Request to access Kafka cluster %s could not be fulfilled because no listeners were found with:
            \t(1) authentication disabled
            \t(2) authentication type `oauth`, or
            \t(3) authentication type `custom` and SASL mechanism OAUTHBEARER supported.
            """;

    private final Supplier<NotFoundException> noSuchKafka =
            () -> new NotFoundException("Requested Kafka cluster does not exist or is not configured with a compatible listener");

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

    /**
     * Provides the Strimzi Kafka custom resource addressed by the current request
     * URL as an injectable bean. This allows for the Kafka to be obtained by
     * application logic without an additional lookup.
     *
     * @return a supplier that gives the Strimzi Kafka CR specific to the current
     *         request
     * @throws IllegalStateException when an attempt is made to access an injected
     *                               Kafka Supplier but the current request does not
     *                               include the Kafka clusterId path parameter.
     * @throws NotFoundException     when the provided Kafka clusterId does not
     *                               match any known Kafka cluster.
     */
    @Produces
    @RequestScoped
    public Supplier<Kafka> kafkaResourceSupplier() {
        String clusterId = requestUri.getPathParameters().getFirst("clusterId");

        if (clusterId == null) {
            throw new IllegalStateException("Admin client was accessed, "
                    + "but the requested operation does not provide a Kafka cluster ID");
        }

        Kafka cluster = KafkaClusterService.findCluster(kafkaInformer, clusterId)
                .orElseThrow(noSuchKafka);

        return () -> cluster;
    }

    @Produces
    @RequestScoped
    public Supplier<Admin> adminClientSupplier(Function<Map<String, Object>, Admin> adminBuilder, Supplier<Kafka> cluster) {
        Map<String, Object> config = buildConfiguration(cluster.get());

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
    public Supplier<Consumer<byte[], byte[]>> consumerSupplier(Supplier<Kafka> cluster) {
        Map<String, Object> config = buildConfiguration(cluster.get());

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

    Map<String, Object> buildConfiguration(Kafka cluster) {
        return KafkaClusterService.consoleListener(cluster)
            .map(l -> buildConfiguration(cluster, l))
            .orElseThrow(() -> {
                log.warnf(NO_COMPATIBLE_LISTENER, cluster.getStatus().getClusterId());
                return noSuchKafka.get();
            });
    }

    Map<String, Object> buildConfiguration(Kafka cluster, ListenerStatus listenerStatus) {
        Map<String, Object> config = new HashMap<>();
        String authType = KafkaClusterService.getAuthType(cluster, listenerStatus).orElse("");
        boolean saslEnabled;

        if (authType.isBlank()) {
            log.debug("Broker authentication/SASL disabled");
            saslEnabled = false;
        } else {
            saslEnabled = true;
            configureOAuthBearer(config);
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
        config.put(SaslConfigs.SASL_MECHANISM, OAUTHBEARER);
        config.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, "io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler");
        // Do not attempt token refresh ahead of expiration (ExpiringCredentialRefreshingLogin)
        // May still cause warnings to be logged when token will expire in less than SASL_LOGIN_REFRESH_MIN_PERIOD_SECONDS.
        config.put(SaslConfigs.SASL_LOGIN_REFRESH_BUFFER_SECONDS, "0");

        final String accessToken = Optional.ofNullable(headers.getHeaderString(HttpHeaders.AUTHORIZATION))
                .filter(header -> header.regionMatches(true, 0, BEARER, 0, BEARER.length()))
                .map(header -> header.substring(BEARER.length()))
                .orElseThrow(() -> new NotAuthorizedException(BEARER.trim()));

        config.put(SaslConfigs.SASL_JAAS_CONFIG, String.format(SASL_OAUTH_CONFIG_TEMPLATE, accessToken));
    }
}
