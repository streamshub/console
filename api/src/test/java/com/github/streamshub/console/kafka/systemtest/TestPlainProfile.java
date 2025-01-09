package com.github.streamshub.console.kafka.systemtest;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.github.streamshub.console.kafka.systemtest.deployment.KafkaResourceManager;
import com.github.streamshub.console.kafka.systemtest.deployment.KeycloakResourceManager;
import com.github.streamshub.console.kafka.systemtest.deployment.StrimziCrdResourceManager;

import io.quarkus.test.junit.QuarkusTestProfile;

public class TestPlainProfile implements QuarkusTestProfile {

    static final String PROFILE = "testplain";
    public static final int MAX_PARTITIONS = 100;
    public static final int EXCESSIVE_PARTITIONS = 101;

    static {
        /*
         * Requires JDK 11.0.4+. If the `Host` header is not set, Keycloak will
         * generate tokens with an issuer URI containing localhost:<random port>.
         */
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "host");
    }

    @Override
    public String getConfigProfile() {
        return PROFILE;
    }

    @Override
    public List<TestResourceEntry> testResources() {
        return List.of(
                new TestResourceEntry(StrimziCrdResourceManager.class, Collections.emptyMap(), true),
                new TestResourceEntry(KeycloakResourceManager.class, Collections.emptyMap(), true),
                new TestResourceEntry(KafkaResourceManager.class, Map.of("profile", PROFILE), true));
    }

    @Override
    public Map<String, String> getConfigOverrides() {
        var configFile = writeConfiguration("""
                kubernetes:
                  enabled: true

                schemaRegistries:
                  - name: test-registry
                    ###
                    # This is the property used by Dev Services for Apicurio Registry
                    # https://quarkus.io/guides/apicurio-registry-dev-services
                    ###
                    url: ${mp.messaging.connector.smallrye-kafka.apicurio.registry.url}

                kafka:
                  clusters:
                    - name: test-kafka1
                      namespace: default
                      id: k1-id
                      schemaRegistry: test-registry
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
