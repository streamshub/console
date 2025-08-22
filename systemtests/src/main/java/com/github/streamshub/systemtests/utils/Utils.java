package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.exceptions.SetupException;

import io.skodjob.testframe.resources.KubeResourceManager;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Utils {

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

    public static TestCaseConfig getTestCaseConfig() {
        final var ctx = KubeResourceManager.get().getTestContext();
        final var store = ctx.getStore(ExtensionContext.Namespace.GLOBAL);
        final String key = ctx.getUniqueId();

        TestCaseConfig tcc = store.get(key, TestCaseConfig.class);
        if (tcc == null) {
            tcc = new TestCaseConfig(ctx);
            store.put(key, tcc);
        }
        return tcc;
    }
}
