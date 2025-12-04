package com.github.streamshub.console;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.TrustStore;
import com.github.streamshub.console.api.v1alpha1.spec.TrustStoreBuilder;
import com.github.streamshub.console.api.v1alpha1.spec.authentication.OIDC;
import com.github.streamshub.console.api.v1alpha1.spec.metrics.MetricsSource;
import com.github.streamshub.console.api.v1alpha1.spec.metrics.MetricsSource.Type;
import com.github.streamshub.console.api.v1alpha1.status.Condition;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.PrometheusConfig;
import com.github.streamshub.console.config.TrustStoreConfig;
import com.github.streamshub.console.config.authentication.Basic;
import com.github.streamshub.console.config.authentication.Bearer;
import com.github.streamshub.console.dependents.ConsoleClusterRole;
import com.github.streamshub.console.dependents.ConsoleClusterRoleBinding;
import com.github.streamshub.console.dependents.ConsoleDeployment;
import com.github.streamshub.console.dependents.ConsoleIngress;
import com.github.streamshub.console.dependents.ConsoleResource;
import com.github.streamshub.console.dependents.ConsoleSecret;
import com.github.streamshub.console.dependents.PrometheusClusterRole;
import com.github.streamshub.console.dependents.PrometheusClusterRoleBinding;
import com.github.streamshub.console.dependents.PrometheusDeployment;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.IngressBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteBuilder;
import io.quarkus.test.junit.QuarkusTest;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserBuilder;
import io.strimzi.api.kafka.model.user.KafkaUserScramSha512ClientAuthentication;
import io.strimzi.api.kafka.model.user.KafkaUserTlsClientAuthentication;
import io.strimzi.api.kafka.model.user.KafkaUserTlsExternalClientAuthentication;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ConsoleReconcilerTest extends ConsoleReconcilerTestBase {

    @Test
    void testBasicConsoleReconciliation() {
        Console consoleCR = createConsole(new ConsoleBuilder()
                .withNewSpec()
                    .withHostname("example.com")
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec());

        awaitDependentsNotReady(consoleCR, "ConsoleIngress", "PrometheusDeployment");
        setConsoleIngressReady(consoleCR);
        setDeploymentReady(consoleCR, PrometheusDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");

        var consoleDeployment = setDeploymentReady(consoleCR, ConsoleDeployment.NAME);
        // Images were not set in CR, so assert that the defaults were used
        var consoleContainers = consoleDeployment.getSpec().getTemplate().getSpec().getContainers();

        assertEquals(config.getValue("console.deployment.default-api-image", String.class),
                consoleContainers.get(0).getImage());
        assertEquals(config.getValue("console.deployment.default-ui-image", String.class),
                consoleContainers.get(1).getImage());

        awaitReady(consoleCR);
    }

    @ParameterizedTest
    @CsvSource({
        "2, oauth-auth-listener",
        "3, oauth-custom-auth-listener"
    })
    void testConsoleReconciliationWithOAuthBearerPlaintext(int listenerIndex, String listenerName) {
        Console consoleCR = createConsole(new ConsoleBuilder()
                .withNewSpec()
                    .withHostname("example.com")
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        // reference oauthbearer listener
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(listenerIndex).getName())
                    .endKafkaCluster()
                .endSpec());

        awaitDependentsNotReady(consoleCR, "ConsoleIngress", "PrometheusDeployment");
        setConsoleIngressReady(consoleCR);
        setDeploymentReady(consoleCR, PrometheusDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");
        setDeploymentReady(consoleCR, ConsoleDeployment.NAME);
        awaitReady(consoleCR);

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var consoleSecret = client.secrets().inNamespace(CONSOLE_NS)
                    .withName(CONSOLE_NAME + "-" + ConsoleSecret.NAME).get();
            assertNotNull(consoleSecret);
            String configEncoded = consoleSecret.getData().get("console-config.yaml");
            byte[] configDecoded = Base64.getDecoder().decode(configEncoded);
            ConsoleConfig consoleConfig = YAML.readValue(configDecoded, ConsoleConfig.class);
            var clusterConfig = consoleConfig.getKafka().getClusters().get(0);
            assertEquals(listenerName, clusterConfig.getListener());
            var properties = clusterConfig.getProperties();
            assertEquals("SASL_PLAINTEXT", properties.get(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG));
            assertEquals("OAUTHBEARER", properties.get(SaslConfigs.SASL_MECHANISM));
        });
    }

    @Test
    void testConsoleReconciliationWithContainerOverrides() {
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("console-1")
                        .withNamespace("ns2")
                        .build())
                .withNewSpec()
                    .withHostname("console.example.com")
                    .addNewMetricsSource()
                        .withName("metrics")
                        .withType(Type.STANDALONE)
                        .withUrl("http://prometheus.example.com")
                    .endMetricsSource()
                    .withNewImages()
                        .withApi("deprecated-api-image")
                        .withUi("deprecated-ui-image")
                    .endImages()
                    .addToEnv(new EnvVarBuilder()
                            .withName("DEPRECATED_API_VAR")
                            .withValue("value0")
                            .build())
                    .withNewContainers()
                        .withNewApi()
                            .withNewSpec()
                                .withImage("custom-api-image")
                                .withResources(new ResourceRequirementsBuilder()
                                        .withRequests(Map.of("cpu", Quantity.parse("250m")))
                                        .withLimits(Map.of("cpu", Quantity.parse("500m")))
                                        .build())
                                .addToEnv(new EnvVarBuilder()
                                        .withName("CUSTOM_API_VAR")
                                        .withValue("value1")
                                        .build())
                            .endSpec()
                        .endApi()
                        .withNewUi()
                            .withNewSpec()
                                .withImage("custom-ui-image")
                                .withResources(new ResourceRequirementsBuilder()
                                        .withRequests(Map.of("cpu", Quantity.parse("100m")))
                                        .withLimits(Map.of("cpu", Quantity.parse("200m")))
                                        .build())
                                .addToEnv(new EnvVarBuilder()
                                        .withName("CUSTOM_UI_VAR")
                                        .withValue("value2")
                                        .build())
                            .endSpec()
                        .endUi()
                    .endContainers()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                        .withMetricsSource("metrics")
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        awaitDependentsNotReady(consoleCR, "ConsoleIngress");
        setConsoleIngressReady(consoleCR);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");
        var consoleDeployment = setDeploymentReady(consoleCR, ConsoleDeployment.NAME);
        var consoleContainers = consoleDeployment.getSpec().getTemplate().getSpec().getContainers();
        var apiContainer = consoleContainers.get(0);

        assertEquals("custom-api-image", apiContainer.getImage());
        assertEquals(new ResourceRequirementsBuilder()
                .withRequests(Map.of("cpu", Quantity.parse("250m")))
                .withLimits(Map.of("cpu", Quantity.parse("500m")))
                .build(), apiContainer.getResources());
        assertEquals(3, apiContainer.getEnv().size()); // 2 overrides + 1 from YAML template
        assertEquals("value0", apiContainer.getEnv().stream()
                .filter(e -> e.getName().equals("DEPRECATED_API_VAR")).map(EnvVar::getValue).findFirst().orElseThrow());
        assertEquals("value1", apiContainer.getEnv().stream()
                .filter(e -> e.getName().equals("CUSTOM_API_VAR")).map(EnvVar::getValue).findFirst().orElseThrow());

        var uiContainer = consoleContainers.get(1);
        assertEquals("custom-ui-image", uiContainer.getImage());
        assertEquals(new ResourceRequirementsBuilder()
                .withRequests(Map.of("cpu", Quantity.parse("100m")))
                .withLimits(Map.of("cpu", Quantity.parse("200m")))
                .build(), uiContainer.getResources());
        assertEquals(7, uiContainer.getEnv().size()); // 1 override + 6 from YAML template
        assertEquals("value2", uiContainer.getEnv().stream()
                .filter(e -> e.getName().equals("CUSTOM_UI_VAR")).map(EnvVar::getValue).findFirst().orElseThrow());
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
            List<Condition> conditions = new ArrayList<>(console.getStatus().getConditions());
            assertEquals(2, conditions.size());
            var ready = conditions.get(0);
            assertEquals(Condition.Types.READY, ready.getType());
            assertEquals("False", ready.getStatus());
            assertEquals(Condition.Reasons.INVALID_CONFIGURATION, ready.getReason());
            var warning = conditions.get(1);
            assertEquals(Condition.Types.ERROR, warning.getType());
            assertEquals("True", warning.getStatus());
            assertEquals(Condition.Reasons.RECONCILIATION_EXCEPTION, warning.getReason());
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
            List<Condition> conditions = new ArrayList<>(console.getStatus().getConditions());
            assertEquals(2, conditions.size());
            var ready = conditions.get(0);
            assertEquals(Condition.Types.READY, ready.getType());
            assertEquals("False", ready.getStatus());
            assertEquals(Condition.Reasons.INVALID_CONFIGURATION, ready.getReason());
            var warning = conditions.get(1);
            assertEquals(Condition.Types.ERROR, warning.getType());
            assertEquals("True", warning.getStatus());
            assertEquals(Condition.Reasons.RECONCILIATION_EXCEPTION, warning.getReason());
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
            List<Condition> conditions = new ArrayList<>(console.getStatus().getConditions());
            assertEquals(2, conditions.size());
            var ready = conditions.get(0);
            assertEquals(Condition.Types.READY, ready.getType());
            assertEquals("False", ready.getStatus());
            assertEquals(Condition.Reasons.INVALID_CONFIGURATION, ready.getReason());
            var warning = conditions.get(1);
            assertEquals(Condition.Types.ERROR, warning.getType());
            assertEquals("True", warning.getStatus());
            assertEquals(Condition.Reasons.RECONCILIATION_EXCEPTION, warning.getReason());
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
            List<Condition> conditions = new ArrayList<>(console.getStatus().getConditions());
            assertEquals(2, conditions.size());
            var ready = conditions.get(0);
            assertEquals(Condition.Types.READY, ready.getType());
            assertEquals("False", ready.getStatus());
            assertEquals(Condition.Reasons.INVALID_CONFIGURATION, ready.getReason());
            var warning = conditions.get(1);
            assertEquals(Condition.Types.ERROR, warning.getType());
            assertEquals("True", warning.getStatus());
            assertEquals(Condition.Reasons.RECONCILIATION_EXCEPTION, warning.getReason());
            assertEquals("Secret ns1/ku1 missing key 'sasl.jaas.config'", warning.getMessage());
        });
    }

    @Test
    void testConsoleReconciliationWithValidScramKafkaUser() {
        KafkaUser userCR = new KafkaUserBuilder()
                .withNewMetadata()
                    .withName("ku1")
                    .withNamespace("ns1")
                .endMetadata()
                .withNewSpec()
                    .withAuthentication(new KafkaUserScramSha512ClientAuthentication())
                .endSpec()
                .withNewStatus()
                    .withSecret("ku1")
                .endStatus()
                .build();

        userCR = apply(client, userCR);

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
            var ready = console.getStatus().getConditions().iterator().next();
            assertEquals(Condition.Types.READY, ready.getType(), ready::toString);
            assertEquals("False", ready.getStatus(), ready::toString);
            assertEquals("DependentsNotReady", ready.getReason(), ready::toString);

            var consoleSecret = client.secrets().inNamespace("ns2").withName("console-1-" + ConsoleSecret.NAME).get();
            assertNotNull(consoleSecret);
            String configEncoded = consoleSecret.getData().get("console-config.yaml");
            byte[] configDecoded = Base64.getDecoder().decode(configEncoded);
            ConsoleConfig consoleConfig = YAML.readValue(configDecoded, ConsoleConfig.class);
            assertEquals("jaas-config-value",
                    consoleConfig.getKafka().getClusters().get(0).getProperties().get(SaslConfigs.SASL_JAAS_CONFIG));
        });
    }

    @Test
    void testConsoleReconciliationWithValidMutualTlsKafkaUser() {
        KafkaUser userCR = new KafkaUserBuilder()
                .withNewMetadata()
                    .withName("ku1")
                    .withNamespace("ns1")
                .endMetadata()
                .withNewSpec()
                    .withAuthentication(new KafkaUserTlsClientAuthentication())
                .endSpec()
                .withNewStatus()
                    .withSecret("ku1")
                .endStatus()
                .build();

        userCR = apply(client, userCR);

        Secret userSecret = new SecretBuilder()
                .withNewMetadata()
                    .withName("ku1")
                    .withNamespace("ns1")
                    .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
                .endMetadata()
                .addToData("ca.crt", Base64.getEncoder().encodeToString("cert-chain-value".getBytes()))
                .addToData("user.key", Base64.getEncoder().encodeToString("keystore-key-value".getBytes()))
                .addToData("user.crt", Base64.getEncoder().encodeToString("keystore-certs-value".getBytes()))
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
                        .withListener("tls-auth-listener")
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
            assertEquals(1, console.getStatus().getConditions().size(), () -> "Unexpected conditions: " + console.getStatus().getConditions());
            var ready = console.getStatus().getConditions().iterator().next();
            assertEquals(Condition.Types.READY, ready.getType(), ready::toString);
            assertEquals("False", ready.getStatus(), ready::toString);
            assertEquals("DependentsNotReady", ready.getReason(), ready::toString);

            var consoleSecret = client.secrets().inNamespace("ns2").withName("console-1-" + ConsoleSecret.NAME).get();
            assertNotNull(consoleSecret);
            String configEncoded = consoleSecret.getData().get("console-config.yaml");
            byte[] configDecoded = Base64.getDecoder().decode(configEncoded);
            ConsoleConfig consoleConfig = YAML.readValue(configDecoded, ConsoleConfig.class);
            Map<String, String> properties = consoleConfig.getKafka().getClusters().get(0).getProperties();

            // certificate from Kafka CR status overrides the value in the KafkaUser CR's secret
            assertEquals("PEM", properties.get(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG));
            assertEquals("--tls-auth-listener-certificate-chain--", properties.get(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG));

            assertEquals("PEM", properties.get(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG));
            assertEquals("keystore-key-value", properties.get(SslConfigs.SSL_KEYSTORE_KEY_CONFIG));

            assertEquals("keystore-certs-value", properties.get(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG));
        });
    }

    @Test
    void testConsoleReconciliationWithUnsupportedKafkaUserAuthentication() {
        KafkaUser userCR = new KafkaUserBuilder()
                .withNewMetadata()
                    .withName("ku1")
                    .withNamespace(KAFKA_NS)
                .endMetadata()
                .withNewSpec()
                    .withAuthentication(new KafkaUserTlsExternalClientAuthentication())
                .endSpec()
                .withNewStatus()
                    .withSecret("ku1")
                .endStatus()
                .build();

        userCR = apply(client, userCR);

        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(CONSOLE_NAME)
                        .withNamespace(CONSOLE_NS)
                        .build())
                .withNewSpec()
                    .withHostname("example.com")
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener("tls-auth-listener")
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
            List<Condition> conditions = new ArrayList<>(console.getStatus().getConditions());
            assertEquals(2, conditions.size());
            var ready = conditions.get(0);
            assertEquals(Condition.Types.READY, ready.getType());
            assertEquals("False", ready.getStatus());
            assertEquals(Condition.Reasons.INVALID_CONFIGURATION, ready.getReason());
            var warning = conditions.get(1);
            assertEquals(Condition.Types.ERROR, warning.getType());
            assertEquals("True", warning.getStatus());
            assertEquals(Condition.Reasons.RECONCILIATION_EXCEPTION, warning.getReason());
            assertEquals("Unsupported authentication type for KafkaUser ns1/ku1: 'tls-external'", warning.getMessage());
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
            ConsoleConfig consoleConfig = YAML.readValue(configDecoded, ConsoleConfig.class);
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
                    .addNewSchemaRegistry()
                        .withName("example-registry")
                        .withUrl("http://example.com/apis/registry/v2")
                    .endSchemaRegistry()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                        .withSchemaRegistry("example-registry")
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        assertConsoleConfig(consoleConfig -> {
            String registryName = consoleConfig.getSchemaRegistries().get(0).getName();
            assertEquals("example-registry", registryName);
            String registryUrl = consoleConfig.getSchemaRegistries().get(0).getUrl();
            assertEquals("http://example.com/apis/registry/v2", registryUrl);

            String registryNameRef = consoleConfig.getKafka().getClusters().get(0).getSchemaRegistry();
            assertEquals("example-registry", registryNameRef);
        });
    }

    @Test
    void testConsoleReconciliationWithKafkaConnect() {
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("console-1")
                        .withNamespace("ns2")
                        .build())
                .withNewSpec()
                    .withHostname("example.com")
                    .addNewKafkaConnectCluster()
                        .withName("example-connect-cluster")
                        .withUrl("http://example-connect-cluster.example.com")
                        .withMirrorMaker(false)
                        .withKafkaClusters(kafkaCR.getMetadata().getName())
                    .endKafkaConnectCluster()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        assertConsoleConfig(consoleConfig -> {
            String connectClusterName = consoleConfig.getKafkaConnectClusters().get(0).getName();
            assertEquals("example-connect-cluster", connectClusterName);
            String connectClusterUrl = consoleConfig.getKafkaConnectClusters().get(0).getUrl();
            assertEquals("http://example-connect-cluster.example.com", connectClusterUrl);

            String kafkaClusterNameRef = consoleConfig.getKafkaConnectClusters().get(0).getKafkaClusters().get(0);
            assertEquals(kafkaCR.getMetadata().getName(), kafkaClusterNameRef);
        });
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    void testConsoleReconciliationWithSchemaRegistryOIDCAuthN(boolean useTrustStore) {
        TrustStore oidcTrustStore = null;

        if (useTrustStore) {
            oidcTrustStore = new TrustStoreBuilder()
                .withType(TrustStore.Type.PEM)
                .withNewContent()
                    .withValue("---CERT---")
                .endContent()
                .build();
        }

        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("console-1")
                        .withNamespace("ns2")
                        .build())
                .withNewSpec()
                    .withHostname("example.com")
                    .addNewSchemaRegistry()
                        .withName("example-registry")
                        .withUrl("http://example.com/apis/registry/v2")
                        .withNewAuthentication()
                            .withNewOidc()
                                .withAbsoluteExpiresIn(true)
                                .withAuthServerUrl("https://keycloak.example.com/realms/kafka")
                                .withClientId("console-client-id")
                                .withNewClientSecret()
                                    .withValue("console-client-secret")
                                .endClientSecret()
                                .withGrantType(OIDC.GrantType.CLIENT)
                                .withGrantOptions(Map.of("audience", "http://example.com/apis/registry/v2"))
                                .withTrustStore(oidcTrustStore)
                            .endOidc()
                        .endAuthentication()
                    .endSchemaRegistry()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                        .withSchemaRegistry("example-registry")
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        assertConsoleConfig(consoleConfig -> {
            var registryConfig = consoleConfig.getSchemaRegistries().get(0);
            assertEquals("example-registry", registryConfig.getName());
            assertEquals("http://example.com/apis/registry/v2", registryConfig.getUrl());

            String registryNameRef = consoleConfig.getKafka().getClusters().get(0).getSchemaRegistry();
            assertEquals("example-registry", registryNameRef);

            var consoleSecretData = getConsoleSecret(consoleCR).getData();
            var registryAuthN = registryConfig.getAuthentication().getOidc();
            assertEquals(
                    "/deployments/config/schema-registry-example-registry-oidc-clientsecret.txt",
                    registryAuthN.getClientSecret().getValueFrom());
            assertArrayEquals(
                    "console-client-secret".getBytes(),
                    Base64.getDecoder().decode(consoleSecretData.get("schema-registry-example-registry-oidc-clientsecret.txt")));

            if (useTrustStore) {
                var registryOidcTrustStore = registryAuthN.getTrustStore();
                assertEquals(TrustStoreConfig.Type.PEM, registryOidcTrustStore.getType());
                assertEquals(
                    "/deployments/config/truststore-schema-registry-example-registry-oidc-content.pem",
                    registryOidcTrustStore.getContent().getValueFrom()
                );
            }
        });
    }

    @Test
    void testConsoleReconciliationWithOpenShiftMonitoring() {
        String thanosQueryHost = "thanos.example.com";

        client.resource(new NamespaceBuilder()
                .withNewMetadata()
                    .withName("openshift-monitoring")
                .endMetadata()
                .build())
            .serverSideApply();

        Route thanosQuerier = new RouteBuilder()
                .withNewMetadata()
                    .withNamespace("openshift-monitoring")
                    .withName("thanos-querier")
                .endMetadata()
                .withNewSpec()
                .endSpec()
                .withNewStatus()
                    .addNewIngress()
                        .withHost(thanosQueryHost)
                    .endIngress()
                .endStatus()
                .build();

        apply(client, thanosQuerier);

        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("console-1")
                        .withNamespace("ns2")
                        .build())
                .withNewSpec()
                    .withHostname("example.com")
                    .addNewMetricsSource()
                        .withName("ocp-platform-monitoring")
                        .withType(Type.fromValue("openshift-monitoring"))
                    .endMetricsSource()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                        .withMetricsSource("ocp-platform-monitoring")
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        assertConsoleConfig(consoleConfig -> {
            String metricsName = consoleConfig.getMetricsSources().get(0).getName();
            assertEquals("ocp-platform-monitoring", metricsName);
            String metricsUrl = consoleConfig.getMetricsSources().get(0).getUrl();
            assertEquals("https://" + thanosQueryHost, metricsUrl);

            String metricsRef = consoleConfig.getKafka().getClusters().get(0).getMetricsSource();
            assertEquals("ocp-platform-monitoring", metricsRef);
        });
    }

    void testConsoleReconciliationWithDeprecatedPrometheusBasicAuthN() {
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("console-1")
                        .withNamespace("ns2")
                        .build())
                .withNewSpec()
                    .withHostname("example.com")
                    .addNewMetricsSource()
                        .withName("some-prometheus")
                        .withType(Type.fromValue("standalone"))
                        .withUrl("https://prometheus.example.com")
                        .withNewAuthentication()
                            .withUsername("pr0m3th3u5")
                            .withPassword("password42")
                        .endAuthentication()
                    .endMetricsSource()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                        .withMetricsSource("some-prometheus")
                    .endKafkaCluster()
                .endSpec()
                .build();

        var thrown = assertThrows(KubernetesClientException.class, client.resource(consoleCR)::create);
        assertEquals(422, thrown.getStatus().getCode());
    }

    @Test
    void testConsoleReconciliationWithPrometheusBasicAuthN() {
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("console-1")
                        .withNamespace("ns2")
                        .build())
                .withNewSpec()
                    .withHostname("example.com")
                    .addNewMetricsSource()
                        .withName("some-prometheus")
                        .withType(Type.fromValue("standalone"))
                        .withUrl("https://prometheus.example.com")
                        .withNewAuthentication()
                            .withNewBasic()
                                .withUsername("pr0m3th3u5")
                                .withNewPassword()
                                    .withValue("password42")
                                .endPassword()
                            .endBasic()
                        .endAuthentication()
                    .endMetricsSource()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                        .withMetricsSource("some-prometheus")
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        assertConsoleConfig(consoleConfig -> {
            var prometheusConfig = consoleConfig.getMetricsSources().get(0);
            assertEquals("some-prometheus", prometheusConfig.getName());
            assertEquals("https://prometheus.example.com", prometheusConfig.getUrl());
            assertEquals(PrometheusConfig.Type.STANDALONE, prometheusConfig.getType());
            var prometheusAuthN = (Basic) prometheusConfig.getAuthentication().getBasic();
            assertEquals("pr0m3th3u5", prometheusAuthN.getUsername());
            assertEquals(
                    "/deployments/config/metrics-source-some-prometheus-basic-password.txt",
                    prometheusAuthN.getPassword().getValueFrom());
            var secretData = getConsoleSecret(consoleCR).getData();
            assertArrayEquals(
                    "password42".getBytes(),
                    Base64.getDecoder().decode(secretData.get("metrics-source-some-prometheus-basic-password.txt")));

            String metricsRef = consoleConfig.getKafka().getClusters().get(0).getMetricsSource();
            assertEquals("some-prometheus", metricsRef);
        });
    }

    void testConsoleReconciliationWithDeprecatedPrometheusBearerAuthN() {
        String token = UUID.randomUUID().toString();
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("console-1")
                        .withNamespace("ns2")
                        .build())
                .withNewSpec()
                    .withHostname("example.com")
                    .addNewMetricsSource()
                        .withName("some-prometheus")
                        .withType(Type.fromValue("standalone"))
                        .withUrl("https://prometheus.example.com")
                        .withNewAuthentication()
                            .withToken(token)
                        .endAuthentication()
                    .endMetricsSource()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                        .withMetricsSource("some-prometheus")
                    .endKafkaCluster()
                .endSpec()
                .build();

        var thrown = assertThrows(KubernetesClientException.class, client.resource(consoleCR)::create);
        assertEquals(422, thrown.getStatus().getCode());
    }

    @Test
    void testConsoleReconciliationWithPrometheusBearerAuthN() {
        String token = UUID.randomUUID().toString();
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("console-1")
                        .withNamespace("ns2")
                        .build())
                .withNewSpec()
                    .withHostname("example.com")
                    .addNewMetricsSource()
                        .withName("some-prometheus")
                        .withType(Type.fromValue("standalone"))
                        .withUrl("https://prometheus.example.com")
                        .withNewAuthentication()
                            .withNewBearer()
                                .withNewToken()
                                    .withValue(token)
                                .endToken()
                            .endBearer()
                        .endAuthentication()
                    .endMetricsSource()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                        .withMetricsSource("some-prometheus")
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        assertConsoleConfig(consoleConfig -> {
            var prometheusConfig = consoleConfig.getMetricsSources().get(0);
            assertEquals("some-prometheus", prometheusConfig.getName());
            assertEquals("https://prometheus.example.com", prometheusConfig.getUrl());
            assertEquals(PrometheusConfig.Type.STANDALONE, prometheusConfig.getType());
            var prometheusAuthN = (Bearer) prometheusConfig.getAuthentication().getBearer();
            assertEquals(
                    "/deployments/config/metrics-source-some-prometheus-bearer-token.txt",
                    prometheusAuthN.getToken().getValueFrom());
            assertArrayEquals(
                    token.getBytes(),
                    Base64.getDecoder().decode(getConsoleSecret(consoleCR).getData().get("metrics-source-some-prometheus-bearer-token.txt")));

            String metricsRef = consoleConfig.getKafka().getClusters().get(0).getMetricsSource();
            assertEquals("some-prometheus", metricsRef);
        });
    }

    @Test
    void testConsoleReconciliationWithPrometheusEmptyAuthN() {
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("console-1")
                        .withNamespace("ns2")
                        .build())
                .withNewSpec()
                    .withHostname("example.com")
                    .addNewMetricsSource()
                        .withName("some-prometheus")
                        .withType(Type.fromValue("standalone"))
                        .withUrl("https://prometheus.example.com")
                        .withNewAuthentication()
                        .endAuthentication()
                    .endMetricsSource()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                        .withMetricsSource("some-prometheus")
                    .endKafkaCluster()
                .endSpec()
                .build();

        // Fails K8s resource validation due to empty `spec.metricsSources[0].authentication object
        assertThrows(KubernetesClientException.class, client.resource(consoleCR)::create);
    }

    @Test
    void testConsoleReconciliationWithTrustStores() {
        final String jksPassword = "0p3n535@m3";
        Secret passwordSecret = new SecretBuilder()
                .withNewMetadata()
                    .withName("my-secret")
                    .withNamespace("ns2")
                    .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
                .endMetadata()
                .addToData("pass", Base64.getEncoder().encodeToString(jksPassword.getBytes()))
                .build();
        ConfigMap contentConfigMap = new ConfigMapBuilder()
                .withNewMetadata()
                    .withName("my-configmap")
                    .withNamespace("ns2")
                    .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
                .endMetadata()
                .addToData("truststore", "dummy-keystore")
                .build();

        client.resource(passwordSecret).create();
        client.resource(contentConfigMap).create();

        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName("console-1")
                        .withNamespace("ns2")
                        .build())
                .withNewSpec()
                    .withHostname("example.com")
                    .addNewMetricsSource()
                        .withName("example-prometheus")
                        .withType(MetricsSource.Type.STANDALONE)
                        .withUrl("https://prometheus.example.com")
                        .withNewTrustStore()
                            .withType(TrustStore.Type.JKS)
                            .withNewPassword()
                                .withNewValueFrom()
                                    .withNewSecretKeyRef("pass", "my-secret", Boolean.FALSE)
                                .endValueFrom()
                            .endPassword()
                            .withNewContent()
                                .withNewValueFrom()
                                    .withNewConfigMapKeyRef("truststore", "my-configmap", Boolean.FALSE)
                                .endValueFrom()
                            .endContent()
                            .withAlias("cert-ca")
                        .endTrustStore()
                    .endMetricsSource()
                    .addNewSchemaRegistry()
                        .withName("example-registry")
                        .withUrl("https://example.com/apis/registry/v2")
                        .withNewTrustStore()
                            .withType(TrustStore.Type.PEM)
                            .withNewContent()
                                .withValue("---CERT---")
                            .endContent()
                        .endTrustStore()
                    .endSchemaRegistry()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                        .withSchemaRegistry("example-registry")
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        awaitDependentsNotReady(consoleCR, "ConsoleIngress");
        setConsoleIngressReady(consoleCR);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");

        assertConsoleConfig(consoleConfig -> {
            var metricsTrustStore = consoleConfig.getMetricsSources().get(0).getTrustStore();
            assertEquals(TrustStoreConfig.Type.JKS, metricsTrustStore.getType());
            assertEquals(
                "/deployments/config/truststore-metrics-source-example-prometheus-content.jks",
                metricsTrustStore.getContent().getValueFrom()
            );
            assertEquals(
                "/deployments/config/truststore-metrics-source-example-prometheus-password.txt",
                metricsTrustStore.getPassword().getValueFrom()
            );
            assertEquals("cert-ca", metricsTrustStore.getAlias());

            var registryTrustStore = consoleConfig.getSchemaRegistries().get(0).getTrustStore();
            assertEquals(TrustStoreConfig.Type.PEM, registryTrustStore.getType());
            assertEquals(
                "/deployments/config/truststore-schema-registry-example-registry-content.pem",
                registryTrustStore.getContent().getValueFrom()
            );
        });
    }

    @Test
    void testConsoleReconciliationWithInvalidConfigError() {
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(CONSOLE_NAME)
                        .withNamespace(CONSOLE_NS)
                        .build())
                .withNewSpec()
                    .withHostname("example.com")
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                        .withMetricsSource("does-not-exist")
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        AtomicReference<Condition> initialError = new AtomicReference<>();

        assertInvalidConfiguration(consoleCR, conditions -> {
            var errorCondition = conditions.get(0);
            assertEquals(Condition.Types.ERROR, errorCondition.getType(), errorCondition::toString);
            assertEquals("True", errorCondition.getStatus(), errorCondition::toString);
            assertEquals(Condition.Reasons.INVALID_CONFIGURATION, errorCondition.getReason(), errorCondition::toString);
            assertEquals("Kafka cluster references an unknown metrics source", errorCondition.getMessage(), errorCondition::toString);
            initialError.set(errorCondition);
        });

        var updatedConsoleCR = client.resource(consoleCR)
            .edit(console -> new ConsoleBuilder(console)
                    .editSpec()
                        .withHostname("console.example.com")
                    .endSpec()
                    .build());
        var lastGeneration = updatedConsoleCR.getStatus().getObservedGeneration();

        await().atMost(LIMIT).untilAsserted(() -> {
            var generation = client.resource(updatedConsoleCR).get().getStatus().getObservedGeneration();
            assertTrue(generation > lastGeneration);
        });

        assertInvalidConfiguration(consoleCR, conditions -> {
            var errorCondition = conditions.get(0);
            // the condition should not have changed
            assertEquals(initialError.get(), errorCondition);
            assertEquals(initialError.get().getLastTransitionTime(), errorCondition.getLastTransitionTime());
        });
    }

    @Test
    void testMultipleConsoleReconciliationsWithSameName() {
        Console consoleCR1 = createConsole(new ConsoleBuilder()
                .withNewSpec()
                    .withHostname("console1.example.com")
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec());

        createNamespace("ns3");

        Console consoleCR2 = createConsole(new ConsoleBuilder()
                .withNewSpec()
                    .withHostname("console2.example.com")
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec(), "ns3");

        Stream.of(consoleCR1, consoleCR2)
            .map(cr -> {
                return CompletableFuture.runAsync(() -> {
                    awaitDependentsNotReady(cr, "ConsoleIngress", "PrometheusDeployment");
                    setConsoleIngressReady(cr);
                    setDeploymentReady(cr, PrometheusDeployment.NAME);
                    awaitDependentsNotReady(cr, "ConsoleDeployment");
                    setDeploymentReady(cr, ConsoleDeployment.NAME);
                    awaitReady(cr);
                }).thenRunAsync(() -> {
                    String prefix = cr.optionalMetadata()
                            .map(meta -> String.format("%s-%s-", meta.getNamespace(), meta.getName()))
                            .orElseThrow();
                    assertNotNull(client.resources(ClusterRole.class)
                            .withName(prefix + ConsoleClusterRole.NAME).get());
                    assertNotNull(client.resources(ClusterRoleBinding.class)
                            .withName(prefix + ConsoleClusterRoleBinding.NAME).get());
                    assertNotNull(client.resources(ClusterRole.class)
                            .withName(prefix + PrometheusClusterRole.NAME).get());
                    assertNotNull(client.resources(ClusterRoleBinding.class)
                            .withName(prefix + PrometheusClusterRoleBinding.NAME).get());
                });
            })
            .forEach(CompletableFuture::join);
    }

    @Test
    void testConsoleReconciliationWithResourceConflicts() {
        client.resource(new IngressBuilder()
                .withNewMetadata()
                    .withNamespace(CONSOLE_NS)
                    .withName(CONSOLE_NAME + '-' + ConsoleIngress.NAME)
                    .withNamespace(CONSOLE_NS)
                    .withLabels(ConsoleResource.MANAGEMENT_LABEL)
                .endMetadata()
                .withNewSpec()
                    .withNewDefaultBackend()
                        .withNewService()
                            .withName("fake-service")
                            .withNewPort()
                                .withNumber(8080)
                            .endPort()
                        .endService()
                    .endDefaultBackend()
                .endSpec()
                .build())
            .serverSideApply();

        client.resource(new DeploymentBuilder()
                .withNewMetadata()
                    .withNamespace(CONSOLE_NS)
                    .withName(CONSOLE_NAME + '-' + PrometheusDeployment.NAME)
                    .withNamespace(CONSOLE_NS)
                    .withLabels(ConsoleResource.MANAGEMENT_LABEL)
                .endMetadata()
                .withNewSpec()
                    .withNewSelector()
                        .addToMatchLabels("app", "pause")
                    .endSelector()
                    .withNewTemplate()
                        .withNewMetadata()
                            .addToLabels("app", "pause")
                        .endMetadata()
                        .withNewSpec()
                            .addNewContainer()
                                .withName("pause")
                                .withImage("k8s.gcr.io/pause:3.1")
                                .withImagePullPolicy("IfNotPresent")
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec()
                .build())
            .serverSideApply();

        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(CONSOLE_NAME)
                        .withNamespace(CONSOLE_NS)
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

        client.resource(consoleCR).serverSideApply();

        await().ignoreException(NullPointerException.class).atMost(LIMIT).untilAsserted(() -> {
            var console = client.resources(Console.class)
                    .inNamespace(consoleCR.getMetadata().getNamespace())
                    .withName(consoleCR.getMetadata().getName())
                    .get();
            List<Condition> conditions = new ArrayList<>(console.getStatus().getConditions());
            assertEquals(3, conditions.size());

            var ready = conditions.get(0);
            assertEquals(Condition.Types.READY, ready.getType());
            assertEquals("False", ready.getStatus());
            assertEquals(Condition.Reasons.RECONCILIATION_EXCEPTION, ready.getReason());

            var errors = conditions.subList(1, conditions.size());

            for (var error : errors) {
                assertEquals(Condition.Types.ERROR, error.getType());
                assertEquals("True", error.getStatus());
                assertEquals(Condition.Reasons.RECONCILIATION_EXCEPTION, error.getReason());
                assertThat(error.getMessage(), containsString("code=409"));
            }

            var errorMessages = errors.stream().map(Condition::getMessage).toList();
            assertThat(errorMessages, hasItem(containsString(CONSOLE_NAME + '-' + ConsoleIngress.NAME)));
            assertThat(errorMessages, hasItem(containsString(CONSOLE_NAME + '-' + PrometheusDeployment.NAME)));
        });
    }
}
