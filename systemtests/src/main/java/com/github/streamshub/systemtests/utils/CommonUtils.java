package com.github.streamshub.systemtests.utils;

import io.skodjob.testframe.wait.WaitException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.BooleanSupplier;


public class CommonUtils {
    private static final Logger LOGGER = LogManager.getLogger(CommonUtils.class);

    // ---------------
    // Hash methods
    // --------------
    public static byte[] sha1Digest(byte[] toBeHashed) {
        try {
            // This is used to generate unique identifier which is not used for security => using SHA-1 is ok
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            return sha1.digest(toBeHashed);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to get SHA-1 hash", e);
        }
    }

    public static String hashStub(byte[] toBeHashed) {
        byte[] digest = sha1Digest(toBeHashed);
        return String.format("%040x", new BigInteger(1, digest))
            .substring(0, 4);
    }

    public static String hashStub(String toBeHashed) {
        return hashStub(toBeHashed.getBytes(StandardCharsets.US_ASCII)).substring(0, 4);
    }

    // ---------------
    // Wait For
    // --------------
    public static long waitFor(String description, long pollIntervalMs, long timeoutMs, BooleanSupplier ready) {
        return waitFor(description, pollIntervalMs, timeoutMs, ready, () -> {
        });
    }

    public static long waitFor(String description, long pollIntervalMs, long timeoutMs, BooleanSupplier ready, Runnable onTimeout) {
        LOGGER.debug("Waiting for {} ", description);
        long deadline = System.currentTimeMillis() + timeoutMs;
        String exceptionMessage = null;
        int exceptionCount = 0;
        StringWriter stackTraceError = new StringWriter();

        while (true) {
            boolean result;
            try {
                result = ready.getAsBoolean();
            } catch (Exception e) {
                exceptionMessage = e.getMessage();
                if (++exceptionCount == 1 && exceptionMessage != null) {
                    // Log the first exception as soon as it occurs
                    LOGGER.error("Exception waiting for {}, {}", description, exceptionMessage, e);
                    // log the stacktrace
                    e.printStackTrace(new PrintWriter(stackTraceError));
                }
                result = false;
            }
            long timeLeft = deadline - System.currentTimeMillis();
            if (result) {
                return timeLeft;
            }
            if (timeLeft <= 0) {
                if (exceptionCount > 1) {
                    LOGGER.error("Exception waiting for {}, {}", description, exceptionMessage);

                    if (!stackTraceError.toString()
                        .isEmpty()) {
                        // printing handled stacktrace
                        LOGGER.error(stackTraceError.toString());
                    }
                }
                onTimeout.run();
                WaitException waitException = new WaitException("Timeout after " + timeoutMs + " ms waiting for " + description);
                LOGGER.error(waitException.getMessage());
                throw waitException;
            }
            long sleepTime = Math.min(pollIntervalMs, timeLeft);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("{} not ready, will try again in {} ms ({}ms till timeout)", description, sleepTime, timeLeft);
            }
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                Thread.currentThread()
                    .interrupt();
                return deadline - System.currentTimeMillis();
            }
        }
    }
}
