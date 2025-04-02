package com.github.streamshub.console.kafka.systemtest.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startable;

import io.strimzi.test.container.StrimziKafkaContainer;

@SuppressWarnings("resource")
public class DeploymentManager {

    protected static final Logger LOGGER = Logger.getLogger(DeploymentManager.class);
    static final Map<String, String> TEST_CONTAINER_LABELS =
            Collections.singletonMap("test-ident", "systemtest");

    @Retention(RetentionPolicy.RUNTIME)
    public @interface InjectDeploymentManager {
    }

    private final Network testNetwork;

    private StrimziKafkaContainer kafkaContainer;

    public static DeploymentManager newInstance() {
        return new DeploymentManager();
    }

    private DeploymentManager() {
        this.testNetwork = Network.newNetwork();
    }

    private static String name(String prefix) {
        return prefix + '-' + UUID.randomUUID().toString();
    }

    public void shutdown() {
        stopAll(kafkaContainer);
    }

    private void stopAll(Startable... containers) {
        for (var container : containers) {
            if (container != null) {
                container.stop();
            }
        }
    }

    public StrimziKafkaContainer getKafkaContainer() {
        if (kafkaContainer == null) {
            kafkaContainer = deployStrimziKafka();
        }

        return kafkaContainer;
    }

    public void stopKafkaContainer() {
        if (kafkaContainer != null) {
            kafkaContainer.stop();
            kafkaContainer = null;
        }
    }

    public String getExternalBootstrapServers() {
        if (kafkaContainer != null) {
            return this.kafkaContainer.getBootstrapServers();
        }

        return null;
    }

    private StrimziKafkaContainer deployStrimziKafka() {
        String kafkaImage;

        try (InputStream in = getClass().getResourceAsStream("/Dockerfile.kafka");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            kafkaImage = reader.readLine().substring("FROM ".length());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        LOGGER.infof("Deploying Strimzi Kafka container: %s", kafkaImage);

        var container = new StrimziKafkaContainer(kafkaImage)
                .withLabels(TEST_CONTAINER_LABELS)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("systemtests.kafka"), true))
                .withCreateContainerCmdModifier(cmd -> cmd.withName(name("kafka")))
                .withKafkaConfigurationMap(Map.of(
                    "auto.create.topics.enable", "false",
                    "group.initial.rebalance.delay.ms", "0",
                    "group.coordinator.new.enable", "false"
                ))
                .withNetwork(testNetwork);

        container.start();
        return container;
    }
}
