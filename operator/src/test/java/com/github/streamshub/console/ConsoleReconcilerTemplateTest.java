package com.github.streamshub.console;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.dependents.ConsoleDeployment;
import com.github.streamshub.console.dependents.ConsoleResource;
import com.github.streamshub.console.dependents.PrometheusDeployment;

import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.NodeAffinityBuilder;
import io.fabric8.kubernetes.api.model.NodeSelectorRequirementBuilder;
import io.fabric8.kubernetes.api.model.NodeSelectorTermBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.TolerationBuilder;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class ConsoleReconcilerTemplateTest extends ConsoleReconcilerTestBase {

    @Test
    void testConsoleReconciliationWithDeploymentTemplate() {
        var affinity = new AffinityBuilder()
                .withNodeAffinity(new NodeAffinityBuilder()
                        .addNewPreferredDuringSchedulingIgnoredDuringExecution()
                            .withWeight(1)
                            .withPreference(new NodeSelectorTermBuilder()
                                    .addToMatchExpressions(new NodeSelectorRequirementBuilder()
                                            .withKey("topology.kubernetes.io/zone")
                                            .withOperator("In")
                                            .withValues("us-east-1a")
                                            .build())
                                    .build())
                        .endPreferredDuringSchedulingIgnoredDuringExecution()
                        .build())
                .build();
        var toleration = new TolerationBuilder()
                .withKey("dedicated")
                .withOperator("Equal")
                .withValue("console")
                .withEffect("NoSchedule")
                .build();

        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(CONSOLE_NAME)
                        .withNamespace(CONSOLE_NS)
                        .build())
                .withNewSpec()
                    .withHostname("console.example.com")
                    .withNewDeployment()
                        .withNewSpec()
                            .withNewTemplate()
                                .withNewSpec()
                                    .withAffinity(affinity)
                                    .withTolerations(toleration)
                                    .withNodeSelector(Map.of("disktype", "ssd"))
                                .endSpec()
                            .endTemplate()
                        .endSpec()
                    .endDeployment()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        awaitDependentsNotReady(consoleCR, "ConsoleDeployment", "PrometheusDeployment");
        setDeploymentReady(consoleCR, PrometheusDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");
        var consoleDeployment = setDeploymentReady(consoleCR, ConsoleDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleIngress");
        setConsoleIngressReady(consoleCR);

        var podSpec = consoleDeployment.getSpec().getTemplate().getSpec();
        assertEquals(affinity, podSpec.getAffinity());
        assertEquals(List.of(toleration), podSpec.getTolerations());
        assertEquals(Map.of("disktype", "ssd"), podSpec.getNodeSelector());

        awaitReady(consoleCR);
    }

    @Test
    void testDeploymentAndPodMetadataTemplateApplied() {
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(CONSOLE_NAME)
                        .withNamespace(CONSOLE_NS)
                        .build())
                .withNewSpec()
                    .withHostname("console.example.com")
                    .withNewDeployment()
                        .withNewMetadata()
                            .withLabels(Map.of("custom-deploy-label", "deploy-value"))
                            .withAnnotations(Map.of("custom-deploy-annotation", "deploy-ann-value"))
                        .endMetadata()
                        .withNewSpec()
                            .withNewTemplate()
                                .withNewMetadata()
                                    .withLabels(Map.of("custom-pod-label", "pod-value"))
                                    .withAnnotations(Map.of("custom-pod-annotation", "pod-ann-value"))
                                .endMetadata()
                            .endTemplate()
                        .endSpec()
                    .endDeployment()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        awaitDependentsNotReady(consoleCR, "ConsoleDeployment", "PrometheusDeployment");
        setDeploymentReady(consoleCR, PrometheusDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");
        var consoleDeployment = setDeploymentReady(consoleCR, ConsoleDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleIngress");
        setConsoleIngressReady(consoleCR);

        // Deployment metadata — user labels merged alongside required system labels
        var deployLabels = consoleDeployment.getMetadata().getLabels();
        assertEquals("deploy-value", deployLabels.get("custom-deploy-label"));
        // System labels must not be overwritten
        assertEquals(ConsoleResource.MANAGER, deployLabels.get("app.kubernetes.io/managed-by"));
        assertEquals("console", deployLabels.get("app.kubernetes.io/name"));

        var deployAnnotations = consoleDeployment.getMetadata().getAnnotations();
        assertEquals("deploy-ann-value", deployAnnotations.get("custom-deploy-annotation"));

        // Pod template metadata — user labels merged alongside required instance label
        var podLabels = consoleDeployment.getSpec().getTemplate().getMetadata().getLabels();
        assertEquals("pod-value", podLabels.get("custom-pod-label"));
        // Required instance label must be present and not overwritten
        String expectedInstanceLabel = CONSOLE_NAME + "-" + ConsoleDeployment.NAME;
        assertEquals(expectedInstanceLabel, podLabels.get("app.kubernetes.io/instance"));

        var podAnnotations = consoleDeployment.getSpec().getTemplate().getMetadata().getAnnotations();
        assertEquals("pod-ann-value", podAnnotations.get("custom-pod-annotation"));

        awaitReady(consoleCR);
    }

    @Test
    void testSystemLabelsCannotBeOverwrittenByDeploymentTemplate() {
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(CONSOLE_NAME)
                        .withNamespace(CONSOLE_NS)
                        .build())
                .withNewSpec()
                    .withHostname("console.example.com")
                    .withNewDeployment()
                        .withNewMetadata()
                            // Attempt to overwrite system-managed labels
                            .withLabels(Map.of(
                                    "app.kubernetes.io/managed-by", "my-own-operator",
                                    "app.kubernetes.io/name", "my-app"))
                        .endMetadata()
                        .withNewSpec()
                            .withNewTemplate()
                                .withNewMetadata()
                                    // Attempt to overwrite the required instance label on the pod
                                    .withLabels(Map.of("app.kubernetes.io/instance", "tampered"))
                                .endMetadata()
                            .endTemplate()
                        .endSpec()
                    .endDeployment()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        awaitDependentsNotReady(consoleCR, "ConsoleDeployment", "PrometheusDeployment");
        setDeploymentReady(consoleCR, PrometheusDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");
        var consoleDeployment = setDeploymentReady(consoleCR, ConsoleDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleIngress");
        setConsoleIngressReady(consoleCR);

        var deployLabels = consoleDeployment.getMetadata().getLabels();
        assertEquals(ConsoleResource.MANAGER, deployLabels.get("app.kubernetes.io/managed-by"));
        assertEquals("console", deployLabels.get("app.kubernetes.io/name"));

        String expectedInstanceLabel = CONSOLE_NAME + "-" + ConsoleDeployment.NAME;
        var podLabels = consoleDeployment.getSpec().getTemplate().getMetadata().getLabels();
        assertEquals(expectedInstanceLabel, podLabels.get("app.kubernetes.io/instance"));

        awaitReady(consoleCR);
    }

    @Test
    void testServerContainerEnvOverridePreventedForSystemVar() {
        Console consoleCR = new ConsoleBuilder()
                .withMetadata(new ObjectMetaBuilder()
                        .withName(CONSOLE_NAME)
                        .withNamespace(CONSOLE_NS)
                        .build())
                .withNewSpec()
                    .withHostname("console.example.com")
                    .withNewDeployment()
                        .withNewSpec()
                            .withNewTemplate()
                                .withNewSpec()
                                    .withNewServerContainer()
                                        // Attempt to override the system-seeded CONSOLE_CONFIG_PATH
                                        .addToEnv(new EnvVarBuilder()
                                                .withName("CONSOLE_CONFIG_PATH")
                                                .withValue("/tampered/path")
                                                .build())
                                        // A custom var that should be added normally
                                        .addToEnv(new EnvVarBuilder()
                                                .withName("CUSTOM_VAR")
                                                .withValue("custom-value")
                                                .build())
                                    .endServerContainer()
                                .endSpec()
                            .endTemplate()
                        .endSpec()
                    .endDeployment()
                    .addNewKafkaCluster()
                        .withName(kafkaCR.getMetadata().getName())
                        .withNamespace(kafkaCR.getMetadata().getNamespace())
                        .withListener(kafkaCR.getSpec().getKafka().getListeners().get(0).getName())
                    .endKafkaCluster()
                .endSpec()
                .build();

        client.resource(consoleCR).create();

        awaitDependentsNotReady(consoleCR, "ConsoleDeployment", "PrometheusDeployment");
        setDeploymentReady(consoleCR, PrometheusDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleDeployment");
        var consoleDeployment = setDeploymentReady(consoleCR, ConsoleDeployment.NAME);
        awaitDependentsNotReady(consoleCR, "ConsoleIngress");
        setConsoleIngressReady(consoleCR);

        var env = consoleDeployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();

        // The system-seeded value must not be overwritten
        var configPath = env.stream()
                .filter(e -> "CONSOLE_CONFIG_PATH".equals(e.getName()))
                .findFirst()
                .orElseThrow();
        assertNotEquals("/tampered/path", configPath.getValue(),
                "CONSOLE_CONFIG_PATH must not be overrideable by the deployment template");

        // The custom var must be present
        assertTrue(env.stream().anyMatch(e -> "CUSTOM_VAR".equals(e.getName())
                && "custom-value".equals(e.getValue())),
                "CUSTOM_VAR should be added to the container env");

        awaitReady(consoleCR);
    }
}
