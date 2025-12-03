package com.github.streamshub.console.api.support;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.config.SaslConfigs;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.console.api.support.serdes.ForceCloseable;
import com.github.streamshub.console.api.support.serdes.MultiformatDeserializer;
import com.github.streamshub.console.api.support.serdes.MultiformatSerializer;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.config.SchemaRegistryConfig;

import io.apicurio.registry.resolver.client.RegistryClientFacade;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaClusterSpec;
import io.strimzi.api.kafka.model.kafka.KafkaSpec;
import io.strimzi.api.kafka.model.kafka.KafkaStatus;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListener;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationOAuth;
import io.strimzi.kafka.oauth.client.ClientConfig;

public class KafkaContext implements Closeable {

    public static final KafkaContext EMPTY = new KafkaContext(null, null, Collections.emptyMap(), null);
    private static final Logger LOGGER = Logger.getLogger(KafkaContext.class);

    final KafkaClusterConfig clusterConfig;
    final Kafka resource;
    final Map<Class<?>, Map<String, Object>> configs;
    final Admin admin;
    boolean applicationScoped;
    SchemaRegistryContext schemaRegistryContext;
    PrometheusAPI prometheus;

    public KafkaContext(KafkaClusterConfig clusterConfig, Kafka resource, Map<Class<?>, Map<String, Object>> configs, Admin admin) {
        this.clusterConfig = clusterConfig;
        this.resource = resource;
        this.configs = Map.copyOf(configs);
        this.admin = admin;
        this.applicationScoped = true;
    }

    public KafkaContext(KafkaContext other, Admin admin) {
        this(other.clusterConfig, other.resource, other.configs, admin);
        this.applicationScoped = false;
        this.schemaRegistryContext = other.schemaRegistryContext;
        this.prometheus = other.prometheus;
    }

    public static String clusterId(KafkaClusterConfig clusterConfig, Optional<Kafka> kafkaResource) {
        return Optional.ofNullable(clusterConfig.getId())
                .or(() -> kafkaResource.map(Kafka::getStatus).map(KafkaStatus::getClusterId))
                .orElseGet(clusterConfig::clusterKeyEncoded);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof KafkaContext)) {
            return false;
        }

        KafkaContext other = (KafkaContext) obj;
        return Objects.equals(clusterConfig, other.clusterConfig)
                && Objects.equals(resource, other.resource)
                && Objects.equals(configs, other.configs)
                && Objects.equals(admin, other.admin);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterConfig, resource, configs, admin);
    }

    @Override
    public void close() {
        if (admin != null) {
            admin.close();
        }
        /*
         * Do not close the registry context when the KafkaContext has client-provided
         * credentials. I.e., only close when the context is global (with configured
         * credentials).
         */
        if (applicationScoped && schemaRegistryContext != null) {
            try {
                schemaRegistryContext.close();
            } catch (IOException e) {
                LOGGER.warnf("Exception closing schema registry context: %s", e.getMessage());
            }
        }
    }

    public String clusterId() {
        return clusterId(clusterConfig, Optional.ofNullable(resource));
    }

    public KafkaClusterConfig clusterConfig() {
        return clusterConfig;
    }

    public Kafka resource() {
        return resource;
    }

    public Map<Class<?>, Map<String, Object>> configs() {
        return configs;
    }

    public Map<String, Object> configs(Class<?> type) {
        return configs.get(type);
    }

    public Admin admin() {
        return admin;
    }

    public boolean applicationScoped() {
        return applicationScoped;
    }

    public void schemaRegistryClient(RegistryClientFacade registryClient, SchemaRegistryConfig config, ObjectMapper objectMapper) {
        schemaRegistryContext = new SchemaRegistryContext(registryClient, config, objectMapper);
    }

    public SchemaRegistryContext schemaRegistryContext() {
        return schemaRegistryContext;
    }

    public void prometheus(PrometheusAPI prometheus) {
        this.prometheus = prometheus;
    }

    public PrometheusAPI prometheus() {
        return prometheus;
    }

    public String saslMechanism(Class<?> clientType) {
        return configs(clientType).get(SaslConfigs.SASL_MECHANISM) instanceof String auth ? auth : "";
    }

    public boolean hasCredentials(Class<?> clientType) {
        return configs(clientType).get(SaslConfigs.SASL_JAAS_CONFIG) instanceof String;
    }

    public Optional<String> tokenUrl() {
        return Optional.ofNullable(clusterConfig.getProperties().get(ClientConfig.OAUTH_TOKEN_ENDPOINT_URI))
            .or(() -> Optional.ofNullable(resource())
                    .map(Kafka::getSpec)
                    .map(KafkaSpec::getKafka)
                    .map(KafkaClusterSpec::getListeners)
                    .map(Collection::stream)
                    .orElseGet(Stream::empty)
                    .filter(listener -> listener.getName().equals(clusterConfig.getListener()))
                    .findFirst()
                    .map(GenericKafkaListener::getAuth)
                    .filter(KafkaListenerAuthenticationOAuth.class::isInstance)
                    .map(KafkaListenerAuthenticationOAuth.class::cast)
                    .map(KafkaListenerAuthenticationOAuth::getTokenEndpointUri));
    }

    public String securityResourcePath(String subresource) {
        return "kafkas/" + clusterId() + '/' + subresource;
    }

    public String auditDisplayResourcePath(String subresource) {
        return "kafkas/[" + clusterConfig.clusterKey() + "]/" + subresource;
    }

    /**
     * The SchemaRegistryContext contains a per-Kafka registry client
     * and key/value SerDes classes to be used to handle message browsing.
     *
     * The client and SerDes instances will be kept open and reused until
     * the parent KafkaContext is disposed of at application shutdown.
     */
    public class SchemaRegistryContext implements Closeable {
        private final SchemaRegistryConfig config;
        private final RegistryClientFacade registryClient;
        private final MultiformatDeserializer keyDeserializer;
        private final MultiformatDeserializer valueDeserializer;
        private final MultiformatSerializer keySerializer;
        private final MultiformatSerializer valueSerializer;

        SchemaRegistryContext(RegistryClientFacade registryClient, SchemaRegistryConfig config, ObjectMapper objectMapper) {
            this.config = config;
            this.registryClient = registryClient;

            keyDeserializer = new MultiformatDeserializer(registryClient, objectMapper);
            keyDeserializer.configure(configs(Consumer.class), true);

            valueDeserializer = new MultiformatDeserializer(registryClient, objectMapper);
            valueDeserializer.configure(configs(Consumer.class), false);

            keySerializer = new MultiformatSerializer(registryClient, objectMapper);
            keySerializer.configure(configs(Producer.class), true);

            valueSerializer = new MultiformatSerializer(registryClient, objectMapper);
            valueSerializer.configure(configs(Producer.class), false);
        }

        public SchemaRegistryConfig getConfig() {
            return config;
        }

        public RegistryClientFacade registryClient() {
            return registryClient;
        }

        public MultiformatDeserializer keyDeserializer() {
            return keyDeserializer;
        }

        public MultiformatDeserializer valueDeserializer() {
            return valueDeserializer;
        }

        public MultiformatSerializer keySerializer() {
            return keySerializer;
        }

        public MultiformatSerializer valueSerializer() {
            return valueSerializer;
        }

        @Override
        public void close() throws IOException {
            if (registryClient != null) {
                // nothing to close otherwise
                closeOptionally("key deserializer", keyDeserializer);
                closeOptionally("value deserializer", valueDeserializer);
                closeOptionally("key serializer", keySerializer);
                closeOptionally("value serializer", valueSerializer);
            }
        }

        private void closeOptionally(String name, ForceCloseable closeable) {
            if (closeable != null) {
                try {
                    closeable.forceClose();
                } catch (Exception e) {
                    LOGGER.infof("Exception closing resource %s: %s", name, e.getMessage());
                }
            }
        }
    }
}
