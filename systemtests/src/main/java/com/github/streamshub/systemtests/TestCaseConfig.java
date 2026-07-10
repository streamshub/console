package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kafka.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.kroxy.KroxyNamingUtils;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.ColorScheme;
import com.microsoft.playwright.options.ViewportSize;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;
import java.util.Locale;

public class TestCaseConfig {

    private static final Logger LOGGER = LogWrapper.getLogger(TestCaseConfig.class);

    private final String testName;
    private final String namespace;
    private Playwright playwright;
    private BrowserContext context;
    private Browser browser;
    private Page page;
    private boolean tracingActive;
    private final int defaultMessageCount;

    // Default Kafka
    private final String kafkaName;
    private final String connectName;
    private final String kafkaUserName;
    private final String consoleInstanceName;

    // Default Kroxy
    private final String kafkaProxyName;
    private final String kafkaProxyIngressName;
    private final String virtualKafkaClusterName;
    private final String kafkaServiceName;
    private final String kafkaProtocolFilterName;

    // Default Apicurio
    private final String apicurioRegistry3Name;

    public TestCaseConfig(ExtensionContext extensionContext) {
        this.testName = extensionContext.getTestMethod()
            .map(Method::getName)
            .or(() -> extensionContext.getTestClass().map(Class::getSimpleName))
            .orElseThrow();
        this.namespace = extensionContext.getTestClass()
            .map(Class::getSimpleName)
            .map(name -> name.toLowerCase(Locale.ENGLISH) + "-" + Utils.hashStub(testName))
            .orElse("nullClass");

        this.initPlaywright();

        this.kafkaName = KafkaNamingUtils.kafkaClusterName(namespace);
        this.connectName = KafkaNamingUtils.kafkaConnectName(namespace);
        this.kafkaUserName =  KafkaNamingUtils.kafkaUserName(kafkaName);

        this.consoleInstanceName = Constants.CONSOLE_INSTANCE + "-" + Utils.hashStub(namespace);

        this.kafkaProxyName = KroxyNamingUtils.kafkaProxyName(namespace);
        this.kafkaProxyIngressName = KroxyNamingUtils.kafkaProxyIngressName(namespace);
        this.virtualKafkaClusterName = KroxyNamingUtils.virtualKafkaClusterName(namespace);
        this.kafkaServiceName = KroxyNamingUtils.kafkaServiceName(namespace);
        this.kafkaProtocolFilterName = KroxyNamingUtils.kafkaProtocolFilterName(namespace);

        this.defaultMessageCount = Constants.MESSAGE_COUNT;
        this.apicurioRegistry3Name = Constants.APICURIO_PREFIX + "-" + Utils.hashStub(namespace);

        LOGGER.info("Initialized TestCaseConfig for test [{}] in namespace [{}], Kafka [{}], Console instance [{}]",
            testName, namespace, kafkaName, consoleInstanceName);
    }

    private void initPlaywright() {
        LOGGER.debug("Initializing Playwright browser and context for namespace [{}]", namespace);
        this.playwright = Playwright.create();
        // to keep browser context open it must exist within the TestCaseConfig context - can't be a local var
        this.browser = PwUtils.createBrowser(playwright);

        this.context = browser.newContext(new Browser.NewContextOptions()
            .setColorScheme(ColorScheme.DARK)
            .setViewportSize(new ViewportSize(Environment.BROWSER_VIEWPORT_WIDTH, Environment.BROWSER_VIEWPORT_HEIGHT))
            .setIgnoreHTTPSErrors(true));

        this.page = context.newPage();

        startTracing();
    }

    /**
     * Starts tracing on the current {@link BrowserContext} if it is not already active. Safe to call
     * repeatedly - a no-op when tracing is already running.
     */
    public void startTracing() {
        if (!tracingActive) {
            LOGGER.info("Starting tracing for browser context [namespace={}]", namespace);
            this.context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true));
            tracingActive = true;
        }
        LOGGER.debug("Browser context tracing is active");
    }

    /**
     * Discards whatever has been recorded so far (not written to disk) and begins a fresh tracing
     * segment. Used to align tracing boundaries with the start of each {@code @Test} method,
     * regardless of when the underlying {@link BrowserContext} was created.
     */
    public void restartTracing() {
        LOGGER.info("Restarting tracing for browser context [namespace={}] to align with new test boundary", namespace);
        if (tracingActive) {
            this.context.tracing().stop();
            tracingActive = false;
        }
        startTracing();
    }

    /**
     * Stops tracing and saves it to disk if tracing is currently active. No-op if tracing was
     * already stopped (e.g. by a previous call for the same test).
     */
    public void stopAndSaveTracing() {
        if (tracingActive) {
            LOGGER.info("Stopping tracing and saving trace file for browser context [namespace={}]", namespace);
            PwUtils.saveTracing(this.context);
            tracingActive = false;
        }
        LOGGER.debug("Browser context tracing is inactive");
    }

    public void resetBrowserContext() {
        LOGGER.info("Resetting Playwright browser context [namespace={}]", namespace);
        try {
            // closes the context and its page(s), browser stays alive
            this.context.close();
            this.browser.close();
            this.playwright.close();
        } catch (Exception ignored) {
            LOGGER.error("Failed to cleanly close Playwright browser context/browser/playwright during reset", ignored);
        }
        initPlaywright();
    }

    // ----------
    // Getters
    // ----------
    public String testName() {
        return testName;
    }

    public String namespace() {
        return namespace;
    }

    public Page page() {
        return page;
    }

    public BrowserContext context() {
        return context;
    }

    public Playwright playwright() {
        return playwright;
    }

    public String kafkaName() {
        return kafkaName;
    }

    public String connectName() {
        return connectName;
    }

    public String kafkaUserName() {
        return kafkaUserName;
    }

    public String consoleInstanceName() {
        return consoleInstanceName;
    }

    public String kafkaProxyName() {
        return kafkaProxyName;
    }

    public String kafkaProxyIngressName() {
        return kafkaProxyIngressName;
    }

    public String virtualKafkaClusterName() {
        return virtualKafkaClusterName;
    }

    public String kafkaServiceName() {
        return kafkaServiceName;
    }

    public String kafkaProtocolFilterName() {
        return kafkaProtocolFilterName;
    }

    public int defaultMessageCount() {
        return defaultMessageCount;
    }

    public String apicurioRegistry3Name() {
        return apicurioRegistry3Name;
    }
}
