package com.github.eyefloaters.console.kafka.systemtest.deployment;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import org.testcontainers.containers.GenericContainer;

import com.github.eyefloaters.console.legacy.KafkaAdminConfigRetriever;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class KafkaOAuthSecuredResourceManager implements QuarkusTestResourceLifecycleManager {

    Map<String, String> initArgs;
    DeploymentManager deployments;
    GenericContainer<?> keycloakContainer;
    KafkaContainer kafkaContainer;

    @Override
    public void init(Map<String, String> initArgs) {
        this.initArgs = Map.copyOf(initArgs);
    }

    @Override
    public Map<String, String> start() {
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        ThreadPoolExecutor exec = new ThreadPoolExecutor(2, 2, 5, TimeUnit.SECONDS, new ArrayBlockingQueue<>(2), threadFactory);
        deployments = DeploymentManager.newInstance(true);

        CompletableFuture.allOf(
                CompletableFuture.supplyAsync(() -> deployments.getKafkaContainer(), exec)
                    .thenAccept(container -> kafkaContainer = container),
                CompletableFuture.supplyAsync(() -> deployments.getKeycloakContainer(), exec)
                    .thenAccept(container -> keycloakContainer = container))
            .join();

        String externalBootstrap = deployments.getExternalBootstrapServers();

        int kcPort = keycloakContainer.getMappedPort(8080);
        String profile = "%" + initArgs.get("profile") + ".";

        return Map.of(profile + KafkaAdminConfigRetriever.BOOTSTRAP_SERVERS, externalBootstrap,
                      profile + KafkaAdminConfigRetriever.OAUTH_JWKS_ENDPOINT_URI, String.format("http://localhost:%d/realms/kafka-authz/protocol/openid-connect/certs", kcPort),
                      profile + KafkaAdminConfigRetriever.OAUTH_TOKEN_ENDPOINT_URI, String.format("http://localhost:%d/realms/kafka-authz/protocol/openid-connect/token", kcPort),
                      profile + KafkaAdminConfigRetriever.BROKER_TLS_ENABLED, "true",
                      profile + KafkaAdminConfigRetriever.BROKER_TRUSTED_CERT, Base64.getEncoder().encodeToString(kafkaContainer.getCACertificate().getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public void stop() {
        deployments.shutdown();
    }
}
