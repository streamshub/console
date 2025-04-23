package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.enums.BrowserTypes;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;

import java.util.Locale;

public class PwUtils {

    private PwUtils() {}

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
}
