package com.github.streamshub.systemtests.logs;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.constants.ResourceKinds;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.skodjob.testframe.LogCollector;
import io.skodjob.testframe.LogCollectorBuilder;
import io.skodjob.testframe.clients.KubeClient;
import io.skodjob.testframe.clients.cmdClient.Kubectl;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TestLogCollector {
    private static final Logger LOGGER = LogWrapper.getLogger(TestLogCollector.class);
    private static TestLogCollector instance;
    private final String currentDate;
    private final LogCollector logCollector;

    private TestLogCollector() {
        // Get current date to create a unique folder
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
        dateTimeFormatter = dateTimeFormatter.withZone(ZoneId.of("GMT"));
        currentDate = dateTimeFormatter.format(LocalDateTime.now());
        this.logCollector = defaultLogCollector();
    }

    public static TestLogCollector getInstance() {
        if (instance == null) {
            instance = new TestLogCollector();
        }
        return instance;
    }

    private LogCollector defaultLogCollector() {
        List<String> resources = new ArrayList<>(List.of(
            ResourceKinds.SECRET,
            ResourceKinds.DEPLOYMENT,
            ResourceKinds.KAFKA,
            ResourceKinds.KAFKA_NODE_POOL,
            ResourceKinds.KAFKA_TOPIC,
            ResourceKinds.KAFKA_USER,
            ResourceKinds.CONSOLE
        ));

        if (Environment.INSTALL_USING_OLM) {
            resources.addAll(List.of(
                ResourceKinds.OPERATOR_GROUP,
                ResourceKinds.SUBSCRIPTION,
                ResourceKinds.INSTALL_PLAN,
                ResourceKinds.CLUSTER_SERVICE_VERSION
            ));
        }

        return new LogCollectorBuilder()
            .withKubeClient(new KubeClient())
            .withKubeCmdClient(new Kubectl())
            .withRootFolderPath(Environment.TEST_LOG_DIR)
            .withNamespacedResources(resources.toArray(new String[0]))
            .build();
    }

    private Path checkPathAndReturnFullRootPathWithIndexFolder(Path rootPathToLogsForTestCase) {
        File logsForTestCase = rootPathToLogsForTestCase.toFile();
        int index = 1;

        if (logsForTestCase.exists()) {
            String[] filesInLogsDir = logsForTestCase.list();

            if (filesInLogsDir != null && filesInLogsDir.length > 0) {
                index = Integer.parseInt(
                    Arrays
                        .stream(filesInLogsDir)
                        .filter(file -> {
                            try {
                                Integer.parseInt(file);
                                return true;
                            } catch (NumberFormatException e) {
                                return false;
                            }
                        })
                        .sorted()
                        .toList()
                        .get(filesInLogsDir.length - 1)
                ) + 1;
            }
        }

        return rootPathToLogsForTestCase.resolve(String.valueOf(index));
    }

    private Path buildFullPathToLogs(String testClass, String testCase) {
        Path rootPathToLogsForTestCase = Path.of(Environment.TEST_LOG_DIR, currentDate, testClass);

        if (testCase != null) {
            rootPathToLogsForTestCase = rootPathToLogsForTestCase.resolve(testCase);
        }

        return checkPathAndReturnFullRootPathWithIndexFolder(rootPathToLogsForTestCase);
    }

    public void collectLogs() {
        collectLogs(
            KubeResourceManager.get().getTestContext().getRequiredTestClass().getName(),
            KubeResourceManager.get().getTestContext().getRequiredTestMethod().getName()
        );
    }

    public void collectLogs(String testClass) {
        collectLogs(testClass, null);
    }

    public void collectLogs(String testClass, String testCase) {
        LOGGER.debug("Collecting logs from {}#{}", testClass, testCase);
        Path rootPathToLogsForTestCase = buildFullPathToLogs(testClass, testCase);

        final LogCollector testCaseCollector = new LogCollectorBuilder(logCollector)
            .withRootFolderPath(rootPathToLogsForTestCase.toString())
            .build();

        testCaseCollector.collectFromNamespacesWithLabels(new LabelSelectorBuilder()
            .withMatchLabels(Collections.singletonMap(Labels.COLLECT_ST_LOGS, "true"))
            .build());
    }
}