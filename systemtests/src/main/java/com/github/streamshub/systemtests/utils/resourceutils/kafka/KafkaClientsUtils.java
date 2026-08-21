package com.github.streamshub.systemtests.utils.resourceutils.kafka;

import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.ScramMechanism;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.Logger;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;

import io.fabric8.kubernetes.api.model.Secret;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaStatus;

public class KafkaClientsUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaClientsUtils.class);

    private KafkaClientsUtils() {}
    /**
     * Retrieves and constructs a SCRAM-SHA-512 configuration string for a Kafka user from a Kubernetes Secret.
     *
     * <p>This method accesses the Kubernetes {@code Secret} for the specified Kafka user in the given namespace,</p>
     * <p>decodes the {@code sasl.jaas.config} entry from Base64, and builds a full SCRAM-SHA-512 SASL configuration string</p>
     * <p>including the provided {@code security.protocol} value.</p>
     *
     * <p>The resulting configuration string can be used to authenticate Kafka clients with SCRAM-SHA-512.</p>
     *
     * @param namespace the Kubernetes namespace where the Kafka user secret resides
     * @param userName the name of the Kafka user (and the corresponding secret resource)
     * @param securityProtocol the security protocol to be included (e.g., PLAINTEXT, SASL_SSL)
     * @return a multi-line SASL configuration string for the Kafka client using SCRAM-SHA-512
     */
    public static String getScramShaConfig(String namespace, String userName, SecurityProtocol securityProtocol) {
        // Note: the decoded JAAS config contains a credential and is intentionally not logged
        LOGGER.debug("Building SCRAM-SHA-512 client config for user {}/{} with security protocol {}", namespace, userName, securityProtocol);
        final String saslJaasConfigDecrypted = Utils.decodeFromBase64(ResourceUtils.getKubeResource(Secret.class, namespace, userName).getData().get(SaslConfigs.SASL_JAAS_CONFIG));
        return SaslConfigs.SASL_MECHANISM + "=" + ScramMechanism.SCRAM_SHA_512.mechanismName() + "\n" +
            CommonClientConfigs.SECURITY_PROTOCOL_CONFIG + "=" + securityProtocol + "\n" +
            SaslConfigs.SASL_JAAS_CONFIG + "=" + saslJaasConfigDecrypted + "\n";
    }

    public static Producer<String, String> stringProducer(Properties properties) {
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        return new KafkaProducer<>(properties);
    }

    public static Consumer<String, String> stringConsumer(Properties properties) {
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        return new KafkaConsumer<>(properties);
    }

    public static <C> C createSecureClient(TestCaseConfig tcc, Function<Properties, C> factory) {
        return createSecureClient(tcc.namespace(), tcc.kafkaName(), tcc.kafkaUserName(), factory);
    }

    public static <C> C createSecureClient(String namespace, String kafkaName, String userName, Function<Properties, C> factory) {
        var kafka = ResourceUtils.getKubeResource(Kafka.class, namespace, kafkaName);

        var bootstrapServers = Optional.ofNullable(kafka.getStatus())
                .map(KafkaStatus::getListeners)
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(l -> Constants.SECURE_LISTENER_NAME.equals(l.getName()))
                .map(l -> l.getBootstrapServers())
                .findFirst()
                .orElseThrow();

        String saslJaasConfig = Utils.decodeFromBase64(ResourceUtils.getKubeResource(
                Secret.class,
                namespace,
                userName)
            .getData()
            .get(SaslConfigs.SASL_JAAS_CONFIG));

        var caCertificate = Utils.decodeFromBase64(ResourceUtils.getKubeResource(
                Secret.class,
                namespace,
                kafkaName + "-trustbundle")
            .getData()
            .get("cluster-ca.crt"));

        Properties properties = new Properties();
        properties.setProperty(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.setProperty(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SASL_SSL.name);
        properties.setProperty(SaslConfigs.SASL_MECHANISM, ScramMechanism.SCRAM_SHA_512.mechanismName());
        properties.setProperty(SaslConfigs.SASL_JAAS_CONFIG, saslJaasConfig);
        properties.setProperty(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, caCertificate);
        properties.setProperty(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");

        return factory.apply(properties);
    }
}
