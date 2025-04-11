package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.enums.BrowserTypes;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.util.Locale;

public class PwUtils {
    // -----------------
    // Playwright utils
    // -----------------
    public static Browser createBrowser(Playwright playwright) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(Environment.RUN_HEADLESS);
        BrowserTypes browserType = BrowserTypes.valueOf(Environment.BROWSER_TYPE.toUpperCase(Locale.ENGLISH));

        return switch (browserType) {
            case CHROMIUM -> playwright.chromium().launch(options);
            case MSEDGE -> playwright.chromium().launch(options.setChannel("msedge"));
            case FIREFOX -> playwright.firefox().launch(options);
            case WEBKIT -> playwright.webkit().launch(options);
            default -> throw new SetupException("Cannot create Playwright browser type: " + browserType);
        };

    }

    public static Page createPage(BrowserContext context) {
        return context.newPage();
    }

    public static void closePageIfPossible(Page page) {
        if (!page.isClosed()) {
            page.close();
        }
    }

    public static void closeBrowserIfPossible(Browser browser) {
        if (browser.isConnected()) {
            browser.close();
        }
    }

    public static void closePlaywrightIfPossible(Playwright playwright) {
        if (playwright != null) {
            playwright.close();
        }
    }

    public static void closePlaywright(TestCaseConfig tcc) {
        closePageIfPossible(tcc.getPage());
        closeContextIfPossible(tcc.getContext());
        closeBrowserIfPossible(tcc.getBrowser());
        closePlaywrightIfPossible(tcc.getPlaywright());
    }

    public static void closeContextIfPossible(BrowserContext context) {
        if (context != null && context.pages().isEmpty()) {
            context.close();
        }
    }
}
