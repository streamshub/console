package com.github.streamshub.console;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.status.Condition;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.dependents.ConsoleResource;
import com.github.streamshub.console.dependents.ConsoleSecret;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationScramSha512;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;
import io.strimzi.api.kafka.model.user.KafkaUser;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

abstract class ConsoleReconcilerTestBase {

    private static final Logger LOGGER = Logger.getLogger(ConsoleReconcilerTestBase.class);

    protected static final Duration LIMIT = Duration.ofSeconds(10);
    protected static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    protected static final String KAFKA_NS = "ns1";
    protected static final String KAFKA_NAME = "kafka-1";

    protected static final String CONSOLE_NS = "ns2";
    protected static final String CONSOLE_NAME = "console-1";

    @Inject
    KubernetesClient client;

    @Inject
    Config config;

    @Inject
    Operator operator;

    Kafka kafkaCR;

    public static <T extends HasMetadata> T apply(KubernetesClient client, T resource) {
        client.resource(resource).serverSideApply();
        return client.resource(resource).patchStatus();
    }

    @BeforeEach
    void setUp() {
        client.resource(Crds.kafka()).serverSideApply();
        client.resource(Crds.kafkaUser()).serverSideApply();
        client.resource(new CustomResourceDefinitionBuilder()
                .withNewMetadata()
                .withName("routes.route.openshift.io")
            .endMetadata()
            .withNewSpec()
                .withScope("Namespaced")
                .withGroup("route.openshift.io")
                .addNewVersion()
                    .withName("v1")
                    .withNewSubresources()
                        .withNewStatus()
                        .endStatus()
                    .endSubresources()
                    .withNewSchema()
                        .withNewOpenAPIV3Schema()
                            .withType("object")
                            .withXKubernetesPreserveUnknownFields(true)
                        .endOpenAPIV3Schema()
                    .endSchema()
                    .withStorage(true)
                    .withServed(true)
                .endVersion()
                .withNewNames()
                    .withSingular("route")
                    .withPlural("routes")
                    .withKind("Route")
                .endNames()
            .endSpec()
            .build())
            .serverSideApply();

        var allConsoles = client.resources(Console.class).inAnyNamespace();
        var allKafkas = client.resources(Kafka.class).inAnyNamespace();
        var allKafkaUsers = client.resources(KafkaUser.class).inAnyNamespace();
        var allDeployments = client.resources(Deployment.class).inAnyNamespace().withLabels(ConsoleResource.MANAGEMENT_LABEL);
        var allConfigMaps = client.resources(ConfigMap.class).inAnyNamespace().withLabels(ConsoleResource.MANAGEMENT_LABEL);
        var allSecrets = client.resources(Secret.class).inAnyNamespace().withLabels(ConsoleResource.MANAGEMENT_LABEL);
        var allIngresses = client.resources(Ingress.class).inAnyNamespace().withLabels(ConsoleResource.MANAGEMENT_LABEL);

        allConsoles.delete();
        allKafkas.delete();
        allKafkaUsers.delete();
        allDeployments.delete();
        allConfigMaps.delete();
        allSecrets.delete();
        allIngresses.delete();

        await().atMost(LIMIT).untilAsserted(() -> {
            assertTrue(allConsoles.list().getItems().isEmpty());
            assertTrue(allKafkas.list().getItems().isEmpty());
            assertTrue(allKafkaUsers.list().getItems().isEmpty());
            assertTrue(allDeployments.list().getItems().isEmpty());
            assertTrue(allConfigMaps.list().getItems().isEmpty());
            assertTrue(allSecrets.list().getItems().isEmpty());
            assertTrue(allIngresses.list().getItems().isEmpty());
        });

        operator.start();

        client.resource(new NamespaceBuilder()
                .withNewMetadata()
                    .withName(KAFKA_NS)
                    .withLabels(Map.of("streamshub-operator/test", "true"))
                .endMetadata()
                .build())
            .serverSideApply();

        kafkaCR = new KafkaBuilder()
                .withNewMetadata()
                    .withName(KAFKA_NAME)
                    .withNamespace(KAFKA_NS)
                .endMetadata()
                .withNewSpec()
                    .withNewKafka()
                        .addNewListener()
                            .withName("listener1")
                            .withType(KafkaListenerType.INGRESS)
                            .withPort(9093)
                            .withTls(true)
                            .withAuth(new KafkaListenerAuthenticationScramSha512())
                        .endListener()
                    .endKafka()
                .endSpec()
                .withNewStatus()
                    .withClusterId(UUID.randomUUID().toString())
                    .addNewListener()
                        .withName("listener1")
                        .addNewAddress()
                            .withHost("kafka-bootstrap.example.com")
                            .withPort(9093)
                        .endAddress()
                    .endListener()
                .endStatus()
                .build();

        kafkaCR = apply(client, kafkaCR);

        client.resource(new NamespaceBuilder()
                .withNewMetadata()
                    .withName(CONSOLE_NS)
                    .withLabels(Map.of("streamshub-operator/test", "true"))
                .endMetadata()
                .build())
            .serverSideApply();
    }

    Console createConsole(ConsoleBuilder builder) {
        var meta = new ObjectMetaBuilder(builder.getMetadata())
                .withNamespace(CONSOLE_NS)
                .withName(CONSOLE_NAME)
                .build();

        builder = builder.withMetadata(meta);

        return client.resource(builder.build()).create();
    }

    void awaitReady(Console resource) {
        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var console = client.resources(Console.class)
                    .inNamespace(resource.getMetadata().getNamespace())
                    .withName(resource.getMetadata().getName())
                    .get();

            assertEquals(1, console.getStatus().getConditions().size());
            var condition = console.getStatus().getConditions().iterator().next();

            assertEquals(Condition.Types.READY, condition.getType(), condition::toString);
            assertEquals("True", condition.getStatus(), condition::toString);
            assertNull(condition.getReason());
            assertEquals("All resources ready", condition.getMessage(), condition::toString);
        });
    }

    void awaitDependentsNotReady(Console resource, String... dependents) {
        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var console = client.resources(Console.class)
                    .inNamespace(resource.getMetadata().getNamespace())
                    .withName(resource.getMetadata().getName())
                    .get();

            assertEquals(1, console.getStatus().getConditions().size());
            var condition = console.getStatus().getConditions().iterator().next();

            assertEquals(Condition.Types.READY, condition.getType(), condition::toString);
            assertEquals("False", condition.getStatus(), condition::toString);
            assertEquals(Condition.Reasons.DEPENDENTS_NOT_READY, condition.getReason(), condition::toString);

            for (String dependent : dependents) {
                assertTrue(condition.getMessage().contains(dependent));
            }
        });
    }

    void assertInvalidConfiguration(Console resource, Consumer<List<Condition>> assertion) {
        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var console = client.resources(Console.class)
                    .inNamespace(resource.getMetadata().getNamespace())
                    .withName(resource.getMetadata().getName())
                    .get();

            var conditions = console.getStatus().getConditions();
            assertTrue(conditions.size() > 1);

            var readyCondition = conditions.iterator().next();
            assertEquals(Condition.Types.READY, readyCondition.getType(), readyCondition::toString);
            assertEquals("False", readyCondition.getStatus(), readyCondition::toString);
            assertEquals(Condition.Reasons.INVALID_CONFIGURATION, readyCondition.getReason(), readyCondition::toString);

            // Ready is always sorted as the first condition for ease of reference
            List<Condition> errors = List.copyOf(conditions).subList(1, conditions.size());

            assertion.accept(errors);
        });
    }

    void assertConsoleConfig(Consumer<ConsoleConfig> assertion) {
        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var consoleSecret = client.secrets()
                    .inNamespace(CONSOLE_NS)
                    .withName(CONSOLE_NAME + "-" + ConsoleSecret.NAME)
                    .get();

            assertNotNull(consoleSecret);

            String configEncoded = consoleSecret.getData().get("console-config.yaml");
            byte[] configDecoded = Base64.getDecoder().decode(configEncoded);

            LOGGER.debugf("config YAML: %s", new String(configDecoded));

            ConsoleConfig consoleConfig = YAML.readValue(configDecoded, ConsoleConfig.class);
            assertion.accept(consoleConfig);
        });
    }

    void setConsoleIngressReady(Console consoleCR) {
        var consoleIngress = client.network().v1().ingresses()
                .inNamespace(consoleCR.getMetadata().getNamespace())
                .withName("%s-console-ingress".formatted(consoleCR.getMetadata().getName()))
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
    }

    Deployment setDeploymentReady(Console consoleCR, String deploymentName) {
        var deployment = client.apps().deployments()
            .inNamespace(consoleCR.getMetadata().getNamespace())
            .withName("%s-%s".formatted(consoleCR.getMetadata().getName(), deploymentName))
            .editStatus(this::setReady);
        LOGGER.infof("Set ready replicas for deployment: %s", deploymentName);
        return deployment;
    }

    Deployment setReady(Deployment deployment) {
        int desiredReplicas = Optional.ofNullable(deployment.getSpec().getReplicas()).orElse(1);

        return deployment.edit()
            .editOrNewStatus()
                .withReplicas(desiredReplicas)
                .withUpdatedReplicas(desiredReplicas)
                .withAvailableReplicas(desiredReplicas)
                .withReadyReplicas(desiredReplicas)
            .endStatus()
            .build();
    }
}
