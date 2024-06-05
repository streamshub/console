package com.github.streamshub.console.kafka.systemtest.deployment;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KafkaUnsecuredResourceManager extends KafkaResourceManager implements QuarkusTestResourceLifecycleManager {

    ServerSocket randomSocket;
    File configFile;

    @Override
    public Map<String, String> start() {
        deployments = DeploymentManager.newInstance(false);
        kafkaContainer = deployments.getKafkaContainer();
        String externalBootstrap = deployments.getExternalBootstrapServers();
        String profile = "%" + initArgs.get("profile") + ".";

        try {
            randomSocket = new ServerSocket(0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        URI randomBootstrapServers = URI.create("dummy://localhost:" + randomSocket.getLocalPort());

        try {
            configFile = File.createTempFile("console-test-config-", ".yaml");
            configFile.deleteOnExit();

            Files.writeString(configFile.toPath(), """
                    kafka:
                      clusters:
                        - name: test-kafka1
                          namespace: default
                          properties:
                            bootstrap.servers: %s
                        - name: test-kafka2
                          namespace: default
                          properties:
                            bootstrap.servers: %s
                        - name: test-kafka3
                          namespace: default
                          # listener is named and bootstrap.servers not set (will be retrieved from Kafka CR)
                          listener: listener0
                          properties:
                            security.protocol: SSL
                    """.formatted(
                            externalBootstrap,
                            randomBootstrapServers.toString(),
                            externalBootstrap),
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return Map.ofEntries(
                Map.entry(profile + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, externalBootstrap),
                Map.entry(profile + "console.config-path", configFile.getAbsolutePath()));
    }

    @Override
    public void stop() {
        super.stop();

        if (randomSocket != null) {
            try {
                randomSocket.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        configFile.delete();
    }
}
