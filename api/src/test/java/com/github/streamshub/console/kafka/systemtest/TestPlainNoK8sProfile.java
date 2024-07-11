package com.github.streamshub.console.kafka.systemtest;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.List;
import java.util.Map;

import com.github.streamshub.console.kafka.systemtest.deployment.KafkaUnsecuredResourceManager;

/**
 * Same as profile {@linkplain TestPlainProfile}, but disables Kubernetes use by setting
 * properties {@code kuberentes.enabled=false} in the application's configuration YAML and
 * {@code quarkus.kubernetes-client.devservices.enabled=false} to disable the testing/mock
 * Kubernetes API server.
 */
public class TestPlainNoK8sProfile extends TestPlainProfile implements QuarkusTestProfile {

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(KafkaUnsecuredResourceManager.class, Map.of("profile", PROFILE)));
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        var configFile = writeConfiguration("""
                kubernetes:
                  enabled: false
                kafka:
                  clusters:
                    - name: test-kafka1
                      namespace: default
                      id: k1-id
                      properties:
                        bootstrap.servers: ${console.test.external-bootstrap}
                    - name: test-kafka2
                      namespace: default
                      id: k2-id
                      properties:
                        bootstrap.servers: ${console.test.random-bootstrap}
                """);

        return Map.of(
                "quarkus.kubernetes-client.devservices.enabled", "false",
                "console.config-path", configFile.getAbsolutePath());
    }
}
