package com.github.streamshub.console.support;

import java.util.Map;
import java.util.Set;

public class KafkaConfigs {

    public static final String MECHANISM_OAUTHBEARER = "OAUTHBEARER";
    public static final String MECHANISM_SCRAM_SHA256 = "SCRAM-SHA-256";
    public static final String MECHANISM_SCRAM_SHA512 = "SCRAM-SHA-512";
    public static final String MECHANISM_PLAIN = "PLAIN";

    /**
     * `type` value for listener configuration in Strimzi's Kafka CR. This type is
     * deprecated in Strimzi as of version 0.49.0, so we keep our own copy for removal
     * once it's no longer supported by the API.
     */
    public static final String TYPE_OAUTH = "oauth";

    private KafkaConfigs() {
        // No instances
    }

    /**
     * Retrieve any known SASL mechanism enabled in the provided listener
     * configuration map. Currently only recognizes the OAUTHBEARER mechanism.
     */
    public static String saslMechanism(Map<String, Object> listenerConfig) {
        var enabledMechanisms = (String) listenerConfig.get("sasl.enabled.mechanisms");

        if (enabledMechanisms != null) {
            Set<String> mechanisms = Set.of(enabledMechanisms.split(","));

            if (mechanisms.contains(MECHANISM_OAUTHBEARER)) {
                return MECHANISM_OAUTHBEARER;
            }
        }

        return null;
    }
}
