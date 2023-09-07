package com.github.eyefloaters.console.api.support;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

@Provider
public class StringListParamConverterProvider implements ParamConverterProvider {

    @SuppressWarnings("unchecked")
    @Override
    public <T> ParamConverter<T> getConverter(Class<T> rawType, Type genericType, Annotation[] annotations) {
        if (List.class.equals(rawType) && genericType instanceof ParameterizedType parameterizedType) {
            Type[] typeArgs = parameterizedType.getActualTypeArguments();

            if (typeArgs.length == 1 && String.class.getName().equals(typeArgs[0].getTypeName())) {
                return (ParamConverter<T>) new StringListParamConverter();
            }
        }

        return null;
    }

    public static class StringListParamConverter implements ParamConverter<List<String>> {
        @Override
        public List<String> fromString(String value) {
            if (value == null) {
                return Collections.emptyList();
            }

            // Remove leading/trailing square braces added by RestEasy parameter extractor
            int begin = value.startsWith("[") ? 1 : 0;
            int end = value.length() - (value.endsWith("]") ? 1 : 0);
            value = value.substring(begin, end);

            return Arrays.stream(value.split(","))
                    .filter(Predicate.not(String::isBlank))
                    .map(String::trim)
                    .toList();
        }

        @Override
        public String toString(List<String> value) {
            if (value == null || value.isEmpty()) {
                return null;
            }

            return String.join(",", value);
        }
    }
}
