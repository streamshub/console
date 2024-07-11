package com.github.streamshub.console.kafka.systemtest.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;

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
                Map.entry(profile + "console.test.external-bootstrap", externalBootstrap),
                Map.entry(profile + "console.test.random-bootstrap", randomBootstrapServers.toString()));
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
