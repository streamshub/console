package io.streamshub.console.kafka.systemtest;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.streamshub.console.kafka.systemtest.deployment.KroxyliciousCrdResourceManager;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Minimal profile for testing VKC-based {@code kind} auto-marking: two
 * statically-configured clusters (one with an explicit {@code kind}, one
 * without) plus the Kroxylicious {@code VirtualKafkaCluster} CRD installed
 * in the devservices Kubernetes API. Unlike {@link TestPlainProfile}, this
 * does not start Apicurio, Keycloak, or a real Kafka broker — this test
 * doesn't need live broker connectivity, only the configured
 * {@code bootstrap.servers} string and discovered VKC resources.
 */
public class KroxyliciousMarkingTestProfile implements QuarkusTestProfile {

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(KroxyliciousCrdResourceManager.class, Collections.emptyMap(), true));
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of("console.config-path", getClass().getResource("/config-vkcmarking.yaml").getPath());
    }
}
