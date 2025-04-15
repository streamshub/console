package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.setup.console.ConsoleInstanceSetup;
import com.github.streamshub.systemtests.setup.strimzi.KafkaSetupConfig;
import com.github.streamshub.systemtests.utils.KafkaUtils;
import com.github.streamshub.systemtests.utils.PwUtils;
import com.github.streamshub.systemtests.utils.ResourceUtils;
import com.github.streamshub.systemtests.utils.Utils;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.HashMap;


public class TestCaseConfig {
    private final String testName;
    private final String namespaceName;
    private HashMap<String, KafkaSetupConfig> kafkaClusters = new HashMap<>();
    private ConsoleInstanceSetup consoleInstanceSetup;
    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;

    public TestCaseConfig(ExtensionContext extensionContext, String namespacePrefix) {
        this.testName = extensionContext.getTestMethod().isPresent() ? extensionContext.getTestMethod().get().getName() : extensionContext.getTestClass().get().getSimpleName();
        this.namespaceName = namespacePrefix + "-" + Utils.hashStub(testName);
        this.playwright = Playwright.create();
        this.browser = PwUtils.createBrowser(playwright);
        this.context = browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true));
        this.page = context.newPage();
    }

    // ----------
    // Getters
    // ----------
    public String getTestName() {
        return testName;
    }

    public String getNamespaceName() {
        return namespaceName;
    }

    public HashMap<String, KafkaSetupConfig> getKafkaClusters() {
        return kafkaClusters;
    }

    public KafkaSetupConfig getKafkaCluster(String clusterName) {
        return kafkaClusters.get(clusterName);
    }

    public KafkaSetupConfig getDefaultKafkaCluster() {
        return kafkaClusters.get(KafkaUtils.kafkaClusterName(namespaceName));
    }

    public Page getPage() {
        return page;
    }

    // ----------
    // Setup testcase
    // ----------
    public void defaultTestCaseSetup() {
        // Namespace
        createNamespaceIfNeeded();
        // Kafka
        addKafka(new KafkaSetupConfig(namespaceName));
        setupKafkaClustersIfNeeded();
        // Console
        setConsoleInstance(new ConsoleInstanceSetup(namespaceName, getDefaultKafkaCluster().getClusterName()));
        consoleInstanceSetup.deploy();
    }

    public void createNamespaceIfNeeded() {
        if (ResourceUtils.getKubeResource(Namespace.class, namespaceName) == null) {
            KubeResourceManager.get().createOrUpdateResourceWithWait(new NamespaceBuilder()
                .withNewMetadata()
                    .withName(namespaceName)
                .endMetadata()
                .build());
        }
    }

    public void addKafka(KafkaSetupConfig kafkaSetupConfig) {
        kafkaClusters.put(kafkaSetupConfig.getClusterName(), kafkaSetupConfig);
    }

    public void setupKafkaClustersIfNeeded() {
        for (KafkaSetupConfig kc : kafkaClusters.values()) {
            kc.setupIfNeeded();
        }
    }

    public void setConsoleInstance(ConsoleInstanceSetup consoleInstanceSetup) {
        this.consoleInstanceSetup = consoleInstanceSetup;
    }

    public void close() {
        this.playwright.close();
    }
}
