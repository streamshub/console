package com.github.streamshub.console.kafka.systemtest.deployment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

import com.github.streamshub.console.config.TrustStoreConfig;
import com.github.streamshub.console.test.TlsHelper;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import static java.util.concurrent.CompletableFuture.supplyAsync;

public class ApicurioResourceManager extends ResourceManagerBase implements QuarkusTestResourceLifecycleManager {

    GenericContainer<?> apicurio2;
    GenericContainer<?> apicurio3;

    @Override
    public Map<String, String> start(Map<Class<?>, Map<String, String>> dependencyProperties) {
        return supplyAsync(() -> startV2(dependencyProperties))
            .thenCombineAsync(supplyAsync(() -> startV3(dependencyProperties)), (propsV2, propsV3) -> {
                Map<String, String> props = new HashMap<>();
                props.putAll(propsV2);
                props.putAll(propsV3);
                return props;
            })
            .join();
    }

    @SuppressWarnings("resource")
    private Map<String, String> startV2(Map<Class<?>, Map<String, String>> dependencyProperties) {
        int port = 8443;
        TlsHelper tls = TlsHelper.newInstance();
        String keystorePath = "/opt/apicurio/keystore.p12";
        String truststorePath = "/opt/apicurio/keycloak-truststore.jks";
        String apicurioImage;

        try (InputStream in = getClass().getResourceAsStream("/Dockerfile.apicurio2");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            apicurioImage = reader.readLine().substring("FROM ".length());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        var keycloakProperties = dependencyProperties.get(KeycloakResourceManager.class);
        var authServerUrl = keycloakProperties.get("console.test.oidc-url-internal");
        byte[] keycloakTruststore;

        try {
            keycloakTruststore = Files.readAllBytes(Path.of(keycloakProperties.get("console.test.oidc-trust-store.path")));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        apicurio2 = new GenericContainer<>(apicurioImage)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("systemtests.apicurio2"), true))
                .withCreateContainerCmdModifier(setContainerName("apicurio-registry"))
                .withNetwork(SHARED_NETWORK)
                .withExposedPorts(port)
                .withEnv(Map.of(
                        "REGISTRY_APIS_V2_DATE_FORMAT", "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                        "QUARKUS_TLS_KEY_STORE_P12_PATH", keystorePath,
                        "QUARKUS_TLS_KEY_STORE_P12_PASSWORD", String.copyValueOf(tls.getPassphrase()),
                        "QUARKUS_HTTP_INSECURE_REQUESTS", "disabled",
                        /*
                         * See https://www.apicur.io/registry/docs/apicurio-registry/2.6.x/getting-started/assembly-configuring-registry-security.html#registry-security-keycloak_registry
                         * Note, these variables will change with Apicurio Registry 3.x
                         */
                        "AUTH_ENABLED", "true",
                        "KEYCLOAK_URL", authServerUrl,
                        "KEYCLOAK_REALM", keycloakProperties.get("console.test.oidc-realm"),
                        "KEYCLOAK_API_CLIENT_ID", "registry-api",
                        "QUARKUS_OIDC_TLS_TRUST_STORE_FILE", truststorePath,
                        "QUARKUS_OIDC_TLS_TRUST_STORE_PASSWORD", keycloakProperties.get("console.test.oidc-trust-store.password")
                ))
                .withCopyToContainer(
                        Transferable.of(tls.getKeyStoreBytes()),
                        keystorePath)
                .withCopyToContainer(
                        Transferable.of(keycloakTruststore),
                        truststorePath)
                .waitingFor(Wait.forListeningPort());

        File truststoreFile;

        try {
            truststoreFile = File.createTempFile("schema-registry-trust", "." + tls.getTrustStore().getType());
            Files.write(truststoreFile.toPath(), tls.getTrustStoreBytes());
            truststoreFile.deleteOnExit();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        File truststorePassword;

        try {
            truststorePassword = File.createTempFile("schema-registry-trust-password", ".txt");
            Files.writeString(truststorePassword.toPath(), String.copyValueOf(tls.getPassphrase()));
            truststorePassword.deleteOnExit();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        apicurio2.start();

        String urlTemplate = "https://localhost:%d/apis/registry/v2/";
        var apicurioUrl = urlTemplate.formatted(apicurio2.getMappedPort(port));
        return Map.of(
                "console.test.apicurio2-url", apicurioUrl,
                "console.test.apicurio2-trust-store.type", TrustStoreConfig.Type.JKS.name(),
                "console.test.apicurio2-trust-store.path", truststoreFile.getAbsolutePath(),
                "console.test.apicurio2-trust-store.password-path", truststorePassword.getAbsolutePath()
        );
    }

    @SuppressWarnings("resource")
    private Map<String, String> startV3(Map<Class<?>, Map<String, String>> dependencyProperties) {
        int port = 8443;
        TlsHelper tls = TlsHelper.newInstance();
        String keystorePath = "/opt/apicurio/keystore.p12";
        String truststorePath = "/opt/apicurio/keycloak-truststore.jks";
        String apicurioImage;

        try (InputStream in = getClass().getResourceAsStream("/Dockerfile.apicurio3");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            apicurioImage = reader.readLine().substring("FROM ".length());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        var keycloakProperties = dependencyProperties.get(KeycloakResourceManager.class);
        var authServerUrl = keycloakProperties.get("console.test.oidc-url-internal");
        byte[] keycloakTruststore;

        try {
            keycloakTruststore = Files.readAllBytes(Path.of(keycloakProperties.get("console.test.oidc-trust-store.path")));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        apicurio3 = new GenericContainer<>(apicurioImage)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("systemtests.apicurio3"), true))
                .withCreateContainerCmdModifier(setContainerName("apicurio-registry"))
                .withNetwork(SHARED_NETWORK)
                .withExposedPorts(port)
                .withEnv(Map.of(
                        "QUARKUS_TLS_KEY_STORE_P12_PATH", keystorePath,
                        "QUARKUS_TLS_KEY_STORE_P12_PASSWORD", String.copyValueOf(tls.getPassphrase()),
                        "QUARKUS_HTTP_INSECURE_REQUESTS", "disabled",
                        "QUARKUS_OIDC_TENANT_ENABLED", "true",
                        "QUARKUS_OIDC_AUTH_SERVER_URL", "%s/realms/%s".formatted(authServerUrl, keycloakProperties.get("console.test.oidc-realm")),
                        "QUARKUS_OIDC_CLIENT_ID", "registry-api",
                        "QUARKUS_OIDC_TLS_TRUST_STORE_FILE", truststorePath,
                        "QUARKUS_OIDC_TLS_TRUST_STORE_PASSWORD", keycloakProperties.get("console.test.oidc-trust-store.password")
                ))
                .withCopyToContainer(
                        Transferable.of(tls.getKeyStoreBytes()),
                        keystorePath)
                .withCopyToContainer(
                        Transferable.of(keycloakTruststore),
                        truststorePath)
                .waitingFor(Wait.forListeningPort());

        File truststoreFile;

        try {
            truststoreFile = File.createTempFile("schema-registry-trust", "." + tls.getTrustStore().getType());
            Files.write(truststoreFile.toPath(), tls.getTrustStoreBytes());
            truststoreFile.deleteOnExit();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        File truststorePassword;

        try {
            truststorePassword = File.createTempFile("schema-registry-trust-password", ".txt");
            Files.writeString(truststorePassword.toPath(), String.copyValueOf(tls.getPassphrase()));
            truststorePassword.deleteOnExit();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        apicurio3.start();

        String urlTemplate = "https://localhost:%d/apis/registry/v3";
        var apicurioUrl = urlTemplate.formatted(apicurio3.getMappedPort(port));
        return Map.of(
                "console.test.apicurio3-url", apicurioUrl,
                "console.test.apicurio3-trust-store.type", TrustStoreConfig.Type.JKS.name(),
                "console.test.apicurio3-trust-store.path", truststoreFile.getAbsolutePath(),
                "console.test.apicurio3-trust-store.password-path", truststorePassword.getAbsolutePath()
        );
    }

    @Override
    public void stop() {
        if (apicurio2 != null) {
            apicurio2.stop();
        }
        if (apicurio3 != null) {
            apicurio3.stop();
        }
    }
}
