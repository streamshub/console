package com.github.eyefloaters.console.api.support;

import java.util.Map;

import io.quarkus.test.junit.QuarkusTestProfile;

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