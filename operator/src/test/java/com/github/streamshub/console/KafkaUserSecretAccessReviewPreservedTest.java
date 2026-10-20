package com.github.streamshub.console;

import java.util.Map;

import org.apache.kafka.common.config.SaslConfigs;
import org.junit.jupiter.api.Test;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.status.Condition;
import com.github.streamshub.console.dependents.ConsoleSecret;
import com.github.streamshub.console.dependents.ConsoleServiceAccountAccessReview;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@QuarkusTest
@TestProfile(KafkaUserSecretAccessReviewPreservedTest.Profile.class)
class KafkaUserSecretAccessReviewPreservedTest extends KafkaUserSecretAccessReviewTestBase {

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(ConsoleServiceAccountAccessReview.DENIED_ACTION, "preserve");
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

            assertNotNull(errorCondition, () -> "Expected a SECURITY_ERROR condition but got conditions: " + console.getStatus().getConditions());
            assertThat(errorCondition.getMessage(), containsString("'ns1/ku1'"));
            assertThat(errorCondition.getMessage(), containsString(CONSOLE_NS + ":" + CONSOLE_NAME + "-console-serviceaccount"));
        });

        var consoleSecret = client.secrets().inNamespace(CONSOLE_NS).withName(CONSOLE_NAME + "-" + ConsoleSecret.NAME).get();
        assertNull(consoleSecret, "ConsoleSecret must not be present when SAR is denied");
    }

    @Test
    void testConsoleReconciliationPreservesExistingCredentialsAfterAccessIsRevoked() {
        setupConsoleAndKafkaUser();
        grantConsoleServiceAccountSecretAccess(
                null,
                "ServiceAccount",
                CONSOLE_NAME + "-console-serviceaccount",
                CONSOLE_NS,
                KAFKA_NS,
                "ku1"
        );

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            assertConsoleConfig(consoleConfig -> assertNotNull(
                    consoleConfig.getKafka().getClusters().get(0).getProperties().get(SaslConfigs.SASL_JAAS_CONFIG),
                    "JAAS config must be present before access is revoked"
            ));
        });

        var consoleSecret = client.secrets().inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + "-" + ConsoleSecret.NAME)
                .get();
        assertNotNull(consoleSecret);
        String originalConfig = consoleSecret.getData().get("console-config.yaml");

        client.rbac().roleBindings().inNamespace(KAFKA_NS)
                .withName("test-kafkauser-secret-ku1")
                .delete();
        client.resources(Console.class)
                .inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME)
                .edit(console -> new ConsoleBuilder(console)
                        .editSpec()
                            .withHostname("updated.example.com")
                        .endSpec()
                        .build());

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var console = client.resources(Console.class)
                    .inNamespace(CONSOLE_NS)
                    .withName(CONSOLE_NAME)
                    .get();

            var errorCondition = console.getStatus().getConditions().stream()
                    .filter(c -> Condition.Types.SECURITY_ERROR.equals(c.getType()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(errorCondition, () -> "Expected a SECURITY_ERROR condition but got conditions: " + console.getStatus().getConditions());
            assertThat(errorCondition.getMessage(), containsString("'ns1/ku1'"));
        });

        var preservedSecret = client.secrets().inNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME + "-" + ConsoleSecret.NAME)
                .get();
        assertNotNull(preservedSecret);
        assertEquals(originalConfig, preservedSecret.getData().get("console-config.yaml"),
                "PRESERVE must not replace existing Console configuration");
    }
}
