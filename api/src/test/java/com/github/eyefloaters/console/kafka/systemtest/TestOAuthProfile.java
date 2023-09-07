package com.github.eyefloaters.console.kafka.systemtest;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.List;
import java.util.Map;

import com.github.eyefloaters.console.kafka.systemtest.deployment.KafkaOAuthSecuredResourceManager;

public class TestOAuthProfile implements QuarkusTestProfile {

    static final String PROFILE = "testoauth";
    public static final int MAX_PARTITIONS = 100;
    public static final int EXCESSIVE_PARTITIONS = 101;

    @Override
    public String getConfigProfile() {
        return PROFILE;
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(KafkaOAuthSecuredResourceManager.class, Map.of("profile", PROFILE)));
    }
}
