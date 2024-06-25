package com.github.streamshub.console;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;

import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.quarkus.test.junit.QuarkusTest;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//@QuarkusTestResource(KubernetesServerTestResource.class)
@QuarkusTest
class ConsoleReconcilerTest {

    private static final Logger LOGGER = Logger.getLogger(ConsoleReconcilerTest.class);

    @Inject
    KubernetesClient client;

    @Inject
    Operator operator;

    @BeforeEach
    void setUp() throws Exception {
        operator.start();
    }

    @Test
    void testBasicConsoleReconciliation() {
        client.resource(Crds.kafka()).create();

        client.resource(new NamespaceBuilder()
                .withNewMetadata()
                    .withName("ns1")
                .endMetadata()
                .build())
            .create();

        Kafka kafkaCR = new KafkaBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("kafka-1")
                        .withNamespace("ns1")
                        .build())
                .withNewSpec()
                    .withNewKafka()
                        .addNewListener()
                            .withName("listener1")
                            .withType(KafkaListenerType.INGRESS)
                            .withPort(9093)
                            .withTls(true)
                        .endListener()
                    .endKafka()
                .endSpec()
                .build();

        client.resource(kafkaCR).create();
        client.resource(new NamespaceBuilder()
                .withNewMetadata()
                    .withName("ns2")
                .endMetadata()
                .build())
            .create();

        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("console-1")
                        .withNamespace("ns2")
                        .build())
                .withNewSpec()
                    .withHostname("example.com")
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        await().ignoreException(NullPointerException.class).atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            var console = client.resources(Console.class)
                    .inNamespace(consoleCR.getMetadata().getNamespace())
                    .withName(consoleCR.getMetadata().getName())
                    .get();
            assertEquals(1, console.getStatus().getConditions().size());
            var condition = console.getStatus().getConditions().get(0);
            assertEquals("Ready", condition.getType());
            assertEquals("False", condition.getStatus());
            assertEquals("DependentsNotReady", condition.getReason());
            assertTrue(condition.getMessage().contains("ConsoleIngress"));
            assertTrue(condition.getMessage().contains("PrometheusDeployment"));
        });

        var promDeployment = client.apps().deployments()
            .inNamespace(consoleCR.getMetadata().getNamespace())
            .withName("console-1-prometheus-deployment")
            .get();

        promDeployment = promDeployment.edit()
                .editOrNewStatus()
                    .withReplicas(1)
                    .withReadyReplicas(1)
                .endStatus()
                .build();

        client.resource(promDeployment).patchStatus();
        LOGGER.info("Set ready replicas for Prometheus deployment");

        var consoleIngress = client.network().v1().ingresses()
            .inNamespace(consoleCR.getMetadata().getNamespace())
            .withName("console-1-console-ingress")
            .get();

        consoleIngress = consoleIngress.edit()
                    .editOrNewStatus()
                        .withNewLoadBalancer()
                            .addNewIngress()
                                .withHostname("ingress.example.com")
                            .endIngress()
                        .endLoadBalancer()
                    .endStatus()
                    .build();
        client.resource(consoleIngress).patchStatus();
        LOGGER.info("Set ingress status for Console ingress");

        await().ignoreException(NullPointerException.class).atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            var console = client.resources(Console.class)
                    .inNamespace(consoleCR.getMetadata().getNamespace())
                    .withName(consoleCR.getMetadata().getName())
                    .get();
            assertEquals(1, console.getStatus().getConditions().size());
            var condition = console.getStatus().getConditions().get(0);
            assertEquals("Ready", condition.getType());
            assertEquals("False", condition.getStatus());
            assertEquals("DependentsNotReady", condition.getReason());
            assertTrue(condition.getMessage().contains("ConsoleDeployment"));
        });

        var consoleDeployment = client.apps().deployments()
                .inNamespace(consoleCR.getMetadata().getNamespace())
                .withName("console-1-console-deployment")
                .get();

        consoleDeployment = consoleDeployment.edit()
                .editOrNewStatus()
                    .withReplicas(1)
                    .withReadyReplicas(1)
                .endStatus()
                .build();

        client.resource(consoleDeployment).patchStatus();

        await().ignoreException(NullPointerException.class).atMost(2, TimeUnit.SECONDS).untilAsserted(() -> {
            var console = client.resources(Console.class)
                    .inNamespace(consoleCR.getMetadata().getNamespace())
                    .withName(consoleCR.getMetadata().getName())
                    .get();
            assertEquals(1, console.getStatus().getConditions().size());
            var condition = console.getStatus().getConditions().get(0);
            assertEquals("Ready", condition.getType());
            assertEquals("True", condition.getStatus());
            assertNull(condition.getReason());
            assertEquals("All resources ready", condition.getMessage());
        });
    }
}
