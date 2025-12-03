package com.github.streamshub.console.kafka.systemtest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.streamshub.console.kafka.systemtest.deployment.KafkaResourceManager;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Same as profile {@linkplain TestPlainProfile}, but disables Kubernetes use by setting
 * properties {@code kuberentes.enabled=false} in the application's configuration YAML and
 * {@code quarkus.kubernetes-client.devservices.enabled=false} to disable the testing/mock
 * Kubernetes API server.
 */
public class TestPlainNoK8sProfile extends TestPlainProfile implements QuarkusTestProfile {

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(KafkaResourceManager.class, Collections.emptyMap()));
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
                "quarkus.kubernetes-client.devservices.enabled", "false",
                "console.config-path", getClass().getResource("/config-testplain-nok8s.yaml").getPath());
    }
}
