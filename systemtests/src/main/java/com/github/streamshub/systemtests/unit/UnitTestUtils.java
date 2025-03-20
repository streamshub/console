package com.github.streamshub.systemtests.unit;


import java.lang.reflect.Field;
import java.util.Map;

public class UnitTestUtils {
    // Utility method to set env variable via reflection
    @SuppressWarnings("unchecked")
    public static void setEnv(String key, String value) throws Exception {
        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        ((Map<String, String>) field.get(env)).put(key, value);
    }

    @SuppressWarnings("unchecked")
    public static void removeEnv(String key) throws Exception {
        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        ((Map<String, String>) field.get(env)).remove(key);
    }
}
