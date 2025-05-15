package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.TestCaseConfig;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.constants.ResourceConditions;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.enums.ConditionStatus;
import com.github.streamshub.systemtests.enums.ResourceStatus;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.wait.Wait;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.github.streamshub.systemtests.utils.ResourceUtils.listKubeResourcesByPrefix;

public class WaitUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(WaitUtils.class);
    private WaitUtils() {}

    // ------------
    // Deployment
    // ------------
    public static void waitForDeploymentWithPrefixIsReady(String namespaceName, String deploymentNamePrefix) {
        Wait.until(String.format("creation of Deployment with prefix: %s in Namespace: %s", deploymentNamePrefix, namespaceName),
            TestFrameConstants.GLOBAL_POLL_INTERVAL_1_SEC, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM,
            () -> {
                Deployment dep = listKubeResourcesByPrefix(Deployment.class, namespaceName, deploymentNamePrefix).get(0);
                if (dep == null) {
                    return false;
                }
                return Readiness.isDeploymentReady(dep);
            });
    }

    // ------------
    // Secret
    // ------------
    public static void waitForSecretReady(String namespace, String kafkaUserName) {
        Wait.until(String.format("creation of Secret %s/%s", namespace, kafkaUserName),
            TestFrameConstants.GLOBAL_POLL_INTERVAL_1_SEC, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM,
            () -> {
                Secret secret = ResourceUtils.getKubeResource(Secret.class, namespace, kafkaUserName);
                return secret != null && secret.getData() != null && !secret.getData().isEmpty();
            });
    }

    // ------------
    // Pods
    // ------------
    public static void waitForPodsReady(String namespaceName, LabelSelector selector, int expectPods, boolean containers) {
        waitForPodsReady(namespaceName, selector, expectPods, containers, () -> { });
    }

    public static void waitForPodsReady(String namespaceName, LabelSelector selector, int expectPods, boolean containers, Runnable onTimeout) {
        Wait.until("readiness of all Pods matching: " + selector,
            TimeConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM,
            () -> {
                List<Pod> pods = ResourceUtils.listKubeResourcesByLabelSelector(Pod.class, namespaceName, selector);
                if (pods.isEmpty() || pods.size() != expectPods) {
                    LOGGER.debug("Expected pods: {}/{} are not ready", namespaceName, selector);
                    return false;
                }
                for (Pod pod : pods) {
                    if (!Readiness.isPodReady(pod)) {
                        LOGGER.debug("Pod not ready: {}/{}", namespaceName, pod.getMetadata().getName());
                        return false;
                    }

                    if (containers) {
                        for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                            if (Boolean.FALSE.equals(cs.getReady())) {
                                LOGGER.debug("Container: {} of Pod: {}/{} not ready", namespaceName, pod.getMetadata().getName(), cs.getName());
                                return false;
                            }
                        }
                    }
                }
                return true;
            }, onTimeout);
    }


    public static void waitForPodRoll(String namespace, String podName, Map<String, String> podSnapshot) {
        waitForPodRoll(namespace, podName, podSnapshot, TestFrameConstants.GLOBAL_POLL_INTERVAL_MEDIUM, TestFrameConstants.GLOBAL_TIMEOUT);
    }

    public static void waitForPodRoll(String namespace, String podName, Map<String, String> podSnapshots, long poll, long timeout) {
        Wait.until("Pod to roll " + namespace + "/" + podName, poll, timeout,
            () -> {
                Pod pod = ResourceUtils.getKubeResource(Pod.class, namespace, podName);
                if (pod != null) {
                    return !Objects.equals(podSnapshots.get(podName), pod.getMetadata().getUid());
                }
                return false;
            }
        );
    }

    // ------------
    // Kafka
    // ------------
    public static boolean waitForKafkaStatus(String namespaceName, String clusterName, ResourceStatus status, ConditionStatus condition) {
        return KubeResourceManager.get().waitResourceCondition(ResourceUtils.getKubeResource(Kafka.class, namespaceName, clusterName),
            ResourceConditions.resourceHasDesiredState(status, condition));
    }

    public static boolean waitForKafkaReady(String namespaceName, String clusterName) {
        return waitForKafkaStatus(namespaceName, clusterName, ResourceStatus.READY, ConditionStatus.TRUE);
    }

    public static void waitForKafkaBrokerNodePoolReplicasInSpec(String namespace, String kafkaClusterName, int replicas) {
        Wait.until("KafkaNodePool Broker to contain replica count: " + replicas,
            TimeConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS, TimeConstants.GLOBAL_STATUS_TIMEOUT,
            () -> replicas == ResourceUtils.getKubeResource(KafkaNodePool.class, namespace,
                    KafkaNamingUtils.brokerPoolName(kafkaClusterName)).getSpec().getReplicas(),
            () -> LOGGER.error("Kafka config did not reflect proper replica count"));
    }

    public static void waitForKafkaAnnotationWithValue(String namespaceName, String clusterName, String annotationKey, String annotationValue) {
        Wait.until(String.format("Kafka %s/%s has annotation %s : %s", namespaceName, clusterName, annotationKey, annotationValue),
            TestFrameConstants.GLOBAL_POLL_INTERVAL_SHORT, TestFrameConstants.GLOBAL_TIMEOUT_SHORT,
            () -> {
                Map<String, String> anno = ResourceUtils.getKubeResource(Kafka.class, namespaceName, clusterName).getMetadata().getAnnotations();
                if (!annotationKey.isEmpty()) {
                    return anno.getOrDefault(annotationKey, "nonexistent").equals(annotationValue);
                }
                return false;
            });
    }

    public static void waitForKafkaPodsRoll(TestCaseConfig tcc, Map<String, String> kafkaPodsSnapshots) {
        for (Pod controllerPod : ResourceUtils.listKubeResourcesByLabelSelector(Pod.class, tcc.namespace(), Labels.getKnpControllerLabelSelector(tcc.kafkaName()))) {
            waitForPodRoll(tcc.namespace(), controllerPod.getMetadata().getName(), kafkaPodsSnapshots);
        }

        for (Pod brokerPod : ResourceUtils.listKubeResourcesByLabelSelector(Pod.class, tcc.namespace(), Labels.getKnpBrokerLabelSelector(tcc.kafkaName()))) {
            waitForPodRoll(tcc.namespace(), brokerPod.getMetadata().getName(), kafkaPodsSnapshots);
        }

        waitForPodsReady(tcc.namespace(), Labels.getKafkaPodLabelSelector(tcc.kafkaName()), kafkaPodsSnapshots.size(), true);
    }

    public static void waitForKafkaHasWarningStatus(String namespaceName, String clusterName) {
        waitForKafkaStatus(namespaceName, clusterName, ResourceStatus.WARNING, ConditionStatus.TRUE);
    }

    public static void waitForKafkaHasNoWarningStatus(String namespaceName, String clusterName) {
        Wait.until("Kafka has no warnings",
            TimeConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS, TimeConstants.GLOBAL_STATUS_TIMEOUT,
            () -> {
                Kafka kafka = ResourceUtils.getKubeResource(Kafka.class, namespaceName, clusterName);
                if (kafka.getStatus() != null) {
                    return kafka.getStatus().getConditions().isEmpty() ||
                        !ResourceConditions.checkMatchingConditions(kafka, ResourceStatus.WARNING, ConditionStatus.TRUE);
                }
                return false;
            });
    }
}
