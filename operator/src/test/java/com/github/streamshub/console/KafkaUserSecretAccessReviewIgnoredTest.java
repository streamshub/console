package com.github.streamshub.console;

import java.util.Map;

import org.apache.kafka.common.config.SaslConfigs;
import org.junit.jupiter.api.Test;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.status.Condition;
import com.github.streamshub.console.dependents.ConsoleServiceAccountAccessReview;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
@TestProfile(KafkaUserSecretAccessReviewIgnoredTest.Profile.class)
class KafkaUserSecretAccessReviewIgnoredTest extends KafkaUserSecretAccessReviewTestBase {

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(ConsoleServiceAccountAccessReview.DENIED_ACTION, "ignore");
        }
    }

    @Test
    void testConsoleReconciliationWithKafkaUserSecretDeniedBySAR() {
        setupConsoleAndKafkaUser();
        // Intentionally do NOT grant the console SA access to the Secret

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var console = client.resources(Console.class)
                    .inNamespace(CONSOLE_NS)
                    .withName(CONSOLE_NAME)
                    .get();

            var errorCondition = console.getStatus().getConditions().stream()
                    .filter(c -> Condition.Types.SECURITY_ERROR.equals(c.getType()))
                    .findFirst()
                    .orElse(null);

            assertNull(errorCondition, 
                () -> "Expected SECURITY_ERROR condition to be absent, but got conditions: " + console.getStatus().getConditions());

        });

        assertConsoleConfig(consoleConfig -> {
            assertNotNull(
                    consoleConfig.getKafka().getClusters().get(0).getProperties().get(SaslConfigs.SASL_JAAS_CONFIG),
                    "JAAS config must be present in ConsoleSecret when SAR denial is ignored"
            );
        });
    }
}
