package com.github.streamshub.console.api.service;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClusterServiceTest {

    @Test
    void testEnumNames() {
        List<String> result = KafkaClusterService.enumNames(List.of(
                org.apache.kafka.common.acl.AclOperation.ALTER,
                org.apache.kafka.common.acl.AclOperation.CLUSTER_ACTION,
                org.apache.kafka.common.acl.AclOperation.DESCRIBE,
                org.apache.kafka.common.acl.AclOperation.DESCRIBE_CONFIGS));

        assertEquals(List.of("ALTER", "CLUSTER_ACTION", "DESCRIBE", "DESCRIBE_CONFIGS"), result);
    }

    @Test
    void testEnumNamesEmpty() {
        List<String> result = KafkaClusterService.enumNames(Collections.emptyList());
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void testEnumNamesNull() {
        List<String> result = KafkaClusterService.enumNames(null);
        assertNull(result);
    }

    @ParameterizedTest
    @CsvSource({
        "  1, 'Unknown (<3.3)'", // Below range, metadata.version 1 - 6 are not in enum as of Kafka 4.2. Earliest is 3.3
        "999,  ",                // Above range (null), metadata.version 29 is the highest as of Kafka 4.2
        " 28, '4.2'"             // Within range, 28 & 29 indicate Kafka 4.2
    })
    void testMetadataLevelToVersionString(short level, String expectedVersion) {
        assertEquals(expectedVersion, KafkaClusterService.metadataLevelToVersionString(level));
    }
}
