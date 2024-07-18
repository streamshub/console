package com.github.streamshub.console.api.support;

import java.io.Closeable;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.apache.kafka.clients.admin.Admin;

import com.github.streamshub.console.config.KafkaClusterConfig;

import io.strimzi.api.kafka.model.kafka.Kafka;

public class KafkaContext implements Closeable {

    public static final KafkaContext EMPTY = new KafkaContext(null, null, Collections.emptyMap(), null);

    final KafkaClusterConfig clusterConfig;
    final Kafka resource;
    final Map<Class<?>, Map<String, Object>> configs;
    final Admin admin;

    public KafkaContext(KafkaClusterConfig clusterConfig, Kafka resource, Map<Class<?>, Map<String, Object>> configs, Admin admin) {
        this.clusterConfig = clusterConfig;
        this.resource = resource;
        this.configs = Map.copyOf(configs);
        this.admin = admin;
    }

    public KafkaContext(KafkaContext other, Admin admin) {
        this(other.clusterConfig, other.resource, other.configs, admin);
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
}
