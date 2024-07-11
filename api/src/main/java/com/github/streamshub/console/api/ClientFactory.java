package com.github.streamshub.console.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
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
import jakarta.ws.rs.NotFoundException;
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
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.streamshub.console.api.service.KafkaClusterService;
import com.github.streamshub.console.api.support.Holder;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.TrustAllCertificateManager;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;

import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaStatus;
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

    static final String NO_SUCH_KAFKA_MESSAGE = "Requested Kafka cluster %s does not exist or is not configured";
    private final Function<String, NotFoundException> noSuchKafka =
            clusterName -> new NotFoundException(NO_SUCH_KAFKA_MESSAGE.formatted(clusterName));

    @Inject
    Logger log;

    @Inject
    Config config;

    @Inject
    ScheduledExecutorService scheduler;

    @Inject
    @ConfigProperty(name = "console.config-path")
    Optional<String> configPath;

    @Inject
    Holder<SharedIndexInformer<Kafka>> kafkaInformer;

    @Inject
    KafkaClusterService kafkaClusterService;

    @Inject
    Instance<TrustAllCertificateManager> trustManager;

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

    @Produces
    @ApplicationScoped
    public ConsoleConfig produceConsoleConfig() {
        return configPath.map(Path::of)
            .map(Path::toUri)
            .map(uri -> {
                try {
                    return uri.toURL();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            })
            .filter(Objects::nonNull)
            .map(url -> {
                log.infof("Loading console configuration from %s", url);

                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

                try (InputStream stream = url.openStream()) {
                    return mapper.readValue(stream, ConsoleConfig.class);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            })
            .map(consoleConfig -> {
                consoleConfig.getKafka().getClusters().stream().forEach(cluster -> {
                    resolveValues(cluster.getProperties());
                    resolveValues(cluster.getAdminProperties());
                    resolveValues(cluster.getProducerProperties());
                    resolveValues(cluster.getConsumerProperties());
                });

                return consoleConfig;
            })
            .orElseGet(() -> {
                log.infof("Console configuration not specified");
                return new ConsoleConfig();
            });
    }

    @Produces
    @ApplicationScoped
    Map<String, KafkaContext> produceKafkaContexts(ConsoleConfig consoleConfig,
            Function<Map<String, Object>, Admin> adminBuilder) {

        final Map<String, KafkaContext> contexts = new ConcurrentHashMap<>();

        if (kafkaInformer.isPresent()) {
            addKafkaEventHandler(contexts, consoleConfig, adminBuilder);
        }

        // Configure clusters that will not be configured by events
        consoleConfig.getKafka().getClusters()
            .stream()
            .filter(c -> cachedKafkaResource(c).isEmpty())
            .forEach(clusterConfig -> putKafkaContext(contexts,
                        clusterConfig,
                        Optional.empty(),
                        adminBuilder,
                        false));

        return contexts;
    }

    void addKafkaEventHandler(Map<String, KafkaContext> contexts,
            ConsoleConfig consoleConfig,
            Function<Map<String, Object>, Admin> adminBuilder) {

        kafkaInformer.get().addEventHandlerWithResyncPeriod(new ResourceEventHandler<Kafka>() {
            public void onAdd(Kafka kafka) {
                findConfig(kafka).ifPresent(clusterConfig -> putKafkaContext(contexts,
                            clusterConfig,
                            Optional.of(kafka),
                            adminBuilder,
                            false));
            }

            public void onUpdate(Kafka oldKafka, Kafka newKafka) {
                findConfig(newKafka).ifPresent(clusterConfig -> putKafkaContext(contexts,
                            clusterConfig,
                            Optional.of(newKafka),
                            adminBuilder,
                            true));
            }

            public void onDelete(Kafka kafka, boolean deletedFinalStateUnknown) {
                findConfig(kafka).ifPresent(clusterConfig -> {
                    String clusterKey = clusterConfig.clusterKey();
                    String clusterId = Optional.ofNullable(clusterConfig.getId())
                            .or(() -> Optional.ofNullable(kafka.getStatus()).map(KafkaStatus::getClusterId))
                            .orElse(null);
                    log.infof("Removing KafkaContext for cluster %s, id=%s", clusterKey, clusterId);
                    log.debugf("Known KafkaContext identifiers: %s", contexts.keySet());
                    KafkaContext previous = contexts.remove(clusterId);
                    Optional.ofNullable(previous).ifPresent(KafkaContext::close);
                });
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
            Function<Map<String, Object>, Admin> adminBuilder,
            boolean replace) {

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
        clientConfigs.put(Admin.class, adminConfigs);
        clientConfigs.put(Consumer.class, consumerConfigs);
        clientConfigs.put(Producer.class, producerConfigs);

        Admin admin = null;

        if (establishGlobalConnection(clusterConfig, adminConfigs)) {
            admin = adminBuilder.apply(adminConfigs);
        }

        String clusterKey = clusterConfig.clusterKey();
        String clusterId = Optional.ofNullable(clusterConfig.getId())
                .or(() -> kafkaResource.map(Kafka::getStatus).map(KafkaStatus::getClusterId))
                .orElse(null);

        if (clusterId == null) {
            log.warnf("""
                    Ignoring Kafka cluster %s. Cluster id value missing in \
                    configuration and no Strimzi Kafka resources found with matching \
                    name and namespace.""", clusterKey);
        } else if (!replace && contexts.containsKey(clusterId)) {
            log.warnf("""
                    Ignoring duplicate Kafka cluster id: %s for cluster %s. Cluster id values in \
                    configuration must be unique and may not match id values of \
                    clusters discovered using Strimzi Kafka Kubernetes API resources.""", clusterId, clusterKey);
        } else if (truststoreRequired(adminConfigs)) {
            if (contexts.containsKey(clusterId) && !truststoreRequired(contexts.get(clusterId).configs(Admin.class))) {
                log.warnf("""
                        Ignoring update to Kafka custom resource %s. Connection requires \
                        trusted certificate which is no longer available.""", clusterKey);
            }
        } else {
            KafkaContext ctx = new KafkaContext(clusterConfig, kafkaResource.orElse(null), clientConfigs, admin);
            log.infof("%s KafkaContext for cluster %s, id=%s", replace ? "Replacing" : "Adding", clusterKey, clusterId);
            KafkaContext previous = contexts.put(clusterId, ctx);
            Optional.ofNullable(previous).ifPresent(KafkaContext::close);
        }
    }

    Optional<Kafka> cachedKafkaResource(KafkaClusterConfig clusterConfig) {
        return kafkaInformer.map(SharedIndexInformer::getStore)
                .map(store -> {
                    String key = clusterConfig.clusterKey();
                    Kafka resource = store.getByKey(key);
                    if (resource == null) {
                        log.warnf("Configuration references Kafka resource %s, but it was not found in cache", key);
                    }
                    return resource;
                });
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

    static boolean establishGlobalConnection(KafkaClusterConfig clusterConfig, Map<String, Object> configs) {
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

    void disposeKafkaContexts(@Disposes Map<String, KafkaContext> contexts) {
        log.infof("Closing all known KafkaContexts");

        contexts.values().parallelStream().forEach(context -> {
            log.infof("Closing KafkaContext %s", Cache.metaNamespaceKeyFunc(context.resource()));
            try {
                context.close();
            } catch (Exception e) {
                log.warnf("Exception occurred closing context: %s", e.getMessage());
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
            UnaryOperator<Admin> filter,
            Function<Map<String, Object>, Admin> adminBuilder) {

        String clusterId = requestUri.getPathParameters().getFirst("clusterId");

        if (clusterId == null) {
            return KafkaContext.EMPTY;
        }

        return Optional.ofNullable(contexts.get(clusterId))
                .map(ctx -> {
                    Admin admin = Optional.ofNullable(ctx.admin())
                            /*
                             * Admin may be null if credentials were not given
                             * in the configuration. The user must provide the
                             * login secrets in the request in that case.
                             */
                            .orElseGet(() -> adminBuilder.apply(ctx.configs(Admin.class)));
                    return new KafkaContext(ctx, filter.apply(admin));
                })
                .orElseThrow(() -> noSuchKafka.apply(clusterId));
    }

    public void disposeKafkaContext(@Disposes KafkaContext context, Map<String, KafkaContext> contexts) {
        if (!contexts.values().contains(context)) {
            log.infof("Closing out-of-date KafkaContext %s", Cache.metaNamespaceKeyFunc(context.resource()));
            context.close();
        }
    }

    @Produces
    @RequestScoped
    public Supplier<Consumer<byte[], byte[]>> consumerSupplier(ConsoleConfig consoleConfig, KafkaContext context) {
        var configs = context.configs(Consumer.class);
        Consumer<byte[], byte[]> client = new KafkaConsumer<>(configs);
        return () -> client;
    }

    public void consumerDisposer(@Disposes Supplier<Consumer<byte[], byte[]>> consumer) {
        consumer.get().close();
    }

    @Produces
    @RequestScoped
    public Supplier<Producer<String, String>> producerSupplier(ConsoleConfig consoleConfig, KafkaContext context) {
        var configs = context.configs(Producer.class);
        Producer<String, String> client = new KafkaProducer<>(configs);
        return () -> client;
    }

    public void producerDisposer(@Disposes Supplier<Producer<String, String>> producer) {
        producer.get().close();
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

        if (!cfg.containsKey(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
            cluster.map(Kafka::getStatus)
                .map(KafkaStatus::getListeners)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .filter(listener -> listener.getName().equals(config.getListener()))
                .map(ListenerStatus::getBootstrapServers)
                .findFirst()
                .ifPresent(bootstrapServers -> cfg.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));
        }

        if (truststoreRequired(cfg)) {
            if (trustManager.isResolvable()) {
                trustManager.get().trustClusterCertificate(cfg);
            } else {
                cluster.map(Kafka::getStatus)
                    .map(KafkaStatus::getListeners)
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .filter(listener -> {
                        if (listener.getName().equals(config.getListener())) {
                            return true;
                        }

                        return cfg.getOrDefault(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, "")
                                .toString()
                                .contains(listener.getBootstrapServers());
                    })
                    .map(ListenerStatus::getCertificates)
                    .filter(Objects::nonNull)
                    .filter(Predicate.not(Collection::isEmpty))
                    .findFirst()
                    .ifPresent(certificates -> {
                        cfg.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
                        cfg.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, String.join("\n", certificates).trim());
                    });

                if (truststoreRequired(cfg)) {
                    log.warnf("""
                            Failed to set configuration for %s client to Kafka cluster %s. Connection \
                            requires truststore which could not be obtained from the Kafka resource status."""
                            .formatted(clientType, config.clusterKey()));
                }
            }
        }

        cfg.putAll(overrideProperties);

        logConfig("%s[key=%s, id=%s]".formatted(
                clientType,
                config.clusterKey(),
                cluster.map(Kafka::getStatus).map(KafkaStatus::getClusterId).orElse("UNKNOWN")),
                cfg);

        return cfg;
    }

    private void resolveValues(Map<String, String> properties) {
        properties.entrySet().forEach(entry ->
            entry.setValue(resolveValue(entry.getValue())));
    }

    /**
     * If the given value is an expression referencing a configuration value,
     * replace it with the target property value.
     *
     * @param value configuration value that may be a reference to another
     *              configuration property
     * @return replacement property or the same value if the given string is not a
     *         reference.
     */
    private String resolveValue(String value) {
        if (value.startsWith("${") && value.endsWith("}")) {
            String replacement = value.substring(2, value.length() - 1);
            return config.getOptionalValue(replacement, String.class).orElse(value);
        }

        return value;
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

    private static final Pattern BOUNDARY_QUOTES = Pattern.compile("(^[\"'])|([\"']$)");
    private static final Pattern EMBEDDED_STRING = Pattern.compile("\"[^\"]*\"");
}
