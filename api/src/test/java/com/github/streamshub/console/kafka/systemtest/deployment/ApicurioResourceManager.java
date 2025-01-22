package com.github.streamshub.console.kafka.systemtest.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class ApicurioResourceManager implements QuarkusTestResourceLifecycleManager {

    GenericContainer<?> apicurio;

    @Override
    @SuppressWarnings("resource")
    public Map<String, String> start() {
        int port = 8080;
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
                .waitingFor(Wait.forListeningPort());

        apicurio.start();

        String urlTemplate = "http://localhost:%d/apis/registry/v2/";
        var apicurioUrl = urlTemplate.formatted(apicurio.getMappedPort(port));
        return Map.of("console.test.apicurio-url", apicurioUrl);
    }

    @Override
    public void stop() {
        apicurio.stop();
    }
}
