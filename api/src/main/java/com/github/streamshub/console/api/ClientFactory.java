package com.github.streamshub.console.api;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Instance;
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
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.console.api.security.SaslJaasConfigCredential;
import com.github.streamshub.console.api.service.MetricsService;
import com.github.streamshub.console.api.support.Holder;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.TrustAllCertificateManager;
import com.github.streamshub.console.api.support.serdes.RecordData;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.config.SchemaRegistryConfig;

import io.apicurio.registry.resolver.client.RegistryClientFacade;
import io.apicurio.registry.serde.config.KafkaSerdeConfig;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.quarkus.security.identity.SecurityIdentity;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaClusterSpec;
import io.strimzi.api.kafka.model.kafka.KafkaSpec;
import io.strimzi.api.kafka.model.kafka.KafkaStatus;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListener;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthentication;
import io.strimzi.api.kafka.model.kafka.listener.ListenerStatus;

/**
 * The ClientFactory is responsible for managing the life-cycles of Kafka clients
 * - the {@linkplain Admin} client and the {@linkplain Consumer}. The factory
 * will lazily create a per-request client when accessed by
 * {@linkplain com.github.streamshub.console.api.service service code} which
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

    public static final String OAUTHBEARER = OAuthBearerLoginModule.OAUTHBEARER_MECHANISM;
    public static final String PLAIN = "PLAIN";
    public static final String SCRAM_SHA256 = "SCRAM-SHA-256";
    public static final String SCRAM_SHA512 = "SCRAM-SHA-512";

    private static final String STRIMZI_OAUTH_CALLBACK = "io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler";

    public static final String NO_SUCH_KAFKA_MESSAGE = "Requested Kafka cluster %s does not exist or is not configured";
    public static final Function<String, NotFoundException> NO_SUCH_KAFKA =
            clusterId -> new NotFoundException(NO_SUCH_KAFKA_MESSAGE.formatted(clusterId));

    @Inject
    Logger log;

    @Inject
    Config config;

    @Inject
    ObjectMapper mapper;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    Holder<SharedIndexInformer<Kafka>> kafkaInformer;

    @Inject
    Instance<TrustAllCertificateManager> trustManager;

    @Inject
    HttpHeaders headers;

    @Inject
    UriInfo requestUri;

    /**
     * An inject-able function to produce an Admin client for a given configuration
     * map. This is used in order to allow tests to provide an overridden function
     * to supply a mocked/spy Admin instance.
     */
    @Produces
    @ApplicationScoped
    @Named("kafkaAdminBuilder")
    Function<Map<String, Object>, Admin> kafkaAdminBuilder = Admin::create;

    /**
     * An inject-able operator to filter an Admin client. This is used in order to
     * allow tests to provide an overridden function to supply a mocked/spy Admin
     * instance.
     */
    @Produces
    @ApplicationScoped
    @Named("kafkaAdminFilter")
    UnaryOperator<Admin> kafkaAdminFilter = UnaryOperator.identity();

    @Inject
    MetricsService metricsService;

    @Inject
    Map<String, RegistryClientFacade> registryClients;

    @Produces
    @ApplicationScoped
    Map<String, KafkaContext> produceKafkaContexts(Function<Map<String, Object>, Admin> adminBuilder) {

        final Map<String, KafkaContext> contexts = new ConcurrentHashMap<>();

        if (kafkaInformer.isPresent()) {
            addKafkaEventHandler(contexts, adminBuilder);
        }

        // Configure clusters that will not be configured by events
        consoleConfig.getKafka().getClusters()
            .stream()
            .filter(c -> cachedKafkaResource(c).isEmpty())
            .forEach(clusterConfig -> putKafkaContext(contexts,
                        clusterConfig,
                        Optional.empty(),
                        adminBuilder));

        return Collections.unmodifiableMap(contexts);
    }

    void addKafkaEventHandler(Map<String, KafkaContext> contexts,
            Function<Map<String, Object>, Admin> adminBuilder) {

        kafkaInformer.get().addEventHandlerWithResyncPeriod(new ResourceEventHandler<Kafka>() {
            public void onAdd(Kafka kafka) {
                if (log.isDebugEnabled()) {
                    log.debugf("Kafka resource %s added", Cache.metaNamespaceKeyFunc(kafka));
                }
                findConfig(kafka).ifPresentOrElse(
                        clusterConfig -> {
                            if (defaultedClusterId(clusterConfig, Optional.of(kafka))) {
                                log.debugf("Ignoring added Kafka resource %s, cluster ID not yet available and not provided via configuration",
                                        Cache.metaNamespaceKeyFunc(kafka));
                            } else {
                                putKafkaContext(contexts,
                                        clusterConfig,
                                        Optional.of(kafka),
                                        adminBuilder);
                            }
                        },
                        () -> log.debugf("Ignoring added Kafka resource %s, not found in configuration", Cache.metaNamespaceKeyFunc(kafka)));
            }

            public void onUpdate(Kafka oldKafka, Kafka newKafka) {
                if (log.isDebugEnabled()) {
                    log.debugf("Kafka resource %s updated", Cache.metaNamespaceKeyFunc(oldKafka));
                }
                findConfig(newKafka).ifPresentOrElse(
                        clusterConfig -> putKafkaContext(contexts,
                            clusterConfig,
                            Optional.of(newKafka),
                            adminBuilder),
                        () -> log.debugf("Ignoring updated Kafka resource %s, not found in configuration", Cache.metaNamespaceKeyFunc(newKafka)));
            }

            public void onDelete(Kafka kafka, boolean deletedFinalStateUnknown) {
                if (log.isDebugEnabled()) {
                    log.debugf("Kafka resource %s deleted", Cache.metaNamespaceKeyFunc(kafka));
                }
                findConfig(kafka).ifPresentOrElse(
                        clusterConfig -> {
                            String clusterKey = clusterConfig.clusterKey();
                            String clusterId = KafkaContext.clusterId(clusterConfig, Optional.of(kafka));
                            log.infof("Removing KafkaContext for cluster %s, id=%s", clusterKey, clusterId);
                            log.debugf("Known KafkaContext identifiers: %s", contexts.keySet());
                            KafkaContext previous = contexts.remove(clusterId);
                            Optional.ofNullable(previous).ifPresent(KafkaContext::close);
                        },
                        () -> log.debugf("Ignoring deleted Kafka resource %s, not found in configuration", Cache.metaNamespaceKeyFunc(kafka)));
            }

            Optional<KafkaClusterConfig> findConfig(Kafka kafka) {
                String clusterKey = Cache.metaNamespaceKeyFunc(kafka);
                return consoleConfig.getKafka().getCluster(clusterKey);
            }
        }, TimeUnit.MINUTES.toMillis(1));
    }

    void putKafkaContext(Map<String, KafkaContext> contexts,
            KafkaClusterConfig clusterConfig,
            Optional<Kafka> kafkaResource,
            Function<Map<String, Object>, Admin> adminBuilder) {

        var adminConfigs = buildConfig(AdminClientConfig.configNames(),
                clusterConfig,
                "admin",
                clusterConfig::getAdminProperties,
                requiredAdminConfig(),
                kafkaResource);

        Set<String> configNames = ConsumerConfig.configNames().stream()
                // Do not allow a group Id to be set for this application
                .filter(Predicate.not(ConsumerConfig.GROUP_ID_CONFIG::equals))
                .collect(Collectors.toSet());

        var consumerConfigs = buildConfig(configNames,
                clusterConfig,
                "consumer",
                clusterConfig::getConsumerProperties,
                requiredConsumerConfig(),
                kafkaResource);

        var producerConfigs = buildConfig(ProducerConfig.configNames(),
                clusterConfig,
                "producer",
                clusterConfig::getProducerProperties,
                requiredProducerConfig(),
                kafkaResource);

        Map<Class<?>, Map<String, Object>> clientConfigs = new HashMap<>();
        clientConfigs.put(Admin.class, Collections.unmodifiableMap(adminConfigs));
        clientConfigs.put(Consumer.class, Collections.unmodifiableMap(consumerConfigs));
        clientConfigs.put(Producer.class, Collections.unmodifiableMap(producerConfigs));

        String clusterKey = clusterConfig.clusterKey();
        String clusterId = KafkaContext.clusterId(clusterConfig, kafkaResource);

        if (validConfigs(kafkaResource, clusterConfig, clientConfigs)) {
            Admin admin = null;
            boolean globalConnection = establishGlobalConnection(adminConfigs);

            if (globalConnection) {
                admin = adminBuilder.apply(adminConfigs);
            }

            RegistryClientFacade registryClient = null;
            SchemaRegistryConfig registryConfig = null;

            if (clusterConfig.getSchemaRegistry() != null) {
                registryConfig = consoleConfig.getSchemaRegistries()
                        .stream()
                        .filter(registry -> registry.getName().equals(clusterConfig.getSchemaRegistry()))
                        .findFirst()
                        .orElseThrow();
                registryClient = registryClients.get(registryConfig.getName());
            }

            KafkaContext ctx = new KafkaContext(clusterConfig, kafkaResource.orElse(null), clientConfigs, admin);
            ctx.schemaRegistryClient(registryClient, registryConfig, mapper);

            if (clusterConfig.hasNamespace()) {
                ctx.prometheus(metricsService.createClient(consoleConfig, clusterConfig));
            } else if (clusterConfig.getMetricsSource() != null) {
                log.infof("Skipping setup of metrics client for cluster %s. Reason: namespace is required for metrics retrieval but none was provided", clusterKey);
            }

            KafkaContext previous = contexts.put(clusterId, ctx);

            if (previous == null) {
                log.infof("Added KafkaContext for cluster %s, id=%s, global=%s", clusterKey, clusterId, globalConnection);
            } else {
                log.infof("Replaced KafkaContext for cluster %s, id=%s, global=%s", clusterKey, clusterId, previous.admin() != null);
                previous.close();
            }
        }
    }

    boolean defaultedClusterId(KafkaClusterConfig clusterConfig, Optional<Kafka> kafkaResource) {
        return clusterConfig.getId() == null && kafkaResource.map(Kafka::getStatus).map(KafkaStatus::getClusterId).isEmpty();
    }

    Optional<Kafka> cachedKafkaResource(KafkaClusterConfig clusterConfig) {
        return clusterConfig.hasNamespace() ? kafkaInformer.map(SharedIndexInformer::getStore)
                .map(store -> store.getByKey(clusterConfig.clusterKey()))
                .or(() -> {
                    String key = clusterConfig.clusterKey();

                    if (kafkaInformer.isPresent()) {
                        log.infof("Kafka resource %s not found in Kubernetes cluster", key);
                    } else {
                        log.warnf("Configuration references Kubernetes Kafka resource %s, but Kubernetes access is disabled", key);
                    }

                    return Optional.empty();
                }) : Optional.empty();
    }

    boolean validConfigs(Optional<Kafka> kafkaResource, KafkaClusterConfig clusterConfig, Map<Class<?>, Map<String, Object>> clientConfigs) {
        boolean createContext = true;
        boolean missingListenerStatus = kafkaResource.isPresent()
                && getListenerStatus(kafkaResource, clusterConfig.getListener()).isEmpty();
        StringBuilder clientsMessage = new StringBuilder();

        if (missingListenerStatus) {
            clientsMessage.append("; Kafka resource has no status for listener '%s', bootstrap servers and trusted certificates could not be derived"
                    .formatted(clusterConfig.getListener()));
        }

        for (Map.Entry<Class<?>, Map<String, Object>> client : clientConfigs.entrySet()) {
            Class<?> clientType = client.getKey();
            Set<String> missing = findMissingRequiredConfigs(client.getValue());

            if (!missing.isEmpty()) {
                clientsMessage.append("; %s client is missing required properties: %s"
                        .formatted(clientType.getSimpleName(), missing));
                createContext = false;
            }

            if (truststoreRequired(client.getValue())) {
                clientsMessage.append("""
                        ; %s client is a secure/SSL connection, but no truststore configuration is available. \
                        The connection may fail if the Kafka cluster is using an untrusted certificate""".formatted(clientType.getSimpleName()));
            }
        }

        if (createContext) {
            if (!clientsMessage.isEmpty()) {
                clientsMessage.insert(0, "Some configuration may be missing for connection to cluster %s, connection attempts may fail".formatted(clusterConfig.clusterKey()));
                log.warn(clientsMessage.toString().trim());
            }
        } else {
            clientsMessage.insert(0, "Missing configuration detected for connection to cluster %s, no connection will be setup".formatted(clusterConfig.clusterKey()));
            log.error(clientsMessage.toString().trim());
        }

        return createContext;
    }

    Map<String, Object> requiredAdminConfig() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 5000);
        return configs;
    }

    Map<String, Object> requiredConsumerConfig() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        configs.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 50_000);
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configs.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);
        configs.put(KafkaSerdeConfig.ENABLE_HEADERS, "true");
        return configs;
    }

    Map<String, Object> requiredProducerConfig() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        configs.put(ProducerConfig.ACKS_CONFIG, "all");
        configs.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        configs.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        configs.put(ProducerConfig.RETRIES_CONFIG, 0);
        return configs;
    }

    static boolean establishGlobalConnection(Map<String, Object> configs) {
        if (!configs.containsKey(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
            return false;
        }

        if (truststoreRequired(configs)) {
            return false;
        }

        if (configs.containsKey(SaslConfigs.SASL_MECHANISM)) {
            return configs.containsKey(SaslConfigs.SASL_JAAS_CONFIG);
        }

        return false;
    }

    static Set<String> findMissingRequiredConfigs(Map<String, Object> configs) {
        Set<String> missing = new LinkedHashSet<>();

        if (!configs.containsKey(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
            missing.add(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG);
        }

        if (configs.containsKey(SaslConfigs.SASL_JAAS_CONFIG) && !configs.containsKey(SaslConfigs.SASL_MECHANISM)) {
            missing.add(SaslConfigs.SASL_MECHANISM);
        }

        return missing;
    }

    void disposeKafkaContexts(@Disposes Map<String, KafkaContext> contexts) {
        log.infof("Closing all known KafkaContexts");

        contexts.values().parallelStream().forEach(context -> {
            var key = context.clusterConfig().clusterKey();
            var id = context.clusterId();
            log.infof("Closing KafkaContext %s, id=%s", key, id);

            try {
                context.close();
            } catch (Exception e) {
                log.warnf("Exception occurred closing context %s, id=%s: %s", key, id, e.getMessage());
            }
        });
    }

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
    public KafkaContext produceKafkaContext(Map<String, KafkaContext> contexts,
            SecurityIdentity identity,
            UnaryOperator<Admin> filter,
            Function<Map<String, Object>, Admin> adminBuilder) {

        String clusterId = requestUri.getPathParameters().getFirst("clusterId");

        if (clusterId == null) {
            return KafkaContext.EMPTY;
        }

        KafkaContext ctx = contexts.get(clusterId);

        if (ctx == null) {
            throw NO_SUCH_KAFKA.apply(clusterId);
        }

        if (ctx.admin() == null) {
            /*
             * Admin may be null if credentials were not given in the
             * configuration. The user must provide the login secrets
             * in the request in that case.
             *
             * The identity should already carry the SASL credentials
             * at this point (set in ConsoleAuthenticationMechanism),
             * so here we will only retrieve them (if applicable) and
             * set them in the admin configuration map.
             */
            var adminConfigs = maybeAuthenticate(identity, ctx, Admin.class);
            var admin = adminBuilder.apply(adminConfigs);
            return new KafkaContext(ctx, filter.apply(admin));
        }

        return ctx;
    }

    public void disposeKafkaContext(@Disposes KafkaContext context, Map<String, KafkaContext> contexts) {
        if (!contexts.values().contains(context)) {
            var clusterKey = context.clusterConfig().clusterKey();
            if (context.applicationScoped()) {
                log.infof("Closing out-of-date KafkaContext: %s", clusterKey);
            } else {
                log.debugf("Closing request-scoped KafkaContext: %s", clusterKey);
            }
            context.close();
        }
    }

    @Produces
    @RequestScoped
    public Consumer<RecordData, RecordData> consumerSupplier(SecurityIdentity identity, KafkaContext context) {
        var configs = maybeAuthenticate(identity, context, Consumer.class);

        return new KafkaConsumer<>(
                configs,
                context.schemaRegistryContext().keyDeserializer(),
                context.schemaRegistryContext().valueDeserializer());
    }

    public void disposeConsumer(@Disposes Consumer<RecordData, RecordData> consumer) {
        consumer.close();
    }

    @Produces
    @RequestScoped
    public Producer<RecordData, RecordData> producerSupplier(SecurityIdentity identity, KafkaContext context) {
        var configs = maybeAuthenticate(identity, context, Producer.class);
        return new KafkaProducer<>(
                configs,
                context.schemaRegistryContext().keySerializer(),
                context.schemaRegistryContext().valueSerializer());
    }

    public void disposeProducer(@Disposes Producer<RecordData, RecordData> producer) {
        producer.close();
    }

    Map<String, Object> maybeAuthenticate(SecurityIdentity identity, KafkaContext context, Class<?> clientType) {
        Map<String, Object> configs = context.configs(clientType);

        if (configs.containsKey(SaslConfigs.SASL_MECHANISM)
                && !configs.containsKey(SaslConfigs.SASL_JAAS_CONFIG)) {
            configs = new HashMap<>(configs);
            configureAuthentication(identity, context.saslMechanism(clientType), configs);
        }

        return configs;
    }

    Map<String, Object> buildConfig(Set<String> configNames,
            KafkaClusterConfig config,
            String clientType,
            Supplier<Map<String, String>> clientProperties,
            Map<String, Object> overrideProperties,
            Optional<Kafka> cluster) {

        Map<String, Object> cfg = configNames
            .stream()
            .map(configName -> Optional.ofNullable(clientProperties.get().get(configName))
                    .or(() -> Optional.ofNullable(config.getProperties().get(configName)))
                    .or(() -> getDefaultConfig(clientType, configName))
                    .map(configValue -> Map.entry(configName, configValue)))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (k1, k2) -> k1, TreeMap::new));

        // Ensure no given properties are skipped. The previous stream processing allows
        // for the standard config names to be obtained from the given maps, but also from
        // config overrides via MicroProfile Config.
        clientProperties.get().forEach(cfg::putIfAbsent);
        config.getProperties().forEach(cfg::putIfAbsent);

        var listenerSpec = cluster.map(Kafka::getSpec)
                .map(KafkaSpec::getKafka)
                .map(KafkaClusterSpec::getListeners)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .filter(listener -> listener.getName().equals(config.getListener()))
                .findFirst();

        var listenerStatus = getListenerStatus(cluster, config.getListener());

        listenerSpec.ifPresent(listener -> applyListenerConfiguration(cfg, listener));

        listenerStatus.map(ListenerStatus::getBootstrapServers)
            .ifPresent(bootstrap -> cfg.putIfAbsent(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrap));

        listenerStatus.map(ListenerStatus::getCertificates)
            .filter(Objects::nonNull)
            .filter(Predicate.not(Collection::isEmpty))
            .map(certificates -> String.join("\n", certificates).trim())
            .ifPresent(certificates -> {
                cfg.putIfAbsent(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
                cfg.putIfAbsent(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, certificates);
            });

        if (truststoreRequired(cfg) && trustManager.isResolvable()
                && cfg.containsKey(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
            // trustManager is only resolvable in 'dev' mode
            trustManager.get().trustClusterCertificate(cfg);
        }

        cfg.putAll(overrideProperties);

        logConfig("%s[key=%s, id=%s]".formatted(
                clientType,
                config.clusterKey(),
                cluster.map(Kafka::getStatus).map(KafkaStatus::getClusterId).orElse("UNKNOWN")),
                cfg);

        return cfg;
    }

    Optional<ListenerStatus> getListenerStatus(Optional<Kafka> cluster, String listenerName) {
        return cluster.map(Kafka::getStatus)
                .map(KafkaStatus::getListeners)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .filter(listener -> listener.getName().equals(listenerName))
                .findFirst();
    }

    private void applyListenerConfiguration(Map<String, Object> cfg, GenericKafkaListener listener) {
        var authType = Optional.ofNullable(listener.getAuth())
                .map(KafkaListenerAuthentication::getType)
                .orElse("");
        boolean saslEnabled;

        switch (authType) {
            case "oauth":
                saslEnabled = true;
                cfg.putIfAbsent(SaslConfigs.SASL_MECHANISM, OAUTHBEARER);
                cfg.putIfAbsent(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, STRIMZI_OAUTH_CALLBACK);
                break;
            case "scram-sha-512":
                cfg.putIfAbsent(SaslConfigs.SASL_MECHANISM, SCRAM_SHA512);
                saslEnabled = true;
                break;
            default:
                saslEnabled = false;
                break;
        }

        StringBuilder protocol = new StringBuilder();

        if (saslEnabled) {
            protocol.append("SASL_");
        }

        if (listener.isTls()) {
            protocol.append(SecurityProtocol.SSL.name);
        } else {
            protocol.append(SecurityProtocol.PLAINTEXT.name);
        }

        cfg.putIfAbsent(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, protocol.toString());
    }

    Optional<String> getDefaultConfig(String clientType, String configName) {
        String clientSpecificKey = "console.kafka.%s.%s".formatted(clientType, configName);
        String generalKey = "console.kafka.%s".formatted(configName);

        return config.getOptionalValue(clientSpecificKey, String.class)
            .or(() -> config.getOptionalValue(generalKey, String.class))
            .map(this::unquote);
    }

    String unquote(String cfg) {
        return BOUNDARY_QUOTES.matcher(cfg).replaceAll("");
    }

    static boolean truststoreRequired(Map<String, Object> cfg) {
        if (cfg.containsKey(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG)) {
            return false;
        }

        return cfg.getOrDefault(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "")
                .toString()
                .contains("SSL");
    }

    void logConfig(String clientType, Map<String, Object> config) {
        if (log.isTraceEnabled()) {
            String msg = config.entrySet()
                .stream()
                .map(entry -> {
                    String value = String.valueOf(entry.getValue());

                    if (SaslConfigs.SASL_JAAS_CONFIG.equals(entry.getKey())) {
                        // Mask sensitive information in saas.jaas.config
                        Matcher m = EMBEDDED_STRING.matcher(value);
                        value = m.replaceAll("\"******\"");
                    }

                    return "\t%s = %s".formatted(entry.getKey(), value);
                })
                .collect(Collectors.joining("\n", "%s configuration:\n", ""));

            log.tracef(msg, clientType);
        }
    }

    void configureAuthentication(SecurityIdentity identity, String saslMechanism, Map<String, Object> configs) {
        SaslJaasConfigCredential credential = identity.getCredential(SaslJaasConfigCredential.class);

        switch (saslMechanism) {
            case OAUTHBEARER:
                configureOAuthBearer(credential, configs);
                break;
            case PLAIN:
                configureBasic(credential, configs);
                break;
            case SCRAM_SHA256, SCRAM_SHA512:
                configureBasic(credential, configs);
                break;
            default:
                throw new NotAuthorizedException("Unknown");
        }
    }

    void configureOAuthBearer(SaslJaasConfigCredential credential, Map<String, Object> configs) {
        log.trace("SASL/OAUTHBEARER enabled");

        configs.putIfAbsent(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, STRIMZI_OAUTH_CALLBACK);
        // Do not attempt token refresh ahead of expiration (ExpiringCredentialRefreshingLogin)
        // May still cause warnings to be logged when token will expire in less than SASL_LOGIN_REFRESH_MIN_PERIOD_SECONDS.
        configs.putIfAbsent(SaslConfigs.SASL_LOGIN_REFRESH_BUFFER_SECONDS, "0");

        configs.put(SaslConfigs.SASL_JAAS_CONFIG, credential.value());
    }

    void configureBasic(SaslJaasConfigCredential credential, Map<String, Object> configs) {
        log.trace("SASL/SCRAM enabled");
        configs.put(SaslConfigs.SASL_JAAS_CONFIG, credential.value());
    }

    private static final Pattern BOUNDARY_QUOTES = Pattern.compile("(^[\"'])|([\"']$)");
    private static final Pattern EMBEDDED_STRING = Pattern.compile("\"[^\"]*\"");
}
