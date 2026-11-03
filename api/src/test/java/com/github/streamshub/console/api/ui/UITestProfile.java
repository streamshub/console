package com.github.streamshub.console.api.ui;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.nio.file.Path;
import java.util.Map;

public class UITestProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "console.config-path",
            Path.of("src/test/resources/config-test-ui.yaml").toAbsolutePath().toString(),
            "quarkus.quinoa.dev-server", "false");
    }
}
