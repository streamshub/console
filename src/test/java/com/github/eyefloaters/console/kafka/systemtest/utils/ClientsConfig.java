package com.github.eyefloaters.console.kafka.systemtest.utils;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.Config;

import com.github.eyefloaters.console.legacy.KafkaAdminConfigRetriever;

import java.util.Base64;
import java.util.Properties;
import java.util.UUID;

public class ClientsConfig {
    public static Properties getConsumerConfig(Config config, String groupID) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getValue(KafkaAdminConfigRetriever.BOOTSTRAP_SERVERS, String.class));
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupID == null ? UUID.randomUUID().toString() : groupID);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 50_000);
        return props;
    }

    public static Properties getProducerConfig(Config config) {
        String serializer = StringSerializer.class.getName();
        return getProducerConfig(config, serializer, serializer);
    }

    public static Properties getProducerConfig(Config config, String keySerializer, String valueSerializer) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getValue(KafkaAdminConfigRetriever.BOOTSTRAP_SERVERS, String.class));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        return props;
    }

    public static Properties getAdminConfigOauth(Config config, String token) {
        Properties props = new Properties();

        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, config.getValue(KafkaAdminConfigRetriever.BOOTSTRAP_SERVERS, String.class));
        props.put(AdminClientConfig.METADATA_MAX_AGE_CONFIG, "30000");
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "30000");

        addOauthConfig(config, props, token);

        return props;
    }

    public static Properties getAdminConfig(Config config) {
        Properties conf = new Properties();
        conf.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, config.getValue(KafkaAdminConfigRetriever.BOOTSTRAP_SERVERS, String.class));
        conf.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "5000");
        return conf;
    }

    private static void addOauthConfig(Config config, Properties props, String token) {
        props.put(SaslConfigs.SASL_MECHANISM, "OAUTHBEARER");

        config.getOptionalValue(KafkaAdminConfigRetriever.BROKER_TRUSTED_CERT, String.class)
            // Value is base64 encoded to simulate configuration via Kubernetes env secret
            .map(Base64.getDecoder()::decode)
            .map(String::new)
            .ifPresentOrElse(trustedCert -> {
                props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_SSL");
                props.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, trustedCert);
                props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
            }, () -> props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT"));

        props.put(SaslConfigs.SASL_JAAS_CONFIG, "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required oauth.access.token=\"" + token + "\";");
        props.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, "io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler");
    }

    public static Properties getConsumerConfigOauth(Config config, String groupID, String token) {
        Properties props = getConsumerConfig(config, groupID);
        addOauthConfig(config, props, token);
        return props;
    }

    public static Properties getProducerConfigOauth(Config config, String token) {
        Properties props = getProducerConfig(config);
        addOauthConfig(config, props, token);
        return props;
    }
}

