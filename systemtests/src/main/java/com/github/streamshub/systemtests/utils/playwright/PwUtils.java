package com.github.streamshub.systemtests.utils.playwright;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.enums.BrowserTypes;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.playwright.locators.CssSelectors;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.wait.Wait;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.regex.Pattern;

public class PwUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(PwUtils.class);
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

    public static String getTrimmedText(String text) {
        return text.replaceAll("\\n", "")
            .replaceAll("[\\h\\s+\\t]", " ")
            .trim();
    }

    public static void waitForLocatorVisible(TestCaseConfig tcc, String selector) {
        waitForLocatorVisible(CssSelectors.getLocator(tcc, selector), Constants.ELEMENT_VISIBILITY_TIMEOUT);
    }

    public static void waitForLocatorVisible(Locator locator, double timeout) {
        locator.waitFor(new Locator.WaitForOptions().setTimeout(timeout).setState(WaitForSelectorState.VISIBLE));
    }

    public static void navigateTo(Page page, String fullPageUrl) {
        LOGGER.info("Navigate to url [{}]", fullPageUrl);
        page.navigate(fullPageUrl, getNavigateWait());
    }

    public static Page.NavigateOptions getNavigateWait() {
        return new Page.NavigateOptions()
            .setTimeout(TestFrameConstants.GLOBAL_TIMEOUT_SHORT)
            .setWaitUntil(WaitUntilState.LOAD);
    }

    public static Page.ReloadOptions getReloadWait() {
        return new Page.ReloadOptions()
            .setTimeout(TestFrameConstants.GLOBAL_TIMEOUT_SHORT)
            .setWaitUntil(WaitUntilState.LOAD);
    }

    public static Page.WaitForURLOptions getWaitForUrlOpts() {
        return new Page.WaitForURLOptions()
            .setTimeout(TestFrameConstants.GLOBAL_TIMEOUT_SHORT)
            .setWaitUntil(WaitUntilState.LOAD);
    }

    public static void login(TestCaseConfig tcc) {
        final String loginUrl = PwPageUrls.getKafkaLoginPage(tcc, tcc.kafkaName());
        LOGGER.info("Logging in to the Console with URL: {}", loginUrl);
        waitForConsoleUiToBecomeReady(tcc);
        // Anonymous login
        navigateTo(tcc.page(), loginUrl);
        tcc.page().waitForURL(Pattern.compile(loginUrl + ".*"), getWaitForUrlOpts());
        waitForLocatorVisible(tcc, CssSelectors.LOGIN_ANONYMOUSLY_BUTTON);
        tcc.page().click(CssSelectors.LOGIN_ANONYMOUSLY_BUTTON);
        // Go to overview page
        tcc.page().waitForURL(PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()), getWaitForUrlOpts());
        LOGGER.info("Successfully logged into Console");
    }

    public static void waitForConsoleUiToBecomeReady(TestCaseConfig tcc) {
        LOGGER.info("============= Waiting for Console Website to be online =============");
        Wait.until("Console Web to become available", TestFrameConstants.GLOBAL_POLL_INTERVAL_SHORT, TestFrameConstants.GLOBAL_TIMEOUT_SHORT,
            () -> {
                try {
                    LOGGER.debug("Console website reach-out try");

                    // First test if application is fully running
                    tcc.page().navigate(PwPageUrls.getKafkaLoginPage(tcc, tcc.kafkaName()), PwUtils.getNavigateWait());

                    if (tcc.page().locator("body").innerText().contains("Application is not available")) {
                        return false;
                    }

                    // Second test if login page is able to display a login button
                    if (CssSelectors.getLocator(tcc.page(), CssSelectors.LOGIN_ANONYMOUSLY_BUTTON).isVisible()) {
                        LOGGER.info("Console website is ready");
                        return true;
                    }

                    return false;
                } catch (Exception e) {
                    LOGGER.warn("Console UI has not been loaded yet");
                }
                return false;
            },
            () -> LOGGER.error("Console UI did not load in time")
        );
    }

    public static void waitForContainsText(TestCaseConfig tcc, String selector, String text) {
        waitForContainsText(tcc.page(), CssSelectors.getLocator(tcc, selector), text, TimeConstants.COMPONENT_LOAD_TIMEOUT, true);
    }

    public static void waitForContainsText(Page page, Locator locator, String text, long componentLoadTimeout, boolean reload) {
        Wait.until("locator to contain text: " + text, TimeConstants.GLOBAL_POLL_INTERVAL_SHORT, componentLoadTimeout,
            () -> {
                String innerText = "";

                if (locator.all().size() > 1) {
                    innerText = getTrimmedText(locator.allInnerTexts().toString());
                } else {
                    innerText = getTrimmedText(locator.textContent());
                }

                LOGGER.debug("Current locator text [{}], should contain [{}]", innerText, text);
                if (innerText.contains(text)) {
                    return true;
                }

                if (reload) {
                    page.reload(getReloadWait());
                }
                return false;
            },
            () -> LOGGER.error("Locator does not contain text [{}], instead it contains [{}]", text, getTrimmedText(locator.textContent()))
        );
    }
}
