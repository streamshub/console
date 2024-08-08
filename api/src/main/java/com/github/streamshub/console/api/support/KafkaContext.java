package com.github.streamshub.console.api.support;

import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.common.config.SaslConfigs;

import com.github.streamshub.console.config.KafkaClusterConfig;

import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaClusterSpec;
import io.strimzi.api.kafka.model.kafka.KafkaSpec;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListener;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationOAuth;
import io.strimzi.kafka.oauth.client.ClientConfig;

public class KafkaContext implements Closeable {

    public static final KafkaContext EMPTY = new KafkaContext(null, null, Collections.emptyMap(), null);

    final KafkaClusterConfig clusterConfig;
    final Kafka resource;
    final Map<Class<?>, Map<String, Object>> configs;
    final Admin admin;
    boolean applicationScoped;

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
}
