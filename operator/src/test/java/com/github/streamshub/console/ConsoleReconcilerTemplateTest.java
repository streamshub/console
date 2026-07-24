package com.github.streamshub.console;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.ConsoleBuilder;
import com.github.streamshub.console.dependents.ConsoleDeployment;
import com.github.streamshub.console.dependents.PrometheusDeployment;

import io.fabric8.kubernetes.api.model.AffinityBuilder;
import io.fabric8.kubernetes.api.model.NodeAffinityBuilder;
import io.fabric8.kubernetes.api.model.NodeSelectorRequirementBuilder;
import io.fabric8.kubernetes.api.model.NodeSelectorTermBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.TolerationBuilder;
import io.quarkus.test.junit.QuarkusTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
