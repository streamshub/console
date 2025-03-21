package com.github.streamshub.systemtests.unit;

import java.lang.reflect.Field;
import java.util.Map;

public class UnitTestUtils {
    private UnitTestUtils() {}

    // Utility method to set env variable via reflection
    @SuppressWarnings("unchecked")
    public static void setEnv(String key, String value) throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        ((Map<String, String>) field.get(env)).put(key, value);
    }

    @SuppressWarnings("unchecked")
    public static void removeEnv(String key) throws NoSuchFieldException, IllegalAccessException {
        Map<String, String> env = System.getenv();
        Field field = env.getClass().getDeclaredField("m");
        field.setAccessible(true);
        ((Map<String, String>) field.get(env)).remove(key);
    }

    public static void setField(Object target, String fieldName, Object newValue) throws NoSuchFieldException, IllegalAccessException {
        Field field;
        field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, newValue);
    }
}
