package org.bf2.admin.kafka.systemtest;

import io.quarkus.test.junit.QuarkusTestProfile;
import org.bf2.admin.kafka.systemtest.deployment.KafkaUnsecuredResourceManager;

import java.util.List;
import java.util.Map;

public class TestPlainProfile implements QuarkusTestProfile {

    static final String PROFILE = "testplain";
    public static final int MAX_PARTITIONS = 100;
    public static final int EXCESSIVE_PARTITIONS = 101;

    @Override
    public String getConfigProfile() {
        return PROFILE;
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(KafkaUnsecuredResourceManager.class, Map.of("profile", PROFILE)));
    }

}
