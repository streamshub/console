package com.github.streamshub.console.kafka.systemtest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.streamshub.console.kafka.systemtest.deployment.ApicurioResourceManager;
import com.github.streamshub.console.kafka.systemtest.deployment.KafkaResourceManager;
import com.github.streamshub.console.kafka.systemtest.deployment.KeycloakResourceManager;
import com.github.streamshub.console.kafka.systemtest.deployment.ResourceManagerBase;
import com.github.streamshub.console.kafka.systemtest.deployment.StrimziCrdResourceManager;

import io.quarkus.test.junit.QuarkusTestProfile;

public class TestPlainProfile implements QuarkusTestProfile {

    static final String PROFILE = "testplain";
    public static final int MAX_PARTITIONS = 100;
    public static final int EXCESSIVE_PARTITIONS = 101;

    static {
        /*
         * Requires JDK 11.0.4+. If the `Host` header is not set, Keycloak will
         * generate tokens with an issuer URI containing localhost:<random port>.
         */
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "host");
    }

    private final List<TestResourceEntry> testResources;

    public TestPlainProfile() {
        testResources = List.of(
                new TestResourceEntry(ApicurioResourceManager.class, Map.ofEntries(
                        Map.entry(ResourceManagerBase.DEPENDENCIES, KeycloakResourceManager.class.getName())
                    ), true),
                new TestResourceEntry(StrimziCrdResourceManager.class, Collections.emptyMap(), true),
                new TestResourceEntry(KeycloakResourceManager.class, Collections.emptyMap(), true),
                new TestResourceEntry(KafkaResourceManager.class, Collections.emptyMap(), true)
            );
    }

    @Override
    public String getConfigProfile() {
        return PROFILE;
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return testResources;
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("console.config-path", getClass().getResource("/config-testplain.yaml").getPath());
    }
}
