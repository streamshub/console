package io.streamshub.console.api.ui;

import io.streamshub.console.api.service.KafkaClusterService;
import io.streamshub.console.api.support.KafkaContext;
import io.streamshub.console.config.ConsoleConfig;
import io.streamshub.console.config.KafkaClusterConfig;
import io.streamshub.console.test.TestHelper;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import jakarta.inject.Inject;
import org.apache.kafka.clients.CommonClientConfigs;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import static org.awaitility.Awaitility.await;

abstract class BaseKafkaIT extends UITestBase  {

    static final String KAFKA_NAME = "test-kafka1";
    static final String KAFKA_KEY  = "default/" + KAFKA_NAME;

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    Map<String, KafkaContext> configuredContexts;

    @Inject
    KafkaClusterService kafkaClusterService;

    TestHelper utils;
    URI bootstrapServers;
    String clusterId;

    @BeforeEach
    void baseSetup() {
        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        utils = new TestHelper(bootstrapServers, config);
        utils.resetSecurity(consoleConfig, false);
        kafkaClusterService.setListUnconfigured(false);

        clusterId = consoleConfig.getKafka().getCluster(KAFKA_KEY).orElseThrow().getId(); // "k1-id"

        // Start every test from a clean slate. Each test applies the Kafka CR it needs.
        deleteStrimziResources();
    }

    @AfterEach
    void baseCleanup() {
        deleteStrimziResources();
    }

    /** Override to delete more resource types, and call {@code super} to keep the Kafka cleanup. */
    protected void deleteStrimziResources() {
        client.resources(Kafka.class).inAnyNamespace().delete();
    }

    protected Kafka applyKafka() {
        return applyKafka(UnaryOperator.identity());
    }

    /** Applies the "Ready" test-kafka1 CR; the customizer can adjust the builder (e.g. add status). */
    protected Kafka applyKafka(UnaryOperator<KafkaBuilder> customizer) {
        KafkaBuilder builder = new KafkaBuilder(
                utils.buildKafkaResource(KAFKA_NAME, clusterId, bootstrapServers))
                .editOrNewStatus()
                    .addNewCondition()
                        .withType("Ready")
                        .withStatus("True")
                    .endCondition()
                .endStatus();

        Kafka kafka = utils.apply(client, customizer.apply(builder).build());

        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> configuredContexts.values().stream()
                        .map(KafkaContext::clusterConfig)
                        .map(KafkaClusterConfig::clusterKey)
                        .anyMatch(Cache.metaNamespaceKeyFunc(kafka)::equals));

        return kafka;
    }
}
