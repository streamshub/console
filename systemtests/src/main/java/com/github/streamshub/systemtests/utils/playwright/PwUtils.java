package com.github.streamshub.systemtests.utils.playwright;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.enums.BrowserTypes;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.wait.Wait;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;
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

    // -----------------
    // Wait for locator
    // -----------------
    public static void waitForLocatorVisible(TestCaseConfig tcc, String selector) {
        waitForLocatorVisible(CssSelectors.getLocator(tcc, selector));
    }

    public static void waitForLocatorVisible(Locator locator) {
        waitForLocatorVisible(locator, TimeConstants.ELEMENT_VISIBILITY_TIMEOUT);
    }

    public static void waitForLocatorVisible(Locator locator, long timeout) {
        LOGGER.debug("Waiting for locator to be visible without reloading [{}]", locator.toString());
        locator.waitFor(new Locator.WaitForOptions().setTimeout(timeout).setState(WaitForSelectorState.VISIBLE));
    }

    public static void waitForContainsText(TestCaseConfig tcc, Locator locator, String text, boolean reload) {
        waitForContainsText(tcc.page(), locator, text, TimeConstants.COMPONENT_LOAD_TIMEOUT, reload);
    }

    public static void waitForContainsText(TestCaseConfig tcc, String selector, String text, boolean reload) {
        waitForContainsText(tcc.page(), CssSelectors.getLocator(tcc, selector), text, TimeConstants.COMPONENT_LOAD_TIMEOUT, reload);
    }

    public static void waitForContainsAttribute(TestCaseConfig tcc, String selector, String text, String attribute, boolean reload) {
        waitForContainsAttribute(tcc.page(), CssSelectors.getLocator(tcc, selector), text, attribute, TimeConstants.COMPONENT_LOAD_TIMEOUT, reload);
    }

    // --------------------------
    // Wait for locator and act
    // --------------------------
    public static void waitForLocatorAndClick(TestCaseConfig tcc, String selector) {
        waitForLocatorAndClick(tcc.page().locator(selector));
    }

    public static void waitForLocatorAndClick(Locator locator) {
        waitForLocatorVisible(locator);
        clickWithRetry(locator);
    }

    public static void waitForLocatorAndFill(TestCaseConfig tcc, String selector, String fillText) {
        waitForLocatorAndFill(tcc.page().locator(selector), fillText);
    }

    public static void waitForLocatorAndFill(Locator locator, String fillText) {
        waitForLocatorVisible(locator);
        fillWithRetry(locator, fillText);
    }

    // --------------------------
    // Click and Fill actions
    // --------------------------
    // Due to https://github.com/microsoft/playwright/issues/14946
    // some buttons or elements that change their visibility might throw errors onclick action
    public static void clickWithRetry(Locator locator) {
        retryAction("click on locator", () -> click(locator), Constants.MAX_ACTION_RETRIES);
    }

    public static void click(TestCaseConfig tcc, String selector) {
        click(tcc.page().locator(selector));
    }

    public static void click(Locator locator) {
        LOGGER.debug("Clicking on locator [{}]", locator.toString());
        sleepWaitForComponent(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
        locator.click(new Locator.ClickOptions().setForce(true).setTimeout(TimeConstants.COMPONENT_LOAD_TIMEOUT));
        sleepWaitForComponent(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
    }

    public static void fillWithRetry(Locator locator, String text) {
        retryAction("fill locator", () -> fill(locator, text), Constants.MAX_ACTION_RETRIES);
    }

    public static void fill(TestCaseConfig tcc, String selector, String text) {
        fill(tcc.page().locator(selector), text);
    }

    public static void fill(Locator locator, String text) {
        LOGGER.debug("Fill locator [{}] with text [{}]", locator.toString(), text);
        sleepWaitForComponent(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
        locator.fill(text, new Locator.FillOptions().setForce(true).setTimeout(TimeConstants.COMPONENT_LOAD_TIMEOUT));
        sleepWaitForComponent(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
    }

    /**
     * Executes the given action with retry logic in case of failure.
     * <p>
     * This method attempts to run the provided {@link Runnable} action up to a maximum number of retries.
     * If the action throws an exception, it will retry the execution after a short wait interval.
     * If all attempts fail, a {@link RuntimeException} is thrown with the last encountered exception as the cause.
     * </p>
     *
     * @param actionName a descriptive name for the action, used in logging to identify what is being retried
     * @param action     the {@link Runnable} task to execute
     * @param maxRetries the maximum number of retry attempts before failing the execution
     *
     * @throws RuntimeException if the action fails after the specified number of retries
     */
    public static void retryAction(String actionName, Runnable action, int maxRetries) {
        int attempts = 1;
        while (attempts < Constants.MAX_ACTION_RETRIES) {
            try {
                LOGGER.debug("Trying to run an action {} - {}/{}", actionName, attempts, Constants.MAX_ACTION_RETRIES);
                action.run();
                LOGGER.debug("Action {} was successful on attempt {}/{}", actionName, attempts, Constants.MAX_ACTION_RETRIES);
                return;
            } catch (Exception e) {
                if (attempts >= maxRetries) {
                    throw new RuntimeException("Action " + actionName + " failed after retries", e);
                }
                LOGGER.debug("Action {} failed, trying again", actionName);
                attempts++;
                try {
                    Thread.sleep(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
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
        LOGGER.debug("Waiting for locator [{}] to contain text [{}]", locator.toString(), text);
        Wait.until("locator to contain text: " + text, TimeConstants.GLOBAL_POLL_INTERVAL_SHORT, componentLoadTimeout,
            () -> {
                String innerText = getTrimmedText(locator.allInnerTexts().toString());
                LOGGER.debug("Current locator text [{}], should contain [{}]", innerText, text);
                if (innerText.contains(text)) {
                    LOGGER.debug("Current locator text [{}] contains correct text [{}]", innerText, text);
                    return true;
                }

                if (reload) {
                    page.reload(getDefaultReloadOpts());
                }

                LOGGER.debug("Current locator text [{}] does not contain [{}]", innerText, text);

                return false;
            },
            () -> LOGGER.error("Locator does not contain text [{}], instead it contains [{}]", text, getTrimmedText(locator.textContent()))
        );
    }


    /**
     * Waits until a specified attribute of a locator contains an expected value.
     * <p>
     * This method periodically polls the given attribute of the provided {@link Locator}
     * until it matches the expected text, or a timeout is reached. Optionally reloads
     * the page during retries if the expected value is not yet present.
     * </p>
     *
     * @param page                 the Playwright {@link Page} object representing the current browser page
     * @param locator              the {@link Locator} whose attribute is being checked
     * @param text                 the expected value to be matched in the attribute
     * @param attribute            the name of the attribute to inspect (e.g., "value", "aria-label")
     * @param componentLoadTimeout the maximum time (in milliseconds) to wait before failing
     * @param reload               if {@code true}, the page will be reloaded between polling attempts
     *
     * @throws AssertionError if the attribute value does not match the expected text within the timeout
     */
    public static void waitForContainsAttribute(Page page, Locator locator, String text, String attribute, long componentLoadTimeout, boolean reload) {
        LOGGER.debug("Waiting for locator [{}] to contain value [{}]", locator.toString(), text);
        Wait.until("locator to contain text: " + text, TimeConstants.GLOBAL_POLL_INTERVAL_SHORT, componentLoadTimeout,
            () -> {
                String valueText = getTrimmedText(locator.getAttribute(attribute).toString());

                LOGGER.debug("Current locator value [{}], should contain [{}]", valueText, text);
                if (valueText.equals(text)) {
                    return true;
                }

                if (reload) {
                    page.reload(getDefaultReloadOpts());
                }
                return false;
            },
            () -> LOGGER.error("Locator does not contain value [{}], instead it contains [{}]", text, getTrimmedText(locator.textContent()))
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
        LOGGER.debug("Waiting for locator [{}] to contain item count [{}] reload={}", locator.toString(), count, reload);
        Wait.until("locator to have item count: " + count, TimeConstants.GLOBAL_POLL_INTERVAL_SHORT, TimeConstants.COMPONENT_LOAD_TIMEOUT,
            () -> {
                if (locator.all().size() == count) {
                    LOGGER.debug("Locator has correct item count {}", count);
                    return true;
                }
                LOGGER.debug("Locator has incorrect item count {}, need {}", locator.all().size(), count);
                if (reload) {
                    tcc.page().reload(getDefaultReloadOpts());
                }
                return false;
            },
            () -> LOGGER.error("Page does not have enough locators count {} out of required {}", locator.all().size(), count)
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

    public static void sleepWaitForComponent(long timeInMilis) {
        LOGGER.debug("Giving component time to stabilize");
        try {
            Thread.sleep(timeInMilis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Sleep was interrupted due to: {}", e.getMessage());
        }
    }
}
