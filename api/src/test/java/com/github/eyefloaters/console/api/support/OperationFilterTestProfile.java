package com.github.eyefloaters.console.api.support;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

/**
 * This test profile will disable/enable certain REST operations. Note that it
 * does not start a Kafka broker behind the REST API. Any tests using this
 * profile should not assume a working Kafka environment. That is, operations
 * against Kafka will fail.
 */
public class OperationFilterTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
              "quarkus.http.auth.proactive", "false",
              "quarkus.http.auth.permission.\"oidc\".policy", "permit",
              "console.read-only", "true",
              "console.operations.createTopic.enabled", "true",
              "console.operations.describeTopic.enabled", "false");
    }

}