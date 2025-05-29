package com.github.streamshub.systemtests.utils.playwright;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.TestCaseConfig;
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

    /**
     * Creates a Playwright {@link Browser} instance based on the configured browser type
     * and headless mode.
     *
     * @param playwright the Playwright instance used to create browsers
     * @return a launched {@link Browser} instance
     * @throws SetupException if the configured browser type is not supported
     */
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

    /**
     * Performs login to the Console UI using anonymous login for the specified test case configuration.
     *
     * <p>Navigates to the Kafka login page, waits for UI readiness, clicks the anonymous login button,
     * and waits until redirected to the overview page.
     *
     * @param tcc the test case configuration containing page and Kafka cluster information
     */
    public static void login(TestCaseConfig tcc) {
        final String loginUrl = PwPageUrls.getKafkaLoginPage(tcc, tcc.kafkaName());
        LOGGER.info("Logging in to the Console with URL: {}", loginUrl);
        waitForConsoleUiToBecomeReady(tcc);
        // Anonymous login
        tcc.page().navigate(loginUrl, getDefaultNavigateOpts());
        tcc.page().waitForURL(Pattern.compile(loginUrl + ".*"), getDefaultWaitForUrlOpts());
        waitForLocatorVisible(tcc, CssSelectors.LOGIN_ANONYMOUSLY_BUTTON);
        tcc.page().click(CssSelectors.LOGIN_ANONYMOUSLY_BUTTON);
        // Go to overview page
        tcc.page().waitForURL(PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()), getDefaultWaitForUrlOpts());
        LOGGER.info("Successfully logged into Console");
    }

    /**
     * Cleans and trims a given text string by removing newline characters,
     * replacing multiple horizontal spaces or tabs with a single space, and trimming.
     *
     * @param text the input string to be trimmed and cleaned
     * @return a cleaned, single-line trimmed string
     */
    public static String getTrimmedText(String text) {
        return text.replace("\n", "")
            .replaceAll("[\\h\\s{2,}\\t]", " ")
            .trim();
    }

    // -----------------
    // Wait for locator
    // -----------------
    public static void waitForLocatorVisible(TestCaseConfig tcc, String selector) {
        waitForLocatorVisible(CssSelectors.getLocator(tcc, selector), TimeConstants.ELEMENT_VISIBILITY_TIMEOUT);
    }

    public static void waitForLocatorVisible(Locator locator, long timeout) {
        locator.waitFor(new Locator.WaitForOptions().setTimeout(timeout).setState(WaitForSelectorState.VISIBLE));
    }

    public static void waitForContainsText(TestCaseConfig tcc, Locator locator, String text) {
        waitForContainsText(tcc.page(), locator, text, TimeConstants.COMPONENT_LOAD_TIMEOUT, true);
    }

    public static void waitForContainsText(TestCaseConfig tcc, String selector, String text) {
        waitForContainsText(tcc.page(), CssSelectors.getLocator(tcc, selector), text, TimeConstants.COMPONENT_LOAD_TIMEOUT, true);
    }

    /**
     * Waits until the given {@link Locator} contains the specified text within the provided timeout.
     * Optionally reloads the page if the text is not found during the wait.
     *
     * <p>If the locator resolves to multiple elements, their inner texts are combined and trimmed before checking.
     *
     * @param page the Playwright {@link Page} instance to reload if needed
     * @param locator the {@link Locator} to check for the expected text
     * @param text the expected substring text to wait for in the locator
     * @param componentLoadTimeout maximum time in milliseconds to wait for the text to appear
     * @param reload if true, reloads the page on each poll when the text is not found
     */
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
                    page.reload(getDefaultReloadOpts());
                }
                return false;
            },
            () -> LOGGER.error("Locator does not contain text [{}], instead it contains [{}]", text, getTrimmedText(locator.textContent()))
        );
    }

    public static void waitForLocatorCount(TestCaseConfig tcc, int count, String selector, boolean reload) {
        waitForLocatorCount(tcc, count, CssSelectors.getLocator(tcc, selector), reload);
    }

    /**
     * Waits until the specified {@link Locator} has the expected number of elements.
     * Optionally reloads the page if the count does not match during the wait.
     *
     * @param tcc the {@link TestCaseConfig} containing the page instance to reload if needed
     * @param count the expected number of elements the locator should have
     * @param locator the {@link Locator} whose elements count is checked
     * @param reload if true, reloads the page on each poll when the count is incorrect
     */
    public static void waitForLocatorCount(TestCaseConfig tcc, int count, Locator locator, boolean reload) {
        Wait.until("locator to have item count: " + count, TimeConstants.GLOBAL_POLL_INTERVAL_SHORT, TimeConstants.COMPONENT_LOAD_TIMEOUT,
            () -> {
                if (locator.all().size() == count) {
                    LOGGER.debug("Locator has correct item count [{}]", count);
                    return true;
                }
                LOGGER.debug("Locator has incorrect item count [{}]", locator.all().size());
                if (reload) {
                    tcc.page().reload(getDefaultReloadOpts());
                }
                return false;
            },
            () -> LOGGER.error("Page does not have enough locators count [{}] out of required [{}]", locator.all().size(), count)
        );
    }

    /**
     * Waits for the Console UI to become ready by repeatedly checking the login page availability.
     * It navigates to the Kafka login page and verifies if the application is up and
     * the anonymous login button is visible.
     *
     * @param tcc the {@link TestCaseConfig} containing the Playwright page instance used to perform the checks
     */
    public static void waitForConsoleUiToBecomeReady(TestCaseConfig tcc) {
        LOGGER.info("============= Waiting for Console Website to be online =============");
        Wait.until("Console Web to become available", TestFrameConstants.GLOBAL_POLL_INTERVAL_SHORT, TestFrameConstants.GLOBAL_TIMEOUT_SHORT,
            () -> {
                try {
                    LOGGER.debug("Console website reach-out try");

                    // First test if application is fully running
                    tcc.page().navigate(PwPageUrls.getKafkaLoginPage(tcc, tcc.kafkaName()), getDefaultNavigateOpts());

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

    // ----------------
    // Default options
    // ----------------
    public static Page.NavigateOptions getDefaultNavigateOpts() {
        return new Page.NavigateOptions()
            .setTimeout(TestFrameConstants.GLOBAL_TIMEOUT_SHORT)
            .setWaitUntil(WaitUntilState.LOAD);
    }

    public static Page.ReloadOptions getDefaultReloadOpts() {
        return new Page.ReloadOptions()
            .setTimeout(TestFrameConstants.GLOBAL_TIMEOUT_SHORT)
            .setWaitUntil(WaitUntilState.LOAD);
    }

    public static Page.WaitForURLOptions getDefaultWaitForUrlOpts() {
        return new Page.WaitForURLOptions()
            .setTimeout(TestFrameConstants.GLOBAL_TIMEOUT_SHORT)
            .setWaitUntil(WaitUntilState.LOAD);
    }
}
