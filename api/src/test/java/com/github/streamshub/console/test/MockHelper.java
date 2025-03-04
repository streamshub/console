package com.github.streamshub.console.test;

import java.util.Map;
import java.util.function.Function;

import org.mockito.Mockito;

public class MockHelper {

    public static <T> T mockAll(Class<T> mockType, Map<Function<T, Object>, Object> properties) {
        T mock = Mockito.mock(mockType);
        properties.forEach((getter, value) -> Mockito.when(getter.apply(mock)).thenReturn(value));
        return mock;
    }
}
