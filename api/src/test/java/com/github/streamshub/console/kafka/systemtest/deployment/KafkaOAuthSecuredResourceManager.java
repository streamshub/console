package com.github.streamshub.console.kafka.systemtest.deployment;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KafkaOAuthSecuredResourceManager extends KafkaResourceManager implements QuarkusTestResourceLifecycleManager {

    GenericContainer<?> keycloakContainer;

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

        return Map.of(profile + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, externalBootstrap,
                      profile + "quarkus.oidc.auth-server-url", String.format("http://localhost:%d/realms/kafka-authz", kcPort),
                      profile + SaslConfigs.SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL, String.format("http://localhost:%d/realms/kafka-authz/protocol/openid-connect/token", kcPort),
                      profile + SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, Base64.getEncoder().encodeToString(kafkaContainer.getCACertificate().getBytes(StandardCharsets.UTF_8)));
    }

}
