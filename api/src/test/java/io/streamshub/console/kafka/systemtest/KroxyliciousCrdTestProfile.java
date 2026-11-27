package io.streamshub.console.kafka.systemtest;

import java.util.Collections;
import java.util.List;

import io.streamshub.console.kafka.systemtest.deployment.KroxyliciousCrdResourceManager;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * Minimal profile for tests that only need the Kroxylicious
 * {@code VirtualKafkaCluster} CRD installed in the devservices Kubernetes
 * API — unlike {@link TestPlainProfile}, this does not start Apicurio,
 * Keycloak, or a real Kafka broker.
 */
public class KroxyliciousCrdTestProfile implements QuarkusTestProfile {

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(new TestResourceEntry(KroxyliciousCrdResourceManager.class, Collections.emptyMap(), true));
    }
}
