package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

public class KafkaConsumerUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KafkaConsumerUtils.class);
    private KafkaConsumerUtils() {}


    public static String getConsumerGroupOffset(String namespaceName, String kafkaName, String podName, String consumerGroupName, String topicName, String clientProperties) {
        String bootstrapServer = KafkaUtils.getPlainScramShaBootstrapAddress(kafkaName);

        LOGGER.debug("Retrieve conumer group {} offset", consumerGroupName);

        // First pass config
        String clientConfigPath = "/tmp/client.properties";
        String insertPropertiesCommand = String.format("echo '%s' > %s", clientProperties, clientConfigPath);
        LOGGER.debug("Insert client config");
        String output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", insertPropertiesCommand).out().trim();
        LOGGER.debug("Inserting resulted in => [{}]", output);

        String getOffsetCommand = String.format("./bin/kafka-consumer-groups.sh --bootstrap-server=%s --command-config=%s --group=%s --describe 2>/dev/null \\\n" +
            "  | awk -v topic=%s '$2 == topic { print $4 }' \\\n" +
            "  | grep -E '^[0-9]+$'", bootstrapServer, clientConfigPath, consumerGroupName, topicName);
        LOGGER.debug("Run consumer groups command to get the offset");
        output = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(podName, Constants.BASH_CMD, "-c", getOffsetCommand).out().trim();
        LOGGER.debug("Get offset command resulted in => [{}]", output);

        return output;
    }
}
