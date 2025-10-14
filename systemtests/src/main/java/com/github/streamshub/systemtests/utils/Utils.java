package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.exceptions.PlaywrightActionExecutionException;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Utils {
    private static final Logger LOGGER = LogWrapper.getLogger(Utils.class);

    private Utils() {}

    public static String hashStub(String stringToBeHashed) {
        try {
            byte[] toBeHashed = stringToBeHashed.getBytes(StandardCharsets.UTF_8);
            byte[] digest = MessageDigest.getInstance("SHA-512").digest(toBeHashed);
            return String.format("%040x", new BigInteger(1, digest)).substring(0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new SetupException("Failed to hash:", e);
        }
    }

    public static String decodeFromBase64(String data)  {
        return new String(Base64.getDecoder().decode(data), StandardCharsets.US_ASCII);
    }

    /**
     * Retrieves the {@link TestCaseConfig} associated with the current test execution context.
     *
     * <p>This method first attempts to fetch an existing {@code TestCaseConfig} from the global store
     * of the JUnit {@link ExtensionContext}. If no configuration is found for the current test’s
     * unique identifier, a new {@code TestCaseConfig} is created, initialized with the test context,
     * and stored in the global namespace for later reuse.</p>
     *
     * <p>This ensures that each test case has access to a consistent configuration object
     * throughout its lifecycle, while avoiding redundant re-initialization.</p>
     *
     * @return the {@code TestCaseConfig} instance for the current test case
     */
    public static TestCaseConfig getTestCaseConfig() {
        final ExtensionContext testContext = KubeResourceManager.get().getTestContext();
        final ExtensionContext.Store store = testContext.getStore(ExtensionContext.Namespace.GLOBAL);
        final String key = testContext.getUniqueId();

        TestCaseConfig tcc = store.get(key, TestCaseConfig.class);
        if (tcc == null) {
            LOGGER.info("Init TestCaseConfig instance and store it to a test context with unique testContext ID: {}", key);
            tcc = new TestCaseConfig(testContext);
            store.put(key, tcc);
        }
        return tcc;
    }

    /**
     * Executes the given action with retry logic in case of failure.
     * <p>
     * This method attempts to run the provided {@link Runnable} action up to a maximum number of retries.
     * If the action throws an exception, it will retry the execution after a short wait interval.
     * If all attempts fail, a {@link PlaywrightActionExecutionException} is thrown with the last encountered exception as the cause.
     * </p>
     *
     * @param actionName a descriptive name for the action, used in logging to identify what is being retried
     * @param action     the {@link Runnable} task to execute
     * @param maxRetries the maximum number of retry attempts before failing the execution
     *
     * @throws PlaywrightActionExecutionException if the action fails after the specified number of retries
     */
    public static void retryAction(String actionName, Runnable action, int maxRetries) {
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                LOGGER.debug("Running action '{}' (attempt {}/{})", actionName, attempt, maxRetries);
                action.run();
                LOGGER.debug("Action '{}' succeeded on attempt {}/{}", actionName, attempt, maxRetries);
                return;
            } catch (Exception e) {
                if (attempt == maxRetries) {
                    throw new PlaywrightActionExecutionException(String.format("Action '%s' failed after %d attempts", actionName, maxRetries), e);
                }
                LOGGER.debug("Action '{}' failed on attempt {}/{}, retrying...", actionName, attempt, maxRetries);
                sleepWait(TimeConstants.UI_COMPONENT_REACTION_INTERVAL_SHORT);
            }
        }
    }

    public static void sleepWait(long timeInMilis) {
        LOGGER.debug("Giving component time to stabilize");
        try {
            Thread.sleep(timeInMilis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Sleep was interrupted due to: {}", e.getMessage());
        }
    }
}
