package com.github.streamshub.systemtests.operator;

import java.util.ArrayList;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.systemtests.AbstractST;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.TestTags;
import com.github.streamshub.systemtests.locators.KafkaDashboardPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetup;
import com.github.streamshub.systemtests.utils.playwright.PwPageUrls;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.NamespaceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.skodjob.kubetest4j.wait.Wait;

import static com.github.streamshub.systemtests.utils.Utils.getTestCaseConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag(TestTags.REGRESSION)
class OperatorConfigurationST extends AbstractST {

    private static final Logger LOGGER = LogWrapper.getLogger(OperatorConfigurationST.class);
    private static final String DENIED_ACTION_ENV = "env:CONSOLE_SERVICE_ACCOUNT_ACCESS_REVIEW_DENIED_ACTION:";

    protected TestCaseConfig tcc;
    private Console console;

    // Actions for SAR denial: IGNORE, PRESERVE, REMOVE

    @Test
    @Tag(DENIED_ACTION_ENV + "IGNORE")
    void testIgnoreKafkaUserAccessDenied() {
        PwUtils.navigate(tcc, PwPageUrls.getConsoleUrl(tcc));
        assertKafkaListContains(tcc.kafkaName());

        ConsoleInstanceSetup.removeKafkaUserRBAC(console, true);
        var actualConsole = ResourceUtils.getKubeResourceClient(Console.class)
                .inNamespace(console.getMetadata().getNamespace())
                .withName(console.getMetadata().getName())
                .get();
        assertEquals("True", actualConsole.getStatus().getCondition("Ready").getStatus());

        tcc.page().reload();
        assertKafkaListContains(tcc.kafkaName());
    }

    @Test
    @Tag(DENIED_ACTION_ENV + "PRESERVE")
    void testPreserveKafkaUserAccessDenied() {
        PwUtils.navigate(tcc, PwPageUrls.getConsoleUrl(tcc));
        assertKafkaListContains(tcc.kafkaName());

        ConsoleInstanceSetup.removeKafkaUserRBAC(console, false);
        Wait.until("Console has SecurityError", 1000, 10000, () -> {
            var actualConsole = ResourceUtils.getKubeResourceClient(Console.class)
                    .inNamespace(console.getMetadata().getNamespace())
                    .withName(console.getMetadata().getName())
                    .get();
            return "True".equals(actualConsole.getStatus().getCondition("SecurityError").getStatus());
        });

        tcc.page().reload();
        assertKafkaListContains(tcc.kafkaName());
    }

    @Test
    @Tag(DENIED_ACTION_ENV + "REMOVE")
    void testRemoveKafkaUserAccessDenied() {
        PwUtils.navigate(tcc, PwPageUrls.getConsoleUrl(tcc));
        assertKafkaListContains(tcc.kafkaName());

        ConsoleInstanceSetup.removeKafkaUserRBAC(console, true);
        Wait.until("Console has SecurityWarning", 1000, 10000, () -> {
            var actualConsole = ResourceUtils.getKubeResourceClient(Console.class)
                    .inNamespace(console.getMetadata().getNamespace())
                    .withName(console.getMetadata().getName())
                    .get();
            if ("True".equals(actualConsole.getStatus().getCondition("SecurityWarning").getStatus())) {
                return "True".equals(actualConsole.getStatus().getCondition("Ready").getStatus());
            }
            return false;
        });

        tcc.page().reload();
        assertKafkaListContains("No data available");
    }

    void assertKafkaListContains(String value) {
        PwUtils.waitForLocatorCount(tcc, 1, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, false);
        PwUtils.waitForContainsText(tcc, KafkaDashboardPageSelectors.KDPS_KAFKA_CLUSTER_LIST_ITEMS, value, false);
    }

    @BeforeAll
    void testClassSetup() {
        tcc = getTestCaseConfig();
        NamespaceUtils.prepareNamespace(tcc.namespace());
        KafkaSetup.setupDefaultKafkaIfNeeded(tcc.namespace(), tcc.kafkaName());
    }

    @BeforeEach
    void testCaseSetup(TestInfo testInfo) {
        var setEnvs = testInfo.getTags()
                .stream()
                .filter(tag -> tag.startsWith("env:"))
                .map(tag -> tag.substring("env:".length()).split(":"))
                .collect(Collectors.toMap(e -> e[0], e -> e[1]));

        // editEnv will wait until the operator deployment is ready with the variable changes
        LOGGER.info("Setting envs on Console operator: {}", setEnvs);
        consoleOperatorSetup.editEnv(env -> {
            var newEnv = new ArrayList<>(env);
            newEnv.removeIf(e -> setEnvs.containsKey(e.getName()));
            setEnvs.forEach((k, v) -> newEnv.add(new EnvVar(k, v, null)));
            return newEnv;
        });

        this.console = ConsoleInstanceSetup.getDefaultConsoleInstance(
                tcc.namespace(),
                tcc.consoleInstanceName(),
                tcc.kafkaName(),
                tcc.kafkaUserName()).build();

        ConsoleInstanceSetup.setupIfNeeded(console);
    }

    @AfterAll
    void testClassTeardown() {
        tcc.playwright().close();
    }
}
