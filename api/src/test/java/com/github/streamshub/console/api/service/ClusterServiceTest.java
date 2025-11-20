package com.github.streamshub.console.api.service;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

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
}
