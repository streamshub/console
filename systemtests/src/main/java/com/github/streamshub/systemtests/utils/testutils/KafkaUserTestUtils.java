package com.github.streamshub.systemtests.utils.testutils;

import io.strimzi.api.kafka.model.user.acl.AclRule;

import java.util.Locale;

public class KafkaUserTestUtils {
    public static String getFormattedOperations(AclRule rule) {
        return rule.getOperations().stream()
            .map(op -> op.name().toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.joining(", "));
    }
}
