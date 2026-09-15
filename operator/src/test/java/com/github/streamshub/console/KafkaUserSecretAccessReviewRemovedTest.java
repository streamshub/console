package com.github.streamshub.console;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.status.Condition;
import com.github.streamshub.console.dependents.ConsoleServiceAccountAccessReview;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
@TestProfile(KafkaUserSecretAccessReviewRemovedTest.Profile.class)
class KafkaUserSecretAccessReviewRemovedTest extends KafkaUserSecretAccessReviewTestBase {

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(ConsoleServiceAccountAccessReview.DENIED_ACTION, "remove");
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

            var warning = console.getStatus().getConditions().stream()
                    .filter(c -> Condition.Types.SECURITY_WARNING.equals(c.getType()))
                    .findFirst()
                    .orElse(null);

            assertNotNull(warning, () -> "Expected a SECURITY_WARNING condition but got conditions: " + console.getStatus().getConditions());
            assertThat(warning.getMessage(), containsString("'ns1/ku1'"));
            assertThat(warning.getMessage(), containsString(CONSOLE_NS + ":" + CONSOLE_NAME + "-console-serviceaccount"));
        });

        assertConsoleConfig(consoleConfig -> {
            assertEquals(0, consoleConfig.getKafka().getClusters().size(), () -> "Cluster list should be empty when SAR is denied");
        });
    }
}
