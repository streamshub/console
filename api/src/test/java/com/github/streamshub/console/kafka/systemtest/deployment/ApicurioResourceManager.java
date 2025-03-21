package com.github.streamshub.console.kafka.systemtest.deployment;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.Transferable;

import com.github.streamshub.console.config.TrustStoreConfig;
import com.github.streamshub.console.test.TlsHelper;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class ApicurioResourceManager implements QuarkusTestResourceLifecycleManager {

    GenericContainer<?> apicurio;

    @Override
    @SuppressWarnings("resource")
    public Map<String, String> start() {
        int port = 8443;
        TlsHelper tls = TlsHelper.newInstance();
        String keystorePath = "/opt/apicurio/keystore.p12";
        String apicurioImage;

        try (InputStream in = getClass().getResourceAsStream("/Dockerfile.apicurio");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            apicurioImage = reader.readLine().substring("FROM ".length());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        apicurio = new GenericContainer<>(apicurioImage)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("systemtests.apicurio"), true))
                .withExposedPorts(port)
                .withEnv(Map.of(
                        "QUARKUS_TLS_KEY_STORE_P12_PATH", keystorePath,
                        "QUARKUS_TLS_KEY_STORE_P12_PASSWORD", String.copyValueOf(tls.getPassphrase()),
                        "QUARKUS_HTTP_INSECURE_REQUESTS", "disabled"))
                .withCopyToContainer(
                        Transferable.of(tls.getKeyStoreBytes()),
                        keystorePath)
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

        apicurio.start();

        String urlTemplate = "https://localhost:%d/apis/registry/v2/";
        var apicurioUrl = urlTemplate.formatted(apicurio.getMappedPort(port));
        return Map.of(
                "console.test.apicurio-url", apicurioUrl,
                "console.test.apicurio-trust-store.type", TrustStoreConfig.Type.JKS.name(),
                "console.test.apicurio-trust-store.path", truststoreFile.getAbsolutePath(),
                "console.test.apicurio-trust-store.password-path", truststorePassword.getAbsolutePath()
        );
    }

    @Override
    public void stop() {
        apicurio.stop();
    }
}
