package com.github.streamshub.console.kafka.systemtest.deployment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.Map;

import org.apache.kafka.clients.CommonClientConfigs;
import org.jboss.logging.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.test.container.StrimziKafkaCluster;
import io.strimzi.test.container.StrimziKafkaCluster.StrimziKafkaClusterBuilder;

public class KafkaResourceManager extends ResourceManagerBase implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = Logger.getLogger(KafkaResourceManager.class);

    private StrimziKafkaCluster kafkaCluster;
    private ServerSocket randomSocket;

    @Override
    public Map<String, String> start(Map<Class<?>, Map<String, String>> dependencyProperties) {
        kafkaCluster = deployStrimziKafka();
        String externalBootstrap = kafkaCluster.getBootstrapServers();
        String profile = "%" + initArgs.get("profile") + ".";

        try {
            randomSocket = new ServerSocket(0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        URI randomBootstrapServers = URI.create("dummy://localhost:" + randomSocket.getLocalPort());

        return Map.ofEntries(
                Map.entry(profile + CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, externalBootstrap),
                Map.entry(profile + "console.test.external-bootstrap", externalBootstrap),
                Map.entry(profile + "console.test.random-bootstrap", randomBootstrapServers.toString()));
    }

    @Override
    public void stop() {
        if (kafkaCluster != null) {
            kafkaCluster.stop();
        }

        if (randomSocket != null) {
            try {
                randomSocket.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }

    private StrimziKafkaCluster deployStrimziKafka() {
        String kafkaImage;

        try (InputStream in = getClass().getResourceAsStream("/Dockerfile.kafka");
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            kafkaImage = reader.readLine().substring("FROM ".length());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String kafkaVersion = kafkaImage.substring(kafkaImage.lastIndexOf('-') + 1);

        LOGGER.infof("Deploying Strimzi Kafka cluster: %s", kafkaVersion);

        StrimziKafkaCluster cluster = new StrimziKafkaClusterBuilder()
                .withKafkaVersion(kafkaVersion)
                .withNumberOfBrokers(1)
                .withAdditionalKafkaConfiguration(Map.of(
                    "auto.create.topics.enable", "false",
                    "group.initial.rebalance.delay.ms", "0",
                    "group.coordinator.new.enable", "false"
                ))
                .build();

        cluster.getNodes().forEach(node -> {
            node.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("systemtests.kafka"), true))
                .withCreateContainerCmdModifier(setContainerName("kafka"))
                .withNetwork(SHARED_NETWORK);
        });

        cluster.start();
        return cluster;
    }
}
