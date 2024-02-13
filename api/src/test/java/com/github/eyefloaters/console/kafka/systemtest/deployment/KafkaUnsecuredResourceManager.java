package com.github.eyefloaters.console.kafka.systemtest.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KafkaUnsecuredResourceManager extends KafkaResourceManager implements QuarkusTestResourceLifecycleManager {

    ServerSocket randomSocket;

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

        return Map.ofEntries(
                Map.entry(profile + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, externalBootstrap),
                Map.entry(profile + "console.kafka.testk1", "default/test-kafka1"),
                Map.entry(profile + "console.kafka.testk1." + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, externalBootstrap),
                Map.entry(profile + "console.kafka.testk2", "default/test-kafka2"),
                Map.entry(profile + "console.kafka.testk2." + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, randomBootstrapServers.toString()),
                // Placeholder configuration to allow for a CR to be added named test-kafka3 that will proxy to test-kafka1
                Map.entry(profile + "console.kafka.testk3", "default/test-kafka3"),
                Map.entry(profile + "console.kafka.testk3." + CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name),
                Map.entry(profile + "console.kafka.testk3." + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, externalBootstrap));
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
    }
}
