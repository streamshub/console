package com.github.eyefloaters.console.legacy.handlers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.eyefloaters.console.legacy.model.Types;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestOperationsTest {
    private static final int MAX_PARTITIONS = 100;
    protected RestOperations restOperations = new RestOperations();

    @ParameterizedTest(name = "testNumPartitionsLessThanEqualToMax")
    @ValueSource(ints = {99, 100})
    void testNumPartitionsLessThanEqualToMaxTrue(int numPartitions) {
        Types.TopicSettings settings = new Types.TopicSettings();
        settings.setNumPartitions(numPartitions);
        assertTrue(restOperations.numPartitionsLessThanEqualToMax(settings, MAX_PARTITIONS));
    }

    @Test
    void testNumPartitionsLessThanEqualToMaxFalse() {
        Types.TopicSettings settings = new Types.TopicSettings();
        settings.setNumPartitions(101);
        assertFalse(restOperations.numPartitionsLessThanEqualToMax(settings, MAX_PARTITIONS));
    }

}
