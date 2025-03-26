package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.enums.BrowserTypes;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.util.Locale;

public class PwUtils {
    public static Browser createAndReturnBrowser(Playwright playwright) {
        BrowserType.LaunchOptions options = new BrowserType.LaunchOptions().setHeadless(Environment.RUN_HEADLESS);
        switch (BrowserTypes.valueOf(Environment.BROWSER_TYPE.toUpperCase(Locale.ENGLISH))) {
            case CHROMIUM: return playwright.chromium().launch(options);
            case MSEDGE: return playwright.chromium().launch(options.setChannel("msedge"));
            case FIREFOX: return playwright.firefox().launch(options);
            case WEBKIT: return playwright.webkit().launch(options);
            default: return playwright.chromium().launch(options);
        }
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
}
