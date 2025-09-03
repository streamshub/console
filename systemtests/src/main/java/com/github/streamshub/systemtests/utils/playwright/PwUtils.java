package com.github.streamshub.systemtests.utils.playwright;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.enums.BrowserTypes;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.Utils;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.wait.Wait;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
     * Performs login to the Console UI for the specified test case configuration.
     *
     * <p>Navigates to the Kafka login page, waits for UI readiness, logs in,
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
        waitForLocatorAndClick(tcc, CssSelectors.LOGIN_ANONYMOUSLY_BUTTON);
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
        // Replaces newline, NBSP, horizontal whitespace, tab or whitespace with a whitespace
        return text.replace("\n", "")
            .replace("\u00A0", " ")
            .replaceAll("\\s+", " ")
            .trim();
    }

    // --------------------------
    // Wait for locator visible
    // --------------------------
    public static void waitForLocatorVisible(TestCaseConfig tcc, String selector) {
        waitForLocatorVisible(tcc, selector, TimeConstants.ELEMENT_VISIBILITY_TIMEOUT);
    }

    public static void waitForLocatorVisible(TestCaseConfig tcc, String selector, long timeout) {
        LOGGER.debug("Waiting for locator to be visible without reloading [{}]", selector);
        tcc.page()
            .locator(selector)
            .waitFor(new Locator.WaitForOptions().setTimeout(timeout)
                .setState(WaitForSelectorState.VISIBLE));
    }

    // --------------------------
    // Click actions
    // --------------------------
    public static void waitForLocatorAndClick(TestCaseConfig tcc, String selector) {
        waitForLocatorVisible(tcc, selector);
        clickWithRetry(tcc, selector);
    }

    // Due to https://github.com/microsoft/playwright/issues/14946
    // some buttons or elements that change their visibility might throw errors onclick action
    public static void clickWithRetry(TestCaseConfig tcc, String selector) {
        Utils.retryAction("click on locator", () -> click(tcc, selector), Constants.MAX_ACTION_RETRIES);
    }

    public static void click(TestCaseConfig tcc, String selector) {
        LOGGER.debug("Clicking on locator [{}]", selector);
        Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
        tcc.page().locator(selector).click(new Locator.ClickOptions().setForce(true).setTimeout(TimeConstants.COMPONENT_LOAD_TIMEOUT));
        Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
    }

    // --------------------------
    // Fill actions
    // --------------------------
    public static void waitForLocatorAndFill(TestCaseConfig tcc, String selector, String fillText) {
        waitForLocatorVisible(tcc, selector);
        fillWithRetry(tcc, selector, fillText);
    }

    public static void fillWithRetry(TestCaseConfig tcc, String selector, String text) {
        Utils.retryAction("fill locator", () -> fill(tcc, selector, text), Constants.MAX_ACTION_RETRIES);
    }

    public static void fill(TestCaseConfig tcc, String selector, String text) {
        LOGGER.debug("Fill locator [{}] with text [{}]", selector, text);
        Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
        tcc.page().locator(selector).fill(text, new Locator.FillOptions().setForce(true).setTimeout(TimeConstants.COMPONENT_LOAD_TIMEOUT));
        Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
    }

    // --------------------------
    // Wait for text
    // --------------------------
    public static void waitForContainsText(TestCaseConfig tcc, String selector, String text, boolean reload) {
        waitForContainsText(tcc, selector, text, TimeConstants.COMPONENT_LOAD_TIMEOUT, reload);
    }

    /**
     * Waits until the given selector contains the specified text within the provided timeout.
     * Optionally reloads the page if the text is not found during the wait.
     *
     * <p>If the locator resolves to multiple elements, their inner texts are combined and trimmed before checking.
     *
     * @param tcc config {@link TestCaseConfig} instance to reload if needed
     * @param selector the selector to check for the expected text
     * @param text the expected substring text to wait for in the locator
     * @param componentLoadTimeout maximum time in milliseconds to wait for the text to appear
     * @param reload if true, reloads the page on each poll when the text is not found
     */
    public static void waitForContainsText(TestCaseConfig tcc, String selector, String text, long componentLoadTimeout, boolean reload) {
        LOGGER.debug("Waiting for locator [{}] to contain text [{}]", selector, text);
        Wait.until("locator to contain text: " + text, TimeConstants.GLOBAL_POLL_INTERVAL_SHORT, componentLoadTimeout,
            () -> {
                String innerText = getTrimmedText(tcc.page().locator(selector).allInnerTexts().toString());
                LOGGER.debug("Current locator text [{}], should contain [{}]", innerText, text);
                if (innerText.contains(text)) {
                    LOGGER.debug("Current locator text [{}] contains correct text [{}]", innerText, text);
                    return true;
                }

                if (reload) {
                    tcc.page().reload(getDefaultReloadOpts());
                }

                LOGGER.debug("Current locator text [{}] does not contain [{}]", innerText, text);

                return false;
            },
            () -> LOGGER.error("Locator contains text [{}], it should contain [{}]", getTrimmedText(tcc.page().locator(selector).allInnerTexts().toString()), text)
        );
    }

    // --------------------------
    // Wait for attribute
    // --------------------------
    public static void waitForContainsAttribute(TestCaseConfig tcc, String selector, String text, String attribute, boolean reload) {
        waitForContainsAttribute(tcc, selector, text, attribute, TimeConstants.COMPONENT_LOAD_TIMEOUT, reload);
    }

    /**
     * Waits until a specified attribute of a locator contains an expected value.
     * <p>
     * This method periodically polls the given attribute of the provided {@link Locator}
     * until it matches the expected text, or a timeout is reached. Optionally reloads
     * the page during retries if the expected value is not yet present.
     * </p>
     *
     * @param tcc the config {@link TestCaseConfig} object representing the current browser page
     * @param selector the {@link Locator} whose attribute is being checked
     * @param text the expected value to be matched in the attribute
     * @param attribute the name of the attribute to inspect (e.g., "value", "aria-label")
     * @param componentLoadTimeout the maximum time (in milliseconds) to wait before failing
     * @param reload if {@code true}, the page will be reloaded between polling attempts
     *
     * @throws AssertionError if the attribute value does not match the expected text within the timeout
     */
    public static void waitForContainsAttribute(TestCaseConfig tcc, String selector, String text, String attribute, long componentLoadTimeout, boolean reload) {
        LOGGER.debug("Waiting for locator [{}] to contain value [{}]", selector, text);
        Wait.until("locator to contain text: " + text, TimeConstants.GLOBAL_POLL_INTERVAL_SHORT, componentLoadTimeout,
            () -> {
                String valueText = getTrimmedText(tcc.page().locator(selector).getAttribute(attribute));


                LOGGER.debug("Current locator value [{}], should contain [{}]", valueText, text);
                if (valueText.contains(text)) {
                    return true;
                }

                if (reload) {
                    tcc.page().reload(getDefaultReloadOpts());
                }
                return false;
            },
            () -> LOGGER.error("Locator contains [{}], should contain [{}]", getTrimmedText(tcc.page().locator(selector).getAttribute(attribute)), text)
        );
    }

    // --------------------------
    // Wait for locator count
    // --------------------------
    /**
     * Waits until the specified {@link Locator} has the expected number of elements.
     * Optionally reloads the page if the count does not match during the wait.
     *
     * @param tcc the {@link TestCaseConfig} containing the page instance to reload if needed
     * @param count the expected number of elements the locator should have
     * @param selector the selector whose elements count is checked
     * @param reload if true, reloads the page on each poll when the count is incorrect
     */
    public static void waitForLocatorCount(TestCaseConfig tcc, int count, String selector, boolean reload) {
        LOGGER.debug("Waiting for locator [{}] to contain item count [{}] reload={}", selector, count, reload);
        Wait.until("locator to have item count: " + count, TimeConstants.GLOBAL_POLL_INTERVAL_SHORT, TimeConstants.COMPONENT_LOAD_TIMEOUT,
            () -> {
                int locatorCount = tcc.page().locator(selector).all().size();
                if (locatorCount == count) {

                    LOGGER.debug("Locator has correct item count {}", count);
                    return true;
                }
                LOGGER.debug("Locator has incorrect item count {}, need {}", locatorCount, count);
                if (reload) {
                    tcc.page().reload(getDefaultReloadOpts());
                }
                return false;
            },
            () -> LOGGER.error("Page did not contain enough locators before timeout")
        );
    }

    // --------------------------
    // Wait for enabled element
    // --------------------------
    public static void waitForElementEnabledState(TestCaseConfig tcc, String selector, boolean shouldBeEnabled, boolean reload, long timeout) {
        waitForElementEnabledState(tcc, tcc.page().locator(selector), shouldBeEnabled, reload, timeout);
    }

    /**
     * Waits until a specific locator reaches the desired enabled or disabled state within the given timeout.
     * <p>
     * Optionally reloads the page if the state is not yet reached during the polling interval.
     * </p>
     *
     * @param tcc             the test case configuration containing the Playwright page
     * @param locator         the Playwright {@link Locator} to check the enabled state for
     * @param shouldBeEnabled {@code true} if the element should be enabled; {@code false} if it should be disabled
     * @param reload          {@code true} if the page should be reloaded on each failed check
     * @param timeout         the maximum amount of time (in milliseconds) to wait for the state to be achieved
     */
    public static void waitForElementEnabledState(TestCaseConfig tcc, Locator locator, boolean shouldBeEnabled, boolean reload, long timeout) {
        Wait.until("locator to be in state enabled=" + shouldBeEnabled, TimeConstants.GLOBAL_POLL_INTERVAL_SHORT, timeout,
            () -> {
                if (locator.isEnabled() == shouldBeEnabled) {
                    LOGGER.debug("Locator has correct state enabled={}", locator.isEnabled());
                    return true;
                }
                LOGGER.debug("Locator has incorrect state enabled={}, need enabled={}", locator.isEnabled(), shouldBeEnabled);
                if (reload) {
                    tcc.page().reload(getDefaultReloadOpts());
                }
                return false;
            },
            () -> LOGGER.error("Locator has incorrect state enabled={}, need enabled={}", locator.isEnabled(), shouldBeEnabled)
        );
    }

    /**
     * Waits for the Console UI to become ready by repeatedly checking the login page availability.
     * It navigates to the Kafka login page and verifies if the application is up and login elements are visible.
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
                    if (tcc.page().locator(CssSelectors.LOGIN_ANONYMOUSLY_BUTTON).isVisible()) {
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
            .setWaitUntil(WaitUntilState.NETWORKIDLE);
    }

    public static Page.ReloadOptions getDefaultReloadOpts() {
        return new Page.ReloadOptions()
            .setTimeout(TestFrameConstants.GLOBAL_TIMEOUT_SHORT)
            .setWaitUntil(WaitUntilState.NETWORKIDLE);
    }

    public static Page.WaitForURLOptions getDefaultWaitForUrlOpts() {
        return new Page.WaitForURLOptions()
            .setTimeout(TestFrameConstants.GLOBAL_TIMEOUT_SHORT)
            .setWaitUntil(WaitUntilState.NETWORKIDLE);
    }

    public static void screenshot(TestCaseConfig tcc) {
        screenshot(tcc, tcc.kafkaName(), "");
    }

    public static void screenshot(TestCaseConfig tcc, String kafkaName, String additionalSuffix) {
        // e.g. screenshots/testFilterTopics/topicst-33aaa/topics/screenshotname-2025-04-21__18-05-33.png
        String pageUrl = tcc.page().url().replace(PwPageUrls.getKafkaBaseUrl(tcc, kafkaName), "");

        String screenshotName = java.lang.String.join("/",
            KubeResourceManager.get().getTestContext().getDisplayName().replace("()", ""),
            tcc.namespace(),
            kafkaName,
            pageUrl.contains("?") ? pageUrl.split("\\?")[0] : pageUrl,
            additionalSuffix +
            (additionalSuffix.isEmpty() ? "" : "-") +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd__HH-mm-ss")) +
            ".png")
            .replaceAll("//+", "/");

        LOGGER.debug("Taking a screenshot: {}", screenshotName);
        tcc.page().screenshot(new Page.ScreenshotOptions().setPath(Path.of(Environment.SCREENSHOTS_DIR_PATH, screenshotName)));
    }



    /**
     * Removes stray or floating popup elements (e.g., tooltips, hints, or
     * info popups) that appear when hovering over certain elements, such as
     * question mark icons.
     *
     * <p>This is achieved by removing focus from the page body into a corner,
     * effectively dismissing any hover-triggered popups that may interfere
     * with test execution or element interaction.</p>
     *
     * @param tcc the {@link TestCaseConfig} containing the page instance to remove focus
     */
    public static void removeFocus(TestCaseConfig tcc) {
        LOGGER.info("Remove focus by moving mouse to X,Y = [0;0]");
        tcc.page().mouse().move(0, 0);

    }

    public static void saveTracing(BrowserContext context) {
        context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get(Environment.TRACING_DIR_PATH,
            KubeResourceManager.get().getTestContext().getDisplayName().replace("()", "") + "-trace.zip")));
    }
}
