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
        var configFile = writeConfiguration("""
                kubernetes:
                  enabled: false
                kafka:
                  clusters:
                    - name: test-kafka1
                      id: k1-id
                      properties:
                        bootstrap.servers: ${console.test.external-bootstrap}
                    - name: test-kafka2
                      id: k2-id
                      properties:
                        bootstrap.servers: ${console.test.random-bootstrap}
                    - name: test-kafka3
                      namespace: default
                      id: k3-id
                      listener: listener0
                """);

        return Map.of(
                "quarkus.kubernetes-client.devservices.enabled", "false",
                "console.config-path", configFile.getAbsolutePath());
    }
}
