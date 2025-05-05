package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePoolBuilder;
import org.apache.logging.log4j.Logger;

import static com.github.streamshub.systemtests.utils.WaitUtils.waitForKafkaReady;

public class KafkaUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaUtils.class);

    public static void scaleBrokers(String namespace, String kafkaName, int newBrokerCount, boolean wait) {
        KafkaNodePool knp = ResourceUtils.getKubeResource(KafkaNodePool.class, namespace, KafkaNamingUtils.brokerPoolName(kafkaName));
        if (knp.getSpec().getReplicas() != newBrokerCount) {
            LOGGER.info("Scaling Kafka {}/{} brokers to {}", namespace, kafkaName, newBrokerCount);

            KubeResourceManager.get().createOrUpdateResourceWithWait(new KafkaNodePoolBuilder(knp)
                .editSpec()
                .withReplicas(newBrokerCount)
                .endSpec()
                .build());

            if (wait) {
                WaitUtils.waitForPodsReady(namespace, Labels.getKnpBrokerLabelSelector(kafkaName), newBrokerCount, true);
                WaitUtils.waitForPodsReady(namespace, Labels.getKnpControllerLabelSelector(kafkaName), newBrokerCount, true);
                waitForKafkaReady(namespace, kafkaName);
            }
        }
    }
}
