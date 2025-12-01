package com.github.streamshub.console.api.support;

import java.util.List;
import java.util.Map;

import com.github.streamshub.console.kafka.systemtest.deployment.StrimziCrdResourceManager;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * This test profile will disable/enable certain REST operations. Note that it
 * does not start a Kafka broker behind the REST API. Any tests using this
 * profile should not assume a working Kafka environment. That is, operations
 * against Kafka will fail.
 */
public class OperationFilterTestProfile implements QuarkusTestProfile {

    @Override
    public List<TestResourceEntry> testResources() {
        // Make sure the Strimzi CRDs are created in the Kube database to avoid informer startup failures
        return List.of(new TestResourceEntry(StrimziCrdResourceManager.class));
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
              //"quarkus.http.auth.proactive", "false",
              //"quarkus.http.auth.permission.\"oidc\".policy", "permit",
              "console.read-only", "true",
              "console.operations.createTopic.enabled", "true",
              "console.operations.describeTopic.enabled", "false");
    }

}