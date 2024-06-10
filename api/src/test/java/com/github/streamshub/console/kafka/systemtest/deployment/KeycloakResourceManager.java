package com.github.streamshub.console.kafka.systemtest.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KeycloakResourceManager implements QuarkusTestResourceLifecycleManager {

    GenericContainer<?> keycloak;

    @Override
    @SuppressWarnings("resource")
    public Map<String, String> start() {
        byte[] realmConfig;

        try (InputStream stream = getClass().getResourceAsStream("/keycloak/console-realm.json")) {
            realmConfig = stream.readAllBytes();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }

        keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:26.0")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("systemtests.keycloak"), true))
                .withExposedPorts(8080)
                .withEnv(Map.of(
                        "KC_BOOTSTRAP_ADMIN_USERNAME", "admin",
                        "KC_BOOTSTRAP_ADMIN_PASSWORD", "admin",
                        "PROXY_ADDRESS_FORWARDING", "true"))
                .withCopyToContainer(
                        Transferable.of(realmConfig),
                        "/opt/keycloak/data/import/console-realm.json")
                .withCommand("start", "--hostname=localhost", "--http-enabled=true", "--import-realm")
                .waitingFor(Wait.forHttp("/realms/console-authz").withStartupTimeout(Duration.ofMinutes(1)));

        keycloak.start();

        String urlTemplate = "http://localhost:%d/realms/console-authz";
        var oidcUrl = urlTemplate.formatted(keycloak.getMappedPort(8080));
        return Map.of(
                "console.test.oidc-url", oidcUrl,
                "console.test.oidc-issuer", urlTemplate.formatted(8080));
    }

    @Override
    public void stop() {
        keycloak.stop();
    }
}
