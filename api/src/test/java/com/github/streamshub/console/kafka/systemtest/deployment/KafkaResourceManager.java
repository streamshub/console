package com.github.streamshub.console.kafka.systemtest.deployment;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KafkaResourceManager implements QuarkusTestResourceLifecycleManager {

    private DeploymentManager deployments;
    private ServerSocket randomSocket;

    @Override
    public void inject(TestInjector testInjector) {
        testInjector.injectIntoFields(deployments, new TestInjector.MatchesType(DeploymentManager.class));
    }

    @Override
    public Map<String, String> start() {
        deployments = DeploymentManager.newInstance();
        deployments.getKafkaContainer();
        String externalBootstrap = deployments.getExternalBootstrapServers();

        try {
            randomSocket = new ServerSocket(0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        URI randomBootstrapServers = URI.create("dummy://localhost:" + randomSocket.getLocalPort());

        return Map.ofEntries(
                Map.entry(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, externalBootstrap),
                Map.entry("console.test.external-bootstrap", externalBootstrap),
                Map.entry("console.test.random-bootstrap", randomBootstrapServers.toString()));
    }

    @Override
    public void stop() {
        deployments.shutdown();

        if (randomSocket != null) {
            try {
                randomSocket.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
