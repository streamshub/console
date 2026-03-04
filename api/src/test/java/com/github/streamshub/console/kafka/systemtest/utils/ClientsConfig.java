package com.github.streamshub.console.kafka.systemtest.utils;

import java.util.Properties;
import java.util.UUID;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.Config;

public class ClientsConfig {

    private static Properties bootstrap(Config config) {
        Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        return props;
    }

    public static Properties getConsumerConfig(Config config, String groupID) {
        Properties props = bootstrap(config);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupID == null ? UUID.randomUUID().toString() : groupID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return props;
    }

    public static Properties getProducerConfig(Config config) {
        String serializer = StringSerializer.class.getName();
        return getProducerConfig(config, serializer, serializer);
    }

    public static Properties getProducerConfig(Config config, String keySerializer, String valueSerializer) {
        Properties props = bootstrap(config);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return props;
    }

    public static Properties getAdminConfig(Config config) {
        Properties props = bootstrap(config);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
        // Set very low values to reduce risk of out-dated metadata
        props.put(AdminClientConfig.METADATA_MAX_AGE_CONFIG, "0");
        props.put(AdminClientConfig.RETRY_BACKOFF_MS_CONFIG, "0");
        return props;
    }
}
