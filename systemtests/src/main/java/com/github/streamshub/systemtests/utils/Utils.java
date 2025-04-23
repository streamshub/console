package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.exceptions.SetupException;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
}
