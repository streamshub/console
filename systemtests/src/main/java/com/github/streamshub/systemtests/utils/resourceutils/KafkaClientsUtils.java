package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.utils.Utils;
import io.fabric8.kubernetes.api.model.Secret;
import org.apache.kafka.common.security.auth.SecurityProtocol;

public class KafkaClientsUtils {
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
        final String saslJaasConfigDecrypted = Utils.decodeFromBase64(ResourceUtils.getKubeResource(Secret.class, namespace, userName).getData().get("sasl.jaas.config"));
        return  "sasl.mechanism=SCRAM-SHA-512\n" +
            "security.protocol=" + securityProtocol + "\n" +
            "sasl.jaas.config=" + saslJaasConfigDecrypted + "\n";
    }
}
