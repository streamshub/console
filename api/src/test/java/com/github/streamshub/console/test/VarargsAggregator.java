package com.github.streamshub.console.test;

import java.lang.reflect.Array;
import java.util.stream.IntStream;

import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregationException;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Aggregates multiple values from a {@link CsvSource} entry to a varargs method
 * parameter.
 */
public class VarargsAggregator implements ArgumentsAggregator {
    @Override
    public Object aggregateArguments(ArgumentsAccessor accessor, ParameterContext context) throws ArgumentsAggregationException {
        Class<?> parameterType = context.getParameter().getType();
        Class<?> componentType = parameterType.getComponentType();

        return IntStream.range(context.getIndex(), accessor.size())
                .mapToObj(index -> accessor.get(index, componentType))
                .toArray(size -> (Object[]) Array.newInstance(componentType, size));
    }
}
