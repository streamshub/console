package com.github.streamshub.systemtests;

import com.github.streamshub.systemtests.utils.PwUtils;
import com.github.streamshub.systemtests.utils.Utils;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.Locale;


public class TestCaseConfig {
    private final String testName;
    private final String namespace;
    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;

    public TestCaseConfig(ExtensionContext extensionContext) {
        this.testName = extensionContext.getTestMethod().isPresent() ? extensionContext.getTestMethod().get().getName() : extensionContext.getTestClass().get().getSimpleName();
        this.namespace = extensionContext.getTestClass().isPresent() ?  extensionContext.getTestClass().get().getSimpleName().toLowerCase(Locale.ENGLISH) + "-" + Utils.hashStub(testName) : "nullClass";
        this.playwright = Playwright.create();
        this.browser = PwUtils.createBrowser(playwright);
        this.context = browser.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true));
        this.page = context.newPage();
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

    public Playwright playwright() {
        return playwright;
    }
}
