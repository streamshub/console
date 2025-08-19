package com.github.streamshub.systemtests.interfaces;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolver;

public class SharedResourcesParameterResolver implements ParameterResolver {

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        return SHARED_SETUP_RESULTS.containsKey("SHARED_1"); // or check by annotation
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) {
        Object[] values = SHARED_SETUP_RESULTS.get("SHARED_1");

        // Return value based on parameter type or index
        Class<?> type = parameterContext.getParameter().getType();
        for (Object val : values) {
            if (type.isInstance(val)) {
                return val;
            }
        }
        return null;
    }
}

