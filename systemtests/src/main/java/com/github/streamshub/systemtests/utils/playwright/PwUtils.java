package com.github.streamshub.systemtests.utils.playwright;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.enums.BrowserTypes;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.locators.CssSelectors;
import com.github.streamshub.systemtests.locators.KafkaDashboardPageSelectors;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.console.ConsoleUtils;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.TimeoutError;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.microsoft.playwright.options.WaitUntilState;
import io.fabric8.kubernetes.api.model.Secret;
import io.skodjob.kubetest4j.KubeTestConstants;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import io.skodjob.kubetest4j.wait.Wait;
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
    public static void login(TestCaseConfig tcc, String kafkaName) {
        final String loginUrl = PwPageUrls.getKafkaLoginPage(tcc, kafkaName);
        LOGGER.info("Logging in to the Console with URL: {}", loginUrl);
        waitForConsoleUiAnonymousLoginToBecomeReady(tcc);
        // Anonymous login
        navigate(tcc, loginUrl);
        waitForUrl(tcc, loginUrl, false);
        // Go to login
        waitForLocatorAndClick(tcc, CssSelectors.LOGIN_ANONYMOUSLY_BUTTON);
        waitForUrl(tcc, PwPageUrls.getOverviewPage(tcc, kafkaName), true);
        LOGGER.info("Successfully logged into Console");
    }

    public static void login(TestCaseConfig tcc) {
        login(tcc, tcc.kafkaName());
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
        waitForLocatorVisible(tcc.page().locator(selector));
    }

    public static void waitForLocatorVisible(Locator locator) {
        waitForLocatorVisible(locator, TimeConstants.ELEMENT_VISIBILITY_TIMEOUT);
    }

    public static void waitForLocatorVisible(Locator locator, long timeout) {
        LOGGER.debug("Waiting for locator to be visible without reloading [{}]", locator);
        locator.waitFor(new Locator.WaitForOptions().setTimeout(timeout).setState(WaitForSelectorState.VISIBLE));
    }

    // --------------------------
    // Click actions
    // --------------------------
    public static void waitForLocatorAndClick(TestCaseConfig tcc, String selector) {
        waitForLocatorAndClick(tcc.page().locator(selector));
    }

    public static void waitForLocatorAndClick(Locator locator) {
        waitForLocatorVisible(locator);
        clickWithRetry(locator);
    }

    // Due to https://github.com/microsoft/playwright/issues/14946
    // some buttons or elements that change their visibility might throw errors onclick action
    public static void clickWithRetry(Locator locator) {
        Utils.retryAction("click on locator", () -> click(locator), Constants.DEFAULT_ACTION_RETRIES);
    }

    public static boolean click(Locator locator) {
        LOGGER.debug("Clicking on locator [{}]", locator);
        Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
        locator.click(new Locator.ClickOptions().setForce(true).setTimeout(TimeConstants.COMPONENT_LOAD_TIMEOUT));
        Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
        return true;
    }

    // --------------------------
    // Fill actions
    // --------------------------
    public static void waitForLocatorAndFill(TestCaseConfig tcc, String selector, String fillText) {
        waitForLocatorAndFill(tcc.page().locator(selector), fillText);
    }

    public static void waitForLocatorAndFill(Locator locator, String fillText) {
        waitForLocatorVisible(locator);
        fillWithRetry(locator, fillText);
    }

    public static void fillWithRetry(Locator locator, String text) {
        Utils.retryAction("fill locator", () -> fill(locator, text), Constants.DEFAULT_ACTION_RETRIES);
    }

    public static boolean fill(Locator locator, String text) {
        LOGGER.debug("Fill locator [{}] with text [{}]", locator, text);
        Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
        locator.fill(text, new Locator.FillOptions().setForce(true).setTimeout(TimeConstants.COMPONENT_LOAD_TIMEOUT));
        Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
        return true;
    }

    // --------------------------
    // Wait for text
    // --------------------------
    public static void waitForContainsText(TestCaseConfig tcc, String selector, String text, boolean reload) {
        waitForContainsText(tcc, selector, text, reload, true, TimeConstants.COMPONENT_LOAD_TIMEOUT, Constants.SELECTOR_RETRIES);
    }

    public static void waitForContainsText(TestCaseConfig tcc, String selector, String text, boolean reload, boolean exactCase) {
        waitForContainsText(tcc, selector, text, reload, exactCase, TimeConstants.COMPONENT_LOAD_TIMEOUT, Constants.SELECTOR_RETRIES);
    }

    public static void waitForContainsText(TestCaseConfig tcc, String selector, String text, long waitTime) {
        waitForContainsText(tcc, selector, text, true, true, waitTime, Constants.SELECTOR_RETRIES);
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
     * @param reload if true, reloads the page on each poll when the text is not found
     * @param exactCase if true, text must match exactly, if false String.contains is used instead
     */
    public static void waitForContainsText(TestCaseConfig tcc, String selector, String text, boolean reload, boolean exactCase, long waitTime, int retries) {
        LOGGER.debug("Waiting for locator [{}] to contain text [{}]", selector, text);
        Utils.retryAction("waitForContainsText: " + text,
            () -> {
                if (locatorContainsText(tcc.page().locator(selector), text, exactCase)) {
                    return true;
                }

                LOGGER.warn("Locator did not contain text [{}]", text);

                if (reload) {
                    tcc.page().reload(getDefaultReloadOpts());
                }

                return false;
            }, retries, waitTime
        );
    }

    public static boolean locatorContainsText(Locator locator, String expectedText, boolean exactCase) {
        // Text might be either inner or a content text
        String allInnerTexts = locator.allInnerTexts().toString();
        String innerText = getTrimmedText(allInnerTexts.isEmpty() || allInnerTexts.equals("[null]") ?
            locator.allTextContents().toString() : allInnerTexts);

        LOGGER.debug("Checking locator text [{}], expected [{}], exact case - {}", innerText, expectedText, exactCase);

        boolean containsText = innerText.contains(expectedText) ||
            !exactCase && innerText.toLowerCase(Locale.ROOT).contains(expectedText.toLowerCase(Locale.ROOT));

        if (containsText) {
            LOGGER.debug("Locator text [{}] CONTAINS expected [{}]", innerText, expectedText);
        } else {
            LOGGER.debug("Locator text [{}] does NOT contain expected [{}]", innerText, expectedText);
        }

        return containsText;
    }

    public static void waitForLocatorContainsText(Locator locator, String text, boolean exactCase) {
        waitForLocatorContainsText(locator, text, TimeConstants.COMPONENT_LOAD_TIMEOUT, exactCase);
    }

    public static void waitForLocatorContainsText(Locator locator, String text, long componentLoadTimeout, boolean exactCase) {
        Wait.until("locator to contain text: " + text, TimeConstants.GLOBAL_POLL_INTERVAL_SHORT, componentLoadTimeout,
            () -> {
                return locatorContainsText(locator, text, exactCase);
            },
            () -> LOGGER.error("Locator did not contain text [{}]", text)
        );
    }

    // --------------------------
    // Wait for attribute
    // --------------------------
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
     * @param reload if {@code true}, the page will be reloaded between polling attempts
     *
     * @throws AssertionError if the attribute value does not match the expected text within the timeout
     */
    public static void waitForAttributeContainsText(TestCaseConfig tcc, String selector, String text, String attribute, boolean reload, boolean exactCase) {
        LOGGER.debug("Waiting for locator [{}] to contain value [{}]", selector, text);
        Utils.retryAction("waitForAttributeContainsText: " + text,
            () -> {
                // For reliability let's wait for the selector before getting it's value
                waitForLocatorVisible(tcc, selector);

                if (attributeContainsText(tcc.page().locator(selector), attribute, text, exactCase)) {
                    return true;
                }

                LOGGER.warn("Locator atribute did not contain text [{}]", text);

                if (reload) {
                    reload(tcc);
                }

                return false;
            }
        );
    }

    public static boolean attributeContainsText(TestCaseConfig tcc, String selector, String attribute, String expectedText, boolean exactCase) {
        return attributeContainsText(tcc.page().locator(selector), attribute, expectedText, exactCase);
    }

    public static boolean attributeContainsText(Locator locator, String attribute, String expectedText, boolean exactCase) {
        String attributeValue = getTrimmedText(locator.getAttribute(attribute));

        LOGGER.debug("Checking locator attribute [{}] value [{}] against expected [{}], exact case - {}", attribute, attributeValue, expectedText, exactCase);

        boolean containsText = attributeValue.contains(expectedText) ||
            !exactCase && attributeValue.toLowerCase(Locale.ROOT).contains(expectedText.toLowerCase(Locale.ROOT));

        if (containsText) {
            LOGGER.debug("Locator attribute [{}] value [{}] CONTAINS expected [{}]", attribute, attributeValue, expectedText);
        } else {
            LOGGER.debug("Locator attribute [{}] value [{}] does NOT contain expected [{}]", attribute, attributeValue, expectedText);
        }

        return containsText;
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
        Utils.retryAction("waitForLocatorCount: " + count,
            () -> {
                int locatorCount = tcc.page().locator(selector).all().size();
                if (locatorCount == count) {
                    LOGGER.debug("Locator has correct item count {}", count);
                    return true;
                }

                LOGGER.warn("Locator has incorrect item count {}, need {}", locatorCount, count);

                if (reload) {
                    tcc.page().reload(getDefaultReloadOpts());
                }

                return false;
            }
        );
    }

    // --------------------------
    // Wait for enabled element
    // --------------------------
    /**
     * Waits until a specific locator reaches the desired enabled or disabled state within the given timeout.
     * <p>
     * Optionally reloads the page if the state is not yet reached during the polling interval.
     * </p>
     *
     * @param tcc             the test case configuration containing the Playwright page
     * @param selector         the Playwright selector {@link String} to check the enabled state for
     * @param shouldBeEnabled {@code true} if the element should be enabled; {@code false} if it should be disabled
     * @param reload          {@code true} if the page should be reloaded on each failed check
     */
    public static void waitForElementEnabledState(TestCaseConfig tcc, String selector, boolean shouldBeEnabled, boolean reload) {
        Utils.retryAction("waitForElementEnabledState enabled=" + shouldBeEnabled,
            () -> {
                Locator locator = tcc.page().locator(selector);

                if (locator.isEnabled() == shouldBeEnabled) {
                    LOGGER.debug("Locator has correct state enabled={}", locator.isEnabled());
                    return true;
                }

                LOGGER.warn("Locator has incorrect state enabled={}, need enabled={}", locator.isEnabled(), shouldBeEnabled);

                if (reload) {
                    tcc.page().reload(getDefaultReloadOpts());
                }

                return false;
            }
        );
    }

    /**
     * Waits for the Console UI to become ready by repeatedly checking the login page availability.
     * It navigates to the Kafka login page and verifies if the application is up and login elements are visible.
     *
     * @param tcc the {@link TestCaseConfig} containing the Playwright page instance used to perform the checks
     */
    public static void waitForConsoleUiAnonymousLoginToBecomeReady(TestCaseConfig tcc) {
        LOGGER.info("============= Waiting for Console Website to be online =============");
        Utils.retryAction("waitForConsoleUiAnonymousLoginToBecomeReady",
            () -> {
                LOGGER.debug("Try to reach out to the console web");

                // First test if application is fully running
                navigate(tcc, PwPageUrls.getKafkaLoginPage(tcc, tcc.kafkaName()));
                Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
                if (tcc.page().locator("body").innerText().contains("Application is not available")) {
                    LOGGER.info("Application is not available yet");
                    return false;
                }

                // Second test if login page is able to display a login button
                if (tcc.page().locator(CssSelectors.LOGIN_ANONYMOUSLY_BUTTON).isVisible()) {
                    LOGGER.info("Console website is ready");
                    return true;
                }

                return false;
            }
        );
    }

    public static void waitForConsoleUiWithKeycloakToBecomeReady(TestCaseConfig tcc) {
        LOGGER.info("============= Waiting for Console Website to be online =============");
        Utils.retryAction("waitForConsoleUiWithKeycloakToBecomeReady",
            () -> {
                LOGGER.debug("Console website reach-out try");

                // First test if application is fully running
                navigate(tcc, PwPageUrls.getKafkaLoginPage(tcc, tcc.kafkaName()));
                Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);

                if (tcc.page().locator("body").innerText().contains("Error")) {
                    LOGGER.info("Console website contains Error display");
                    return false;
                }

                // Second test if login page is able to display a login button
                if (tcc.page().locator(CssSelectors.LOGIN_KEYCLOAK_PAGE_TITLE).isVisible()) {
                    LOGGER.info("Console website is ready");
                    return true;
                }

                return false;
            }
        );
    }

    // ----------------
    // Default options
    // ----------------
    /**
     * Returns the default Playwright options used across system tests.
     *
     * <p>Uses {@link WaitUntilState#COMMIT} with a {@link KubeTestConstants#GLOBAL_TIMEOUT_SHORT} timeout.
     *
     * <h3>WaitUntilState options:</h3>
     * <ul>
     *   <li>{@code DOMCONTENTLOADED} - Resolves when the HTML is parsed. Too early for React/Next.js
     *       as the component tree has not mounted yet.</li>
     *   <li>{@code LOAD} - Resolves on the browser {@code load} event. Unreliable with Next.js App Router
     *       since client-side redirects and RSC hydration continue after this fires, causing subsequent
     *       {@code navigate()} calls to abort in-flight redirects with {@code ERR_ABORTED}.</li>
     *   <li>{@code NETWORKIDLE} - Resolves after 500ms of zero in-flight requests. Times out on dynamic
     *       pages due to continuous RSC streaming, Kafka metrics polling, and health check endpoints.</li>
     *   <li>{@code COMMIT} - Resolves as soon as the HTTP response headers are received. Navigation
     *       completes immediately without waiting for page content, preventing conflicts with
     *       client-side redirects. Explicit {@code waitFor*} calls after each navigate act as
     *       the real readiness gate.</li>
     * </ul>
     *
     * @return {@link Page.NavigateOptions} configured with {@code DOMCONTENTLOADED} and the short global timeout
     */
    public static Page.NavigateOptions getDefaultNavigateOpts() {
        return getDefaultNavigateOpts(KubeTestConstants.GLOBAL_TIMEOUT_SHORT);
    }

    public static Page.NavigateOptions getDefaultNavigateOpts(long timeout) {
        return new Page.NavigateOptions()
            .setTimeout(timeout)
            .setWaitUntil(WaitUntilState.LOAD);
    }

    public static Page.ReloadOptions getDefaultReloadOpts() {
        return new Page.ReloadOptions()
            .setTimeout(KubeTestConstants.GLOBAL_TIMEOUT_SHORT)
            .setWaitUntil(WaitUntilState.LOAD);
    }

    public static Page.WaitForURLOptions getDefaultWaitForUrlOpts() {
        return new Page.WaitForURLOptions()
            .setTimeout(KubeTestConstants.GLOBAL_TIMEOUT_SHORT)
            .setWaitUntil(WaitUntilState.LOAD);
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

    public static void saveTracing(BrowserContext context) {
        context.tracing().stop(new Tracing.StopOptions().setPath(Paths.get(Environment.TRACING_DIR_PATH,
            KubeResourceManager.get().getTestContext().getDisplayName().replace("()", "") + "-trace.zip")));
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

    public static void loginWithOidcUser(TestCaseConfig tcc, String username, String password) {
        final String loginUrl = PwPageUrls.getKafkaLoginPage(tcc, tcc.kafkaName());
        LOGGER.info("Logging in to the Console with URL: {}", loginUrl);
        waitForConsoleUiWithKeycloakToBecomeReady(tcc);
        navigate(tcc, loginUrl);
        // Login with user
        waitForLocatorAndFill(tcc, CssSelectors.LOGIN_KEYCLOAK_USERNAME_INPUT, username);
        waitForLocatorAndFill(tcc, CssSelectors.LOGIN_KEYCLOAK_PASSWORD_INPUT, password);
        waitForLocatorAndClick(tcc, CssSelectors.LOGIN_KEYCLOAK_SIGN_IN_BUTTON);
        // Go to overview page
        waitForUrl(tcc, ConsoleUtils.getConsoleUiUrl(tcc.consoleInstanceName(), true), true);
        LOGGER.info("Successfully logged into Console");
    }

    public static void logoutUser(TestCaseConfig tcc, String userName, boolean https) {
        Utils.retryAction("Log-out user " + userName, () -> {
            String dashboardUrl = ConsoleUtils.getConsoleUiUrl(tcc.consoleInstanceName(), https) + "/";

            // There is a xpath difference between logout button in dashboard and in navbar on other pages
            if (tcc.page().url().equals(dashboardUrl)) {
                waitForLocatorAndClick(tcc, KafkaDashboardPageSelectors.KDPS_CURRENTLY_LOGGED_USER_BUTTON);
            } else {
                waitForLocatorAndClick(tcc, CssSelectors.PAGES_CURRENTLY_LOGGED_USER_BUTTON);
            }

            waitForLocatorAndClick(tcc, CssSelectors.PAGES_LOGOUT_BUTTON);
            Utils.sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);

            if (tcc.page().url().equals(dashboardUrl) ||
                tcc.page().locator(KafkaDashboardPageSelectors.KDPS_CURRENTLY_LOGGED_USER_BUTTON).allInnerTexts().contains(userName)) {
                LOGGER.warn("User [" + userName + "] has not been logged out");
                return false;
            }

            return true;
        }, Constants.LOGOUT_RETRIES);
    }

    public static void loginWithKafkaCredentials(TestCaseConfig tcc, String namespace, String kafkaUser) {
        final String loginUrl = PwPageUrls.getKafkaLoginPage(tcc, tcc.kafkaName());
        LOGGER.info("Logging in to the Console with URL: {}", loginUrl);
        WaitUtils.waitForSecretReady(namespace, kafkaUser);
        String password = Utils.decodeFromBase64(ResourceUtils.getKubeResource(Secret.class, namespace, kafkaUser).getData().get("password"));

        tcc.page().navigate(loginUrl);

        // Login with user
        waitForLocatorAndFill(tcc, CssSelectors.PAGES_KAFKA_CREDENTIALS_NAME_INPUT, kafkaUser);
        waitForLocatorAndFill(tcc, CssSelectors.PAGES_KAFKA_CREDENTIALS_PASSWORD_INPUT, password);
        waitForLocatorAndClick(tcc, CssSelectors.PAGES_KAFKA_CREDENTIALS_LOGIN_BUTTON);
        // Wait for overview page
        waitForUrl(tcc, PwPageUrls.getOverviewPage(tcc, tcc.kafkaName()), true);
    }

    public static void navigate(TestCaseConfig tcc, String url) {
        navigate(tcc, url, false, false);
    }

    public static void navigate(TestCaseConfig tcc, String url, boolean waitForUrl, boolean waitForExactUrl) {
        LOGGER.info("Navigating to '{}'", url);
        Utils.retryAction("Navigate to page: " + url,
            () -> {
                try {
                    tcc.page().navigate(url, getDefaultNavigateOpts(TimeConstants.ELEMENT_VISIBILITY_TIMEOUT));
                    return true;
                } catch (TimeoutError e) {
                    LOGGER.warn("Navigation to '{}' timed out, retrying...", url);
                    // Force reload to reset broken HTTP/2 connection state
                    Utils.sleepWait(TimeConstants.ACTION_WAIT_SHORT);
                    tcc.page().reload();
                    return false;
                }
            }
        );

        if (waitForUrl) {
            LOGGER.info("Waiting for url '{}'", url);
            waitForUrl(tcc, url, waitForExactUrl);
        }
    }

    public static void reload(TestCaseConfig tcc) {
        LOGGER.info("Reloading page with current url '{}'", tcc.page().url());
        tcc.page().reload(getDefaultReloadOpts());
    }

    public static void waitForUrl(TestCaseConfig tcc, String url, boolean exact) {
        if (exact) {
            LOGGER.info("Waiting for exact url '{}'", url);
            tcc.page().waitForURL(url, getDefaultWaitForUrlOpts());
        } else {
            LOGGER.info("Waiting for url starting with '{}'", url);
            tcc.page().waitForURL(Pattern.compile(Pattern.quote(url) + ".*"), getDefaultWaitForUrlOpts());
        }
    }
}
