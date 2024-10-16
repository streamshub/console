package com.github.streamshub.console.kafka.systemtest;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;

import com.github.streamshub.console.kafka.systemtest.deployment.KafkaUnsecuredResourceManager;
import com.github.streamshub.console.kafka.systemtest.deployment.StrimziCrdResourceManager;

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
                new TestResourceEntry(StrimziCrdResourceManager.class),
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

                    # missing required bootstrap.servers and sasl.mechanism
                    - name: test-kafkaX
                      properties:
                        sasl.jaas.config: something

                    - name: test-kafkaY
                      properties:
                        bootstrap.servers: ${console.test.external-bootstrap}

                    # duplicate test-kafkaY that will be ignored
                    - name: test-kafkaY
                      properties:
                        bootstrap.servers: ${console.test.external-bootstrap}
                        sasl.mechanism: SCRAM-SHA-512
                        sasl.jaas.config: something
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
