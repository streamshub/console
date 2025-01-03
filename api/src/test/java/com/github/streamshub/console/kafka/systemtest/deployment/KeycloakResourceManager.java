package com.github.streamshub.console.kafka.systemtest.deployment;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

import com.github.streamshub.console.test.TlsHelper;

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

        int port = 8443;
        TlsHelper tls = TlsHelper.newInstance();
        String keystorePath = "/opt/keycloak/keystore.p12";

        keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:26.0")
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("systemtests.keycloak"), true))
                .withExposedPorts(port)
                .withEnv(Map.of(
                        "KC_BOOTSTRAP_ADMIN_USERNAME", "admin",
                        "KC_BOOTSTRAP_ADMIN_PASSWORD", "admin",
                        "PROXY_ADDRESS_FORWARDING", "true"))
                .withCopyToContainer(
                        Transferable.of(tls.getKeyStoreBytes()),
                        keystorePath)
                .withCopyToContainer(
                        Transferable.of(realmConfig),
                        "/opt/keycloak/data/import/console-realm.json")
                .withCommand(
                        "start",
                        "--hostname=localhost",
                        "--http-enabled=false",
                        "--https-key-store-file=%s".formatted(keystorePath),
                        "--https-key-store-password=%s".formatted(String.copyValueOf(tls.getPassphrase())),
                        "--import-realm"
                )
                .waitingFor(Wait.forHttps("/realms/console-authz")
                        .allowInsecure()
                        .withStartupTimeout(Duration.ofMinutes(1)));

        File truststoreFile;

        try {
            truststoreFile = File.createTempFile("oidc-provider-trust", "." + tls.getTrustStore().getType());
            //truststoreFile.deleteOnExit();
            Files.write(truststoreFile.toPath(), tls.getTrustStoreBytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        keycloak.start();

        String urlTemplate = "https://localhost:%d/realms/console-authz";
        var oidcUrl = urlTemplate.formatted(keycloak.getMappedPort(port));
        return Map.of(
                "console.test.oidc-url", oidcUrl,
                "console.test.oidc-host", "localhost:%d".formatted(port),
                "console.test.oidc-issuer", urlTemplate.formatted(port),
                "quarkus.tls.\"oidc-provider-trust\".trust-store.jks.path", truststoreFile.getAbsolutePath(),
                "quarkus.tls.\"oidc-provider-trust\".trust-store.jks.password", String.copyValueOf(tls.getPassphrase())
        );
    }

    @Override
    public void stop() {
        keycloak.stop();
    }
}
