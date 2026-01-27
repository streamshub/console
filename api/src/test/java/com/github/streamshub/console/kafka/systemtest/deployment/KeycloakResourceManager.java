package com.github.streamshub.console.kafka.systemtest.deployment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

import com.github.streamshub.console.config.TrustStoreConfig;
import com.github.streamshub.console.test.TlsHelper;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class KeycloakResourceManager extends ResourceManagerBase implements QuarkusTestResourceLifecycleManager {

    GenericContainer<?> keycloak;

    @Override
    @SuppressWarnings("resource")
    public Map<String, String> start(Map<Class<?>, Map<String, String>> dependencyProperties) {
        byte[] realmConfig;

        try (InputStream stream = getClass().getResourceAsStream("/keycloak/console-realm.json")) {
            realmConfig = stream.readAllBytes();
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }

        String alias = "keycloak";
        int httpPort = 8080;
        int httpsPort = 8443;
        TlsHelper tls = TlsHelper.newInstance(alias);
        String keystorePath = "/opt/keycloak/keystore.p12";
        String keycloakImage;

        try (InputStream in = getClass().getResourceAsStream("/Dockerfile.keycloak");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            keycloakImage = reader.readLine().substring("FROM ".length());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        keycloak = new GenericContainer<>(keycloakImage)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("systemtests.keycloak"), true))
                .withCreateContainerCmdModifier(setContainerName("keycloak"))
                .withNetwork(SHARED_NETWORK)
                .withNetworkAliases(alias)
                .withExposedPorts(httpPort, httpsPort)
                .withEnv(Map.of(
                        "KC_BOOTSTRAP_ADMIN_USERNAME", "admin",
                        "KC_BOOTSTRAP_ADMIN_PASSWORD", "admin",
                        "PROXY_ADDRESS_FORWARDING", "true"))
                .withCopyToContainer(
                        Transferable.of(tls.getKeyStoreBytes()),
                        keystorePath)
                .withCopyToContainer(
                        Transferable.of(realmConfig),
                        /*
                         * File name must match realm name:
                         * https://github.com/keycloak/keycloak/issues/33637#issuecomment-2651374165
                         */
                        "/opt/keycloak/data/import/console-authz.json")
                .withCommand(
                        "start",
                        "--hostname=https://localhost:%d".formatted(httpsPort),
                        "--hostname-backchannel-dynamic=true",
                        "--http-enabled=true",
                        "--https-port=%d".formatted(httpsPort),
                        "--https-key-store-file=%s".formatted(keystorePath),
                        "--https-key-store-password=%s".formatted(String.copyValueOf(tls.getPassphrase())),
                        "--import-realm",
                        "--verbose"
                )
                .waitingFor(Wait.forHttps("/realms/console-authz")
                        .forPort(httpsPort)
                        .allowInsecure()
                        .withStartupTimeout(Duration.ofMinutes(1)));

        File truststoreFile;

        try {
            truststoreFile = File.createTempFile("oidc-provider-trust", "." + tls.getTrustStore().getType());
            Files.write(truststoreFile.toPath(), tls.getTrustStoreBytes());
            truststoreFile.deleteOnExit();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        keycloak.start();

        String httpsUrlTemplate = "https://localhost:%d/realms/console-authz";
        var httpsOidcUrl = httpsUrlTemplate.formatted(keycloak.getMappedPort(httpsPort));

        return Map.of(
                "console.test.oidc-url-plain", "http://localhost:%d/realms/console-authz".formatted(keycloak.getMappedPort(httpPort)),
                "console.test.oidc-url", httpsOidcUrl,
                "console.test.oidc-url-internal", "https://%s:%d".formatted(alias, httpsPort),
                "console.test.oidc-host", "localhost:%d".formatted(httpsPort),
                "console.test.oidc-issuer", httpsUrlTemplate.formatted(httpsPort),
                "console.test.oidc-realm", "console-authz",
                "console.test.oidc-trust-store.type", TrustStoreConfig.Type.JKS.name(),
                "console.test.oidc-trust-store.path", truststoreFile.getAbsolutePath(),
                "console.test.oidc-trust-store.password", String.copyValueOf(tls.getPassphrase())
        );
    }

    @Override
    public void stop() {
        if (keycloak != null) {
            keycloak.stop();
        }
    }
}
