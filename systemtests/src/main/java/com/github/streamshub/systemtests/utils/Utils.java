package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.TestCaseConfig;
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
}
