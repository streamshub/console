package com.github.eyefloaters.console.api;

import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Default;
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
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule;
import org.apache.kafka.common.security.scram.ScramLoginModule;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.service.KafkaClusterService;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthentication;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationCustom;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationOAuth;
import io.strimzi.api.kafka.model.listener.KafkaListenerAuthenticationScramSha512;
import io.strimzi.api.kafka.model.listener.arraylistener.GenericKafkaListener;
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

    private static final String BASIC = "Basic ";
    private static final String BEARER = "Bearer ";
    private static final String OAUTHBEARER = OAuthBearerLoginModule.OAUTHBEARER_MECHANISM;
    private static final String SASL_OAUTH_CONFIG_TEMPLATE = OAuthBearerLoginModule.class.getName()
            + " required"
            + " oauth.access.token=\"%s\" ;";
    private static final String SASL_SCRAM_CONFIG_TEMPLATE = ScramLoginModule.class.getName()
            + " required"
            + " username=\"%s\""
            + " password=\"%s\" ;";
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
    @Named("userK8sClient")
    public Supplier<KubernetesClient> kubernetesClientSupplier(@Default KubernetesClient commonClient) {
        String[] userPass = getBasicAuthentication()
                .orElseThrow(() -> new NotAuthorizedException("%s realm=\"%s\""
                        .formatted(BASIC.trim(), "kubernetes")));

        Config userConfig = new ConfigBuilder(commonClient.getConfiguration())
                .withOauthToken(null)
                .withUsername(userPass[0])
                .withPassword(userPass[1])
                .build();

        KubernetesClient userClient = new KubernetesClientBuilder()
                .withConfig(userConfig)
                .build();

        return () -> userClient;
    }

    @Produces
    @RequestScoped
    public Supplier<Admin> adminClientSupplier(Function<Map<String, Object>, Admin> adminBuilder, Supplier<Kafka> cluster) {
        Map<String, Object> config = buildConfiguration(cluster.get());

        config.put(AdminClientConfig.METADATA_MAX_AGE_CONFIG, "30000");
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "10000");

        if (log.isTraceEnabled()) {
            String msg = config.entrySet()
                .stream()
                .map(entry -> "\t%s = %s".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n", "AdminClient configuration:\n", ""));
            log.trace(msg);
        }

        Admin client = adminBuilder.apply(config);
        return () -> client;
    }

    public void adminClientDisposer(@Disposes Supplier<Admin> client) {
        if (log.isTraceEnabled()) {
            String msg = client.get().metrics().entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(MetricName::name)))
                .map(entry -> "\t%-20s = %s".formatted(entry.getValue().metricValue(), entry.getKey()))
                .collect(Collectors.joining("\n", "AdminClient metrics:\n", ""));
            log.trace(msg);
        }

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
        config.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);

        @SuppressWarnings("resource") // No leak, it will be closed by the disposer
        Consumer<byte[], byte[]> consumer = new KafkaConsumer<>(config);
        return () -> consumer;
    }

    public void consumerDisposer(@Disposes Supplier<Consumer<byte[], byte[]>> consumer) {
        consumer.get().close();
    }

    @Produces
    @RequestScoped
    public Supplier<Producer<String, String>> producerSupplier(Supplier<Kafka> cluster) {
        Map<String, Object> config = buildConfiguration(cluster.get());

        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.ACKS_CONFIG, "all");
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        config.put(ProducerConfig.RETRIES_CONFIG, 0);

        @SuppressWarnings("resource") // No leak, it will be closed by the disposer
        Producer<String, String> producer = new KafkaProducer<>(config);
        return () -> producer;
    }

    public void producerDisposer(@Disposes Supplier<Producer<String, String>> producer) {
        producer.get().close();
    }

    Map<String, Object> buildConfiguration(Kafka cluster) {
        String listenerName = Optional.ofNullable(headers.getHeaderString("X-Console-Listener")).orElse("");
        var availableListeners = KafkaClusterService.consoleListeners(cluster);

        if (availableListeners.isEmpty()) {
            log.warnf(NO_COMPATIBLE_LISTENER, cluster.getStatus().getClusterId());
            throw noSuchKafka.get();
        }

        var selectedListeners = availableListeners
                .entrySet()
                .stream()
                .filter(listener -> listenerName.isEmpty() || listenerName.equals(listener.getKey().getName()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (selectedListeners.size() != 1) {
            List<String> challenges = availableListeners.keySet()
                .stream()
                .map(listener -> {
                    String scheme = getAuthenticationScheme(listener).trim();
                    return "%s realm=\"listener %s\"".formatted(scheme, listener.getName());
                })
                .toList();
            throw new NotAuthorizedException(challenges.get(0), challenges.subList(1, challenges.size()));
        }

        return selectedListeners.entrySet()
            .stream()
            .findFirst()
            .map(this::buildConfiguration)
            .orElseThrow(() -> {
                log.warnf(NO_COMPATIBLE_LISTENER, cluster.getStatus().getClusterId());
                return noSuchKafka.get();
            });
    }

    String getAuthenticationScheme(GenericKafkaListener listener) {
        var authentication = listener.getAuth();
        String authType = Optional.ofNullable(authentication.getType()).orElse("");

        switch (authType) {
            case KafkaListenerAuthenticationOAuth.TYPE_OAUTH:
                return BEARER;
            case KafkaListenerAuthenticationScramSha512.SCRAM_SHA_512:
                return BASIC;
            case KafkaListenerAuthenticationCustom.TYPE_CUSTOM:
                if (KafkaClusterService.mechanismEnabled(
                        (KafkaListenerAuthenticationCustom) authentication, OAUTHBEARER)) {
                    return BEARER;
                } else if (KafkaClusterService.mechanismEnabled(
                        (KafkaListenerAuthenticationCustom) authentication, "SCRAM-")) {
                    return BASIC;
                } else {
                    throw new IllegalStateException("Unsupported listener type - server error");
                }
            default:
                throw new IllegalStateException("Unsupported listener type - server error");
        }
    }

    Map<String, Object> buildConfiguration(Map.Entry<GenericKafkaListener, ListenerStatus> listener) {
        Map<String, Object> config = new HashMap<>();
        var authentication = listener.getKey().getAuth();
        String authType = Optional.ofNullable(authentication)
                .map(KafkaListenerAuthentication::getType)
                .orElse("");
        boolean saslEnabled;

        switch (authType) {
            case KafkaListenerAuthenticationOAuth.TYPE_OAUTH:
                saslEnabled = true;
                configureOAuthBearer(config);
                break;
            case KafkaListenerAuthenticationScramSha512.SCRAM_SHA_512:
                saslEnabled = true;
                configureScram(config);
                break;
            case KafkaListenerAuthenticationCustom.TYPE_CUSTOM:
                if (KafkaClusterService.mechanismEnabled(
                        (KafkaListenerAuthenticationCustom) authentication, OAUTHBEARER)) {
                    saslEnabled = true;
                    configureOAuthBearer(config);
                } else if (KafkaClusterService.mechanismEnabled(
                        (KafkaListenerAuthenticationCustom) authentication, "SCRAM-")) {
                    saslEnabled = true;
                    configureScram(config);
                } else {
                    saslEnabled = false;
                }

                break;
            default:
                log.trace("Broker authentication/SASL disabled");
                saslEnabled = false;
                break;
        }

        StringBuilder protocol = new StringBuilder();

        if (saslEnabled) {
            protocol.append("SASL_");
        }

        var listenerStatus = listener.getValue();
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
        log.trace("SASL/OAUTHBEARER enabled");
        config.put(SaslConfigs.SASL_MECHANISM, OAUTHBEARER);
        config.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, "io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler");
        // Do not attempt token refresh ahead of expiration (ExpiringCredentialRefreshingLogin)
        // May still cause warnings to be logged when token will expire in less than SASL_LOGIN_REFRESH_MIN_PERIOD_SECONDS.
        config.put(SaslConfigs.SASL_LOGIN_REFRESH_BUFFER_SECONDS, "0");

        String jaasConfig = getAuthorization(BEARER)
                .map(SASL_OAUTH_CONFIG_TEMPLATE::formatted)
                .orElseThrow(() -> new NotAuthorizedException(BEARER.trim()));

        config.put(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig);
    }

    void configureScram(Map<String, Object> config) {
        log.trace("SASL/SCRAM enabled");
        config.put(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512");

        String jassConfig = getBasicAuthentication()
                .map(SASL_SCRAM_CONFIG_TEMPLATE::formatted)
                .orElseThrow(() -> new NotAuthorizedException(BASIC.trim()));

        config.put(SaslConfigs.SASL_JAAS_CONFIG, jassConfig);
    }

    Optional<String[]> getBasicAuthentication() {
        return getAuthorization(BASIC)
            .map(Base64.getDecoder()::decode)
            .map(String::new)
            .filter(authn -> authn.indexOf(':') >= 0)
            .map(authn -> new String[] {
                authn.substring(0, authn.indexOf(':')),
                authn.substring(authn.indexOf(':') + 1)
            })
            .filter(userPass -> !userPass[0].isEmpty() && !userPass[1].isEmpty());
    }

    Optional<String> getAuthorization(String scheme) {
        return Optional.ofNullable(headers.getHeaderString(HttpHeaders.AUTHORIZATION))
                .filter(header -> header.regionMatches(true, 0, scheme, 0, scheme.length()))
                .map(header -> header.substring(scheme.length()));
    }
}
