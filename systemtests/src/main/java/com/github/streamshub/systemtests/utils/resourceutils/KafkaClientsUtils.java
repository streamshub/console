package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.clients.KafkaClientsBuilder;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.Utils;
import io.fabric8.kubernetes.api.model.Secret;
import io.skodjob.testframe.TestFrameConstants;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.logging.log4j.Logger;

import java.util.UUID;

public class KafkaClientsUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaClientsUtils.class);

    private KafkaClientsUtils() {}

    public static long timeoutForClientFinishJob(int messagesCount) {
        return (long) messagesCount * (TestFrameConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS + TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM);
    }

    public static KafkaClientsBuilder scramShaPlainTextClientBuilder(String namespace, String kafkaName, String kafkaUser, String topicName, int messageCount) {
        KafkaClientsBuilder builder = plainClientBuilder(namespace, topicName, topicName + "-producer", topicName + "-consumer", KafkaUtils.getPlainScramShaBootstrapAddress(kafkaName), messageCount, 0);

        // Add scram sha config
        return builder
            .withUsername(kafkaUser)
            .withAdditionalConfig(builder.getAdditionalConfig() + getScramShaConfig(namespace, kafkaUser, SecurityProtocol.SASL_PLAINTEXT));
    }

    private static String getScramShaConfig(String namespace, String userName, SecurityProtocol securityProtocol) {
        final String saslJaasConfigDecrypted = Utils.decodeFromBase64(ResourceUtils.getKubeResource(Secret.class, namespace, userName).getData().get("sasl.jaas.config"));
        return  "sasl.mechanism=SCRAM-SHA-512\n" +
            "security.protocol=" + securityProtocol + "\n" +
            "sasl.jaas.config=" + saslJaasConfigDecrypted + "\n";
    }

    public static KafkaClientsBuilder plainClientBuilder(String namespace, String topicName, String producer, String consumer, String bootstrap, int messageCount, int delayMs) {
        String uid = UUID.randomUUID().toString().substring(0, 4);
        return new KafkaClientsBuilder()
            .withNamespaceName(namespace)
            .withTopicName(topicName)
            .withMessageCount(messageCount)
            .withDelayMs(delayMs)
            .withProducerName(producer)
            .withConsumerName(consumer)
            .withConsumerGroup(consumer + "-group-" + uid)
            .withAdditionalConfig("group.instance.id=group-" + uid + "\n")
            .withBootstrapAddress(bootstrap);
    }
}
