package com.github.streamshub.systemtests.setup;

import com.github.streamshub.systemtests.Environment;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.ResourceRequirementsBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;

import java.util.HashMap;
import java.util.Map;

public class ScraperPod {

    private ScraperPod() { }

    public static DeploymentBuilder getDefaultPod(String namespaceName, String podName) {
        Map<String, String> label = new HashMap<>();

        label.put("user-test-app", "scraper");
        label.put("deployment-type", "Scraper");

        return new DeploymentBuilder()
            .withNewMetadata()
                .withName(podName)
                .withLabels(label)
                .withNamespace(namespaceName)
            .endMetadata()
            .withNewSpec()
                .withNewSelector()
                    .addToMatchLabels("app", podName)
                    .addToMatchLabels(label)
                .endSelector()
                .withReplicas(1)
                .withNewTemplate()
                    .withNewMetadata()
                        .addToLabels("app", podName)
                        .addToLabels(label)
                    .endMetadata()
                    .withNewSpecLike(new PodSpecBuilder().build())
                        .withContainers(
                            new ContainerBuilder()
                                .withName(podName)
                                .withImage("quay.io/strimzi/kafka:latest-kafka-" + Environment.ST_KAFKA_VERSION)
                                .withCommand("sleep")
                                .withArgs("infinity")
                                .withImagePullPolicy("IfNotPresent")
                                .withResources(new ResourceRequirementsBuilder()
                                    .addToRequests("memory", new Quantity("200M"))
                                    .build())
                                .build()
                        )
                    .endSpec()
                .endTemplate()
            .endSpec();
    }
}
