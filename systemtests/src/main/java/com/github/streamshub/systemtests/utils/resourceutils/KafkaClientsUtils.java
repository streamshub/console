package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.utils.Utils;
import io.fabric8.kubernetes.api.model.Secret;
import org.apache.kafka.common.security.auth.SecurityProtocol;

public class KafkaClientsUtils {
    private KafkaClientsUtils() {}

    public static String getScramShaConfig(String namespace, String userName, SecurityProtocol securityProtocol) {
        final String saslJaasConfigDecrypted = Utils.decodeFromBase64(ResourceUtils.getKubeResource(Secret.class, namespace, userName).getData().get("sasl.jaas.config"));
        return  "sasl.mechanism=SCRAM-SHA-512\n" +
            "security.protocol=" + securityProtocol + "\n" +
            "sasl.jaas.config=" + saslJaasConfigDecrypted + "\n";
    }
}
