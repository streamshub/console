package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.playwright.PwUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;
import java.util.Locale;

public class TestCaseConfig {
    private final String testName;
    private final String namespace;
    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;
    private int messageCount;

    // Default
    private final String kafkaName;
    private final String kafkaUserName;
    private final String consoleInstanceName;

    public TestCaseConfig(ExtensionContext extensionContext) {
        this.testName = extensionContext.getTestMethod()
            .map(Method::getName)
            .or(() -> extensionContext.getTestClass().map(Class::getSimpleName))
            .orElseThrow();
        this.namespace = extensionContext.getTestClass()
            .map(Class::getSimpleName)
            .map(name -> name.toLowerCase(Locale.ENGLISH) + "-" + Utils.hashStub(testName))
            .orElse("nullClass");

        this.playwright = Playwright.create();
        this.browser = PwUtils.createBrowser(playwright);
        this.context = browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true));
        // Allow tracing
        this.context.tracing().start(new Tracing.StartOptions().setScreenshots(true).setSnapshots(true).setSources(true));

        this.page = context.newPage();

        this.kafkaName = KafkaNamingUtils.kafkaClusterName(namespace);
        this.kafkaUserName =  KafkaNamingUtils.kafkaUserName(kafkaName);
        this.consoleInstanceName = Constants.CONSOLE_INSTANCE + "-" + Utils.hashStub(namespace);

        this.messageCount = Constants.MESSAGE_COUNT;
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

    public String kafkaUserName() {
        return kafkaUserName;
    }

    public String consoleInstanceName() {
        return consoleInstanceName;
    }

    public int messageCount() {
        return messageCount;
    }

    // ----------
    // Setters
    // ----------
    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }
}
