package com.github.eyefloaters.console.api;

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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
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
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.eyefloaters.console.api.config.ConsoleConfig;
import com.github.eyefloaters.console.api.config.KafkaClusterConfig;
import com.github.eyefloaters.console.api.service.KafkaClusterService;
import com.github.eyefloaters.console.api.support.TrustAllCertificateManager;

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

    static final String NO_SUCH_KAFKA_MESSAGE = "Requested Kafka cluster %s does not exist or is not configured";
    private final Function<String, NotFoundException> noSuchKafka =
            clusterName -> new NotFoundException(NO_SUCH_KAFKA_MESSAGE.formatted(clusterName));

    @Inject
    Logger log;

    @Inject
    Config config;

    @Inject
    @ConfigProperty(name = "console.config-path")
    Optional<String> configPath;

    @Inject
    SharedIndexInformer<Kafka> kafkaInformer;

    @Inject
    KafkaClusterService kafkaClusterService;

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
            .orElseGet(() -> {
                log.infof("Console configuration not specified");
                return new ConsoleConfig();
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
    public Supplier<Kafka> kafkaResourceSupplier() {
        String clusterId = requestUri.getPathParameters().getFirst("clusterId");

        if (clusterId == null) {
            throw new IllegalStateException("Admin client was accessed, "
                    + "but the requested operation does not provide a Kafka cluster ID");
        }

        Kafka cluster = kafkaClusterService.findCluster(clusterId)
                .orElseThrow(() -> noSuchKafka.apply(clusterId));

        return () -> cluster;
    }

    @Produces
    @ApplicationScoped
    Map<String, Admin> getAdmins(ConsoleConfig consoleConfig, Function<Map<String, Object>, Admin> adminBuilder) {
        final Map<String, Admin> adminClients = new HashMap<>();

        kafkaInformer.addEventHandlerWithResyncPeriod(new ResourceEventHandler<Kafka>() {
            public void onAdd(Kafka kafka) {
                put(kafka, "Adding");
            }

            public void onUpdate(Kafka oldKafka, Kafka newKafka) {
                put(newKafka, "Updating");
            }

            private void put(Kafka kafka, String eventType) {
                String clusterKey = Cache.metaNamespaceKeyFunc(kafka);

                consoleConfig.getKafka()
                    .getCluster(clusterKey)
                    .map(e -> {
                        var configs = buildConfig(AdminClientConfig.configNames(), e, "admin", e::getAdminProperties, kafka);

                        if (truststoreRequired(configs)) {
                            log.warnf("""
                                    %s Admin client for Kafka cluster %s failed. Connection \
                                    requires truststore which could not be obtained from the \
                                    Kafka resource status.
                                    """
                                    .formatted(eventType, kafka.getStatus().getClusterId()));
                            return null;
                        } else {
                            logConfig("Admin[name=%s, namespace=%s, id=%s]".formatted(
                                    e.getName(),
                                    e.getNamespace(),
                                    kafka.getStatus().getClusterId()),
                                    configs);
                            return adminBuilder.apply(configs);
                        }
                    })
                    .ifPresent(client -> {
                        log.info("%s Admin client for Kafka cluster %s".formatted(eventType, kafka.getStatus().getClusterId()));
                        Admin previous = adminClients.put(clusterKey, client);
                        Optional.ofNullable(previous).ifPresent(Admin::close);
                    });
            }

            public void onDelete(Kafka kafka, boolean deletedFinalStateUnknown) {
                String clusterKey = Cache.metaNamespaceKeyFunc(kafka);
                log.info("Removing Admin client for Kafka cluster %s".formatted(kafka.getStatus().getClusterId()));
                Admin admin = adminClients.remove(clusterKey);
                Optional.ofNullable(admin).ifPresent(Admin::close);
            }
        }, TimeUnit.MINUTES.toMillis(1));

        return adminClients;
    }

    void closeAdmins(@Disposes Map<String, Admin> admins) {
        admins.values().parallelStream().forEach(admin -> {
            try {
                admin.close();
            } catch (Exception e) {
                log.warnf("Exception occurred closing admin: %s", e.getMessage());
            }
        });
    }

    @Produces
    @RequestScoped
    public Supplier<Admin> adminClientSupplier(Supplier<Kafka> cluster, Map<String, Admin> admins, UnaryOperator<Admin> filter) {
        String clusterKey = Cache.metaNamespaceKeyFunc(cluster.get());

        return Optional.ofNullable(admins.get(clusterKey))
            .map(filter::apply)
            .<Supplier<Admin>>map(client -> () -> client)
            .orElseThrow(() -> noSuchKafka.apply(cluster.get().getStatus().getClusterId()));
    }

    public void adminClientDisposer(@Disposes Supplier<Admin> client, Map<String, Admin> admins) {
        Admin admin = client.get();

        if (!admins.values().contains(admin)) {
            admin.close();
        }
    }

    @Produces
    @RequestScoped
    public Supplier<Consumer<byte[], byte[]>> consumerSupplier(ConsoleConfig consoleConfig, Supplier<Kafka> cluster) {
        String clusterKey = Cache.metaNamespaceKeyFunc(cluster.get());

        return consoleConfig.getKafka()
            .getCluster(clusterKey)
            .<Supplier<Consumer<byte[], byte[]>>>map(e -> {

                Set<String> configNames = ConsumerConfig.configNames().stream()
                        // Do not allow a group Id to be set for this application
                        .filter(Predicate.not(ConsumerConfig.GROUP_ID_CONFIG::equals))
                        .collect(Collectors.toSet());

                var configs = buildConfig(configNames, e, "consumer", e::getConsumerProperties, cluster.get());
                configs.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");
                configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
                configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
                configs.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 50_000);
                configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
                configs.put(ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 5000);

                logConfig("Consumer[name=%s, namespace=%s]".formatted(
                        e.getName(),
                        e.getNamespace()),
                        configs);
                @SuppressWarnings("resource") // no resource leak - client closed by disposer
                Consumer<byte[], byte[]> client = new KafkaConsumer<>(configs);
                return () -> client;
            })
            .orElseThrow(() -> noSuchKafka.apply(cluster.get().getStatus().getClusterId()));
    }

    public void consumerDisposer(@Disposes Supplier<Consumer<byte[], byte[]>> consumer) {
        consumer.get().close();
    }

    @Produces
    @RequestScoped
    public Supplier<Producer<String, String>> producerSupplier(ConsoleConfig consoleConfig, Supplier<Kafka> cluster) {
        String clusterKey = Cache.metaNamespaceKeyFunc(cluster.get());

        return consoleConfig.getKafka()
            .getCluster(clusterKey)
            .<Supplier<Producer<String, String>>>map(e -> {
                var configs = buildConfig(ProducerConfig.configNames(), e, "producer", e::getProducerProperties, cluster.get());
                configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                configs.put(ProducerConfig.ACKS_CONFIG, "all");
                configs.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
                configs.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
                configs.put(ProducerConfig.RETRIES_CONFIG, 0);

                logConfig("Producer[name=%s, namespace=%s]".formatted(
                        e.getName(),
                        e.getNamespace()),
                        configs);
                @SuppressWarnings("resource") // no resource leak - client closed by disposer
                Producer<String, String> client = new KafkaProducer<>(configs);
                return () -> client;
            })
            .orElseThrow(() -> noSuchKafka.apply(cluster.get().getStatus().getClusterId()));
    }

    public void producerDisposer(@Disposes Supplier<Producer<String, String>> producer) {
        producer.get().close();
    }

    Map<String, Object> buildConfig(Set<String> configNames,
            KafkaClusterConfig config,
            String clientType,
            Supplier<Map<String, String>> clientProperties,
            Kafka cluster) {

        Map<String, Object> cfg = configNames
            .stream()
            .map(configName -> Optional.ofNullable(clientProperties.get().get(configName))
                    .or(() -> Optional.ofNullable(config.getProperties().get(configName)))
                    .or(() -> getDefaultConfig(clientType, configName))
                    .map(configValue -> Map.entry(configName, configValue)))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        if (!cfg.containsKey(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG)) {
            Optional.ofNullable(cluster.getStatus())
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
                Optional.ofNullable(cluster.getStatus())
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
            }
        }

        return cfg;
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

    boolean truststoreRequired(Map<String, Object> cfg) {
        if (cfg.containsKey(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG)) {
            return false;
        }

        return cfg.getOrDefault(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "")
                .toString()
                .contains("SSL");
    }

    void logConfig(String clientType, Map<String, Object> config) {
        if (log.isDebugEnabled()) {
            String msg = config.entrySet()
                .stream()
                .map(entry -> "\t%s = %s".formatted(entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n", "%s configuration:\n", ""));
            log.debugf(msg, clientType);
        }
    }

    private static final Pattern BOUNDARY_QUOTES = Pattern.compile("(^[\"'])|([\"']$)");

}
