package com.github.streamshub.console;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import org.apache.kafka.common.config.SaslConfigs;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.dependents.ConsoleResource;
import com.github.streamshub.console.dependents.ConsoleSecret;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.Operator;
import io.quarkus.test.junit.QuarkusTest;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationScramSha512;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserBuilder;
import io.strimzi.api.kafka.model.user.KafkaUserScramSha512ClientAuthentication;

import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

//@QuarkusTestResource(KubernetesServerTestResource.class)
@QuarkusTest
class ConsoleReconcilerTest {

    private static final Logger LOGGER = Logger.getLogger(ConsoleReconcilerTest.class);
    private static final Duration LIMIT = Duration.ofSeconds(1_000);

    @Inject
    KubernetesClient client;

    @Inject
    Config config;

    @Inject
    Operator operator;

    Kafka kafkaCR;

    public static <S, T, C extends CustomResource<S, T>> C apply(KubernetesClient client, C resource) {
        client.resource(resource).serverSideApply();
        return client.resource(resource).patchStatus();
    }

    @BeforeEach
    void setUp() throws Exception {
        client.resource(Crds.kafka()).serverSideApply();
        client.resource(Crds.kafkaUser()).serverSideApply();

        var allConsoles = client.resources(Console.class).inAnyNamespace();
        var allKafkas = client.resources(Kafka.class).inAnyNamespace();
        var allKafkaUsers = client.resources(KafkaUser.class).inAnyNamespace();
        var allDeployments = client.resources(Deployment.class).inAnyNamespace().withLabels(ConsoleResource.MANAGEMENT_LABEL);
        var allConfigMaps = client.resources(ConfigMap.class).inAnyNamespace().withLabels(ConsoleResource.MANAGEMENT_LABEL);
        var allSecrets = client.resources(Secret.class).inAnyNamespace().withLabels(ConsoleResource.MANAGEMENT_LABEL);

        allConsoles.delete();
        allKafkas.delete();
        allKafkaUsers.delete();
        allDeployments.delete();
        allConfigMaps.delete();
        allSecrets.delete();

        await().atMost(LIMIT).untilAsserted(() -> {
            assertTrue(allConsoles.list().getItems().isEmpty());
            assertTrue(allKafkas.list().getItems().isEmpty());
            assertTrue(allKafkaUsers.list().getItems().isEmpty());
            assertTrue(allDeployments.list().getItems().isEmpty());
            assertTrue(allConfigMaps.list().getItems().isEmpty());
            assertTrue(allSecrets.list().getItems().isEmpty());
        });

        operator.start();

        client.resource(new NamespaceBuilder()
                .withNewMetadata()
                    .withName("ns1")
                    .withLabels(Map.of("streamshub-operator/test", "true"))
                .endMetadata()
                .build())
            .serverSideApply();

        kafkaCR = new KafkaBuilder()
                .withNewMetadata()
                    .withName("kafka-1")
                    .withNamespace("ns1")
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
                    .withName("ns2")
                    .withLabels(Map.of("streamshub-operator/test", "true"))
                .endMetadata()
                .build())
            .serverSideApply();
    }

    @Test
    void testBasicConsoleReconciliation() {
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

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
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

        client.apps().deployments()
            .inNamespace(consoleCR.getMetadata().getNamespace())
            .withName("console-1-prometheus-deployment")
            .editStatus(this::setReady);
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

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
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
                .editStatus(this::setReady);
        LOGGER.info("Set ready replicas for Console deployment");

        // Images were not set in CR, so assert that the defaults were used
        var consoleContainers = consoleDeployment.getSpec().getTemplate().getSpec().getContainers();
        assertEquals(config.getValue("console.deployment.default-api-image", String.class),
                consoleContainers.get(0).getImage());
        assertEquals(config.getValue("console.deployment.default-ui-image", String.class),
                consoleContainers.get(1).getImage());

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
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

    @Test
    void testConsoleReconciliationWithInvalidListenerName() {
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
                        .withListener("invalid")
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var console = client.resources(Console.class)
                    .inNamespace(consoleCR.getMetadata().getNamespace())
                    .withName(consoleCR.getMetadata().getName())
                    .get();
            assertEquals(2, console.getStatus().getConditions().size());
            var ready = console.getStatus().getConditions().get(0);
            assertEquals("Ready", ready.getType());
            assertEquals("False", ready.getStatus());
            assertEquals("DependentsNotReady", ready.getReason());
            var warning = console.getStatus().getConditions().get(1);
            assertEquals("Warning", warning.getType());
            assertEquals("True", warning.getStatus());
            assertEquals("ReconcileException", warning.getReason());
        });
    }

    @Test
    void testConsoleReconciliationWithMissingKafkaUser() {
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
                        .withNewCredentials()
                            .withNewKafkaUser()
                                .withName("invalid")
                            .endKafkaUser()
                        .endCredentials()
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var console = client.resources(Console.class)
                    .inNamespace(consoleCR.getMetadata().getNamespace())
                    .withName(consoleCR.getMetadata().getName())
                    .get();
            assertEquals(2, console.getStatus().getConditions().size());
            var ready = console.getStatus().getConditions().get(0);
            assertEquals("Ready", ready.getType());
            assertEquals("False", ready.getStatus());
            assertEquals("DependentsNotReady", ready.getReason());
            var warning = console.getStatus().getConditions().get(1);
            assertEquals("Warning", warning.getType());
            assertEquals("True", warning.getStatus());
            assertEquals("ReconcileException", warning.getReason());
            assertEquals("No such KafkaUser resource: ns1/invalid", warning.getMessage());
        });
    }

    @Test
    void testConsoleReconciliationWithMissingKafkaUserStatus() {
        KafkaUser userCR = new KafkaUserBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("ku1")
                        .withNamespace("ns1")
                        .build())
                .withNewSpec()
                    .withAuthentication(new KafkaUserScramSha512ClientAuthentication())
                .endSpec()
                // no status
                .build();

        client.resource(userCR).create();

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
                        .withNewCredentials()
                            .withNewKafkaUser()
                                .withName(userCR.getMetadata().getName())
                            .endKafkaUser()
                        .endCredentials()
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var console = client.resources(Console.class)
                    .inNamespace(consoleCR.getMetadata().getNamespace())
                    .withName(consoleCR.getMetadata().getName())
                    .get();
            assertEquals(2, console.getStatus().getConditions().size());
            var ready = console.getStatus().getConditions().get(0);
            assertEquals("Ready", ready.getType());
            assertEquals("False", ready.getStatus());
            assertEquals("DependentsNotReady", ready.getReason());
            var warning = console.getStatus().getConditions().get(1);
            assertEquals("Warning", warning.getType());
            assertEquals("True", warning.getStatus());
            assertEquals("ReconcileException", warning.getReason());
            assertEquals("KafkaUser ns1/ku1 missing .status.secret", warning.getMessage());
        });
    }

    @Test
    void testConsoleReconciliationWithMissingJaasConfigKey() {
        KafkaUser userCR = new KafkaUserBuilder()
                .withNewMetadata()
                    .withName("ku1")
                    .withNamespace("ns1")
                .endMetadata()
                .withNewSpec()
                    .withAuthentication(new KafkaUserScramSha512ClientAuthentication())
                .endSpec()
                .build();

        userCR = client.resource(userCR).create();
        client.resource(userCR).editStatus(user -> new KafkaUserBuilder(user)
                .withNewStatus()
                    .withSecret("ku1")
                .endStatus()
                .build());

        Secret userSecret = new SecretBuilder()
                .withNewMetadata()
                    .withName("ku1")
                    .withNamespace("ns1")
                    .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
                .endMetadata()
                // no data map
                .build();

        client.resource(userSecret).create();

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
                        .withNewCredentials()
                            .withNewKafkaUser()
                                .withName(userCR.getMetadata().getName())
                            .endKafkaUser()
                        .endCredentials()
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var console = client.resources(Console.class)
                    .inNamespace(consoleCR.getMetadata().getNamespace())
                    .withName(consoleCR.getMetadata().getName())
                    .get();
            assertEquals(2, console.getStatus().getConditions().size());
            var ready = console.getStatus().getConditions().get(0);
            assertEquals("Ready", ready.getType());
            assertEquals("False", ready.getStatus());
            assertEquals("DependentsNotReady", ready.getReason());
            var warning = console.getStatus().getConditions().get(1);
            assertEquals("Warning", warning.getType());
            assertEquals("True", warning.getStatus());
            assertEquals("ReconcileException", warning.getReason());
            assertEquals("Secret ns1/ku1 missing key 'sasl.jaas.config'", warning.getMessage());
        });
    }

    @Test
    void testConsoleReconciliationWithValidKafkaUser() {
        KafkaUser userCR = new KafkaUserBuilder()
                .withNewMetadata()
                    .withName("ku1")
                    .withNamespace("ns1")
                .endMetadata()
                .withNewSpec()
                    .withAuthentication(new KafkaUserScramSha512ClientAuthentication())
                .endSpec()
                .build();

        userCR = client.resource(userCR).create();
        client.resource(userCR).editStatus(user -> new KafkaUserBuilder(user)
                .withNewStatus()
                    .withSecret("ku1")
                .endStatus()
                .build());

        Secret userSecret = new SecretBuilder()
                .withNewMetadata()
                    .withName("ku1")
                    .withNamespace("ns1")
                    .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
                .endMetadata()
                .addToData(SaslConfigs.SASL_JAAS_CONFIG, Base64.getEncoder().encodeToString("jaas-config-value".getBytes()))
                .build();

        client.resource(userSecret).create();

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
                        .withNewCredentials()
                            .withNewKafkaUser()
                                .withName(userCR.getMetadata().getName())
                            .endKafkaUser()
                        .endCredentials()
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var console = client.resources(Console.class)
                    .inNamespace(consoleCR.getMetadata().getNamespace())
                    .withName(consoleCR.getMetadata().getName())
                    .get();
            assertEquals(1, console.getStatus().getConditions().size());
            var ready = console.getStatus().getConditions().get(0);
            assertEquals("Ready", ready.getType());
            assertEquals("False", ready.getStatus());
            assertEquals("DependentsNotReady", ready.getReason());

            var consoleSecret = client.secrets().inNamespace("ns2").withName("console-1-" + ConsoleSecret.NAME).get();
            assertNotNull(consoleSecret);
            String configEncoded = consoleSecret.getData().get("console-config.yaml");
            byte[] configDecoded = Base64.getDecoder().decode(configEncoded);
            ConsoleConfig consoleConfig = new ObjectMapper().readValue(configDecoded, ConsoleConfig.class);
            assertEquals("jaas-config-value",
                    consoleConfig.getKafka().getClusters().get(0).getProperties().get(SaslConfigs.SASL_JAAS_CONFIG));
        });
    }

    @Test
    void testConsoleReconciliationWithKafkaProperties() {
        client.resource(new ConfigMapBuilder()
                .withNewMetadata()
                    .withName("cm-1")
                    .withNamespace("ns2")
                    .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
                .endMetadata()
                .addToData("x-consumer-prop-name", "x-consumer-prop-value")
                .build())
            .serverSideApply();

        client.resource(new SecretBuilder()
                .withNewMetadata()
                    .withName("scrt-1")
                    .withNamespace("ns2")
                    .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
                .endMetadata()
                .addToData("x-producer-prop-name",
                        Base64.getEncoder().encodeToString("x-producer-prop-value".getBytes()))
                .build())
            .serverSideApply();

        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("console-1")
                        .withNamespace("ns2")
                        .build())
                .withNewSpec()
                    .withHostname("example.com")
                    .addNewKafkaCluster()
                        .withId("custom-id")
                        .withName(kafkaCR.getMetadata().getName())
                        .withNewProperties()
                            .addNewValue()
                                .withName("x-prop-name")
                                .withValue("x-prop-value")
                            .endValue()
                        .endProperties()
                        .withNewAdminProperties()
                            .addNewValue()
                                .withName("x-admin-prop-name")
                                .withValue("x-admin-prop-value")
                            .endValue()
                        .endAdminProperties()
                        .withNewConsumerProperties()
                            .addNewValuesFrom()
                                .withPrefix("extra-")
                                .withNewConfigMapRef("cm-1", false)
                            .endValuesFrom()
                            .addNewValuesFrom()
                                .withNewConfigMapRef("cm-2", true)
                            .endValuesFrom()
                        .endConsumerProperties()
                        .withNewProducerProperties()
                            .addNewValuesFrom()
                                .withNewSecretRef("scrt-1", false)
                            .endValuesFrom()
                        .endProducerProperties()
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var consoleSecret = client.secrets().inNamespace("ns2").withName("console-1-" + ConsoleSecret.NAME).get();
            assertNotNull(consoleSecret);
            String configEncoded = consoleSecret.getData().get("console-config.yaml");
            byte[] configDecoded = Base64.getDecoder().decode(configEncoded);
            ConsoleConfig consoleConfig = new ObjectMapper().readValue(configDecoded, ConsoleConfig.class);
            var kafkaConfig = consoleConfig.getKafka().getClusters().get(0);
            assertEquals("x-prop-value", kafkaConfig.getProperties().get("x-prop-name"));
            assertEquals("x-admin-prop-value", kafkaConfig.getAdminProperties().get("x-admin-prop-name"));
            assertEquals("x-consumer-prop-value", kafkaConfig.getConsumerProperties().get("extra-x-consumer-prop-name"));
            assertEquals("x-producer-prop-value", kafkaConfig.getProducerProperties().get("x-producer-prop-name"));
        });
    }

    @Test
    void testConsoleReconciliationWithSchemaRegistryUrl() {
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
                        .withNewSchemaRegistry()
                            .withUrl("http://example.com/apis/registry/v2")
                        .endSchemaRegistry()
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var console = client.resources(Console.class)
                    .inNamespace(consoleCR.getMetadata().getNamespace())
                    .withName(consoleCR.getMetadata().getName())
                    .get();
            assertEquals(1, console.getStatus().getConditions().size());
            var ready = console.getStatus().getConditions().get(0);
            assertEquals("Ready", ready.getType());
            assertEquals("False", ready.getStatus());
            assertEquals("DependentsNotReady", ready.getReason());

            var consoleSecret = client.secrets().inNamespace("ns2").withName("console-1-" + ConsoleSecret.NAME).get();
            assertNotNull(consoleSecret);
            String configEncoded = consoleSecret.getData().get("console-config.yaml");
            byte[] configDecoded = Base64.getDecoder().decode(configEncoded);
            ConsoleConfig consoleConfig = new ObjectMapper().readValue(configDecoded, ConsoleConfig.class);
            String registryUrl = consoleConfig.getKafka().getClusters().get(0).getSchemaRegistry().getUrl();
            Logger.getLogger(getClass()).infof("config YAML: %s", new String(configDecoded));
            assertEquals("http://example.com/apis/registry/v2", registryUrl);
        });
    }

    // Utility

    private Deployment setReady(Deployment deployment) {
        int desiredReplicas = Optional.ofNullable(deployment.getSpec().getReplicas()).orElse(1);

        return deployment.edit()
            .editOrNewStatus()
                .withReplicas(desiredReplicas)
                .withReadyReplicas(desiredReplicas)
            .endStatus()
            .build();
    }
}
