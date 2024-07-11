package com.github.streamshub.console.kafka.systemtest;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import com.github.streamshub.console.kafka.systemtest.deployment.KafkaUnsecuredResourceManager;

import io.quarkus.test.junit.QuarkusTestProfile;

public class TestPlainProfile implements QuarkusTestProfile {

    static final String PROFILE = "testplain";
    public static final int MAX_PARTITIONS = 100;
    public static final int EXCESSIVE_PARTITIONS = 101;

    @Override
    public String getConfigProfile() {
        return PROFILE;
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(
                //new TestResourceEntry(KubernetesServerTestResource.class),
                new TestResourceEntry(KafkaUnsecuredResourceManager.class, Map.of("profile", PROFILE)));
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        var configFile = writeConfiguration("""
                kubernetes:
                  enabled: true
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
                    - name: test-kafka3
                      namespace: default
                      # listener is named and bootstrap.servers not set (will be retrieved from Kafka CR)
                      listener: listener0
                      properties:
                        security.protocol: SSL
                """);

        return Map.of("console.config-path", configFile.getAbsolutePath());
    }

    protected File writeConfiguration(String configurationYaml) {
        File configFile;

        try {
            configFile = File.createTempFile("console-test-config-", ".yaml");
            configFile.deleteOnExit();

            Files.writeString(configFile.toPath(), configurationYaml, StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return configFile;
    }
}
