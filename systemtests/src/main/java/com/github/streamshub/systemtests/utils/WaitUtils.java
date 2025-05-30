package com.github.streamshub.systemtests.utils;

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
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.wait.Wait;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

import static com.github.streamshub.systemtests.utils.ResourceUtils.listKubeResourcesByPrefix;

public class WaitUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(WaitUtils.class);
    private WaitUtils() {}

    /**
     * Waits for the first Deployment in the given namespace that matches the given name prefix to become ready.
     *
     * @param namespaceName        the namespace in which to search for the Deployment
     * @param deploymentNamePrefix the prefix of the Deployment name to match
     */
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

    /**
     * Wait for the Deployment resource to be deleted.
     *
     * @param namespace name of the Namespace in which the Deployment resides
     * @param name      name of the Deployment
     */
    public static void waitForDeploymentDeletion(String namespace, String name) {
        Wait.until(String.format("deletion of Deployment: %s/%s", namespace, name),
            TestFrameConstants.GLOBAL_POLL_INTERVAL_SHORT, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM,
            () -> {
                if (ResourceUtils.getKubeResource(Deployment.class, namespace, name) == null) {
                    return true;
                } else {
                    LOGGER.warn("Deployment: {}/{} has not been deleted yet!", namespace, name);
                    KubeResourceManager.get().deleteResourceWithWait(ResourceUtils.getKubeResource(Deployment.class, namespace, name));
                    return false;
                }
            });
    }

    /**
     * Waits for a Kubernetes Secret associated with a Kafka user to be created and contain non-empty data.
     *
     * @param namespace     the namespace where the Secret should be located
     * @param secretName    the name of the Secret to wait for
     */
    public static void waitForSecretReady(String namespace, String secretName) {
        Wait.until(String.format("creation of Secret %s/%s", namespace, secretName),
            TestFrameConstants.GLOBAL_POLL_INTERVAL_1_SEC, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM,
            () -> {
                Secret secret = ResourceUtils.getKubeResource(Secret.class, namespace, secretName);
                return secret != null && secret.getData() != null && !secret.getData().isEmpty();
            });
    }

    /**
     * Waits until all pods matching the given label selector are ready.
     * Container readiness is not checked.
     *
     * @param namespaceName the namespace in which the pods should exist
     * @param selector      the label selector used to find matching pods
     * @param expectPods    the expected number of pods
     * @param containers    whether to check container readiness (not used in this overload)
     */
    public static void waitForPodsReady(String namespaceName, LabelSelector selector, int expectPods, boolean containers) {
        waitForPodsReady(namespaceName, selector, expectPods, containers, () -> { });
    }

    /**
     * Waits until all pods matching the given label selector are ready, including optional container readiness.
     * Invokes the provided {@code onTimeout} callback if the timeout is reached.
     *
     * @param namespaceName the namespace in which the pods should exist
     * @param selector      the label selector used to find matching pods
     * @param expectPods    the expected number of pods
     * @param containers    whether to verify that all containers are also ready
     * @param onTimeout     a runnable to invoke if the readiness check times out
     */
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

    /**
     * Waits until the Kafka custom resource reaches the specified status and condition.
     *
     * @param namespaceName the namespace containing the Kafka resource
     * @param clusterName   the name of the Kafka custom resource
     * @param status        the desired {@link ResourceStatus}
     * @param condition     the desired {@link ConditionStatus}
     * @return {@code true} if the Kafka resource reached the desired state; {@code false} otherwise
     */
    public static boolean waitForKafkaStatus(String namespaceName, String clusterName, ResourceStatus status, ConditionStatus condition) {
        return KubeResourceManager.get().waitResourceCondition(ResourceUtils.getKubeResource(Kafka.class, namespaceName, clusterName),
            ResourceConditions.resourceHasDesiredState(status, condition));
    }

    /**
     * Waits for the Kafka custom resource to reach a {@code READY/TRUE} condition.
     *
     * @param namespaceName the namespace containing the Kafka resource
     * @param clusterName   the name of the Kafka cluster
     * @return {@code true} if the Kafka resource is ready; {@code false} otherwise
     */
    public static boolean waitForKafkaReady(String namespaceName, String clusterName) {
        return waitForKafkaStatus(namespaceName, clusterName, ResourceStatus.READY, ConditionStatus.TRUE);
    }

    /**
     * Waits until the Kafka broker node pool's replica count matches the expected value.
     *
     * @param namespace        the namespace containing the KafkaNodePool
     * @param kafkaClusterName the name of the Kafka cluster
     * @param replicas         the expected number of replicas
     */
    public static void waitForKafkaBrokerNodePoolReplicasInSpec(String namespace, String kafkaClusterName, int replicas) {
        Wait.until("KafkaNodePool Broker to contain replica count: " + replicas,
            TimeConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS, TimeConstants.GLOBAL_STATUS_TIMEOUT,
            () -> replicas == ResourceUtils.getKubeResource(KafkaNodePool.class, namespace,
                    KafkaNamingUtils.brokerPoolName(kafkaClusterName)).getSpec().getReplicas(),
            () -> LOGGER.error("Kafka config did not reflect proper replica count"));
    }

    /**
     * Waits for the Kafka resource to contain a specific annotation key with a given value.
     *
     * @param namespaceName   the namespace containing the Kafka resource
     * @param clusterName     the name of the Kafka cluster
     * @param annotationKey   the annotation key to check
     * @param annotationValue the expected value for the annotation key
     */
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

    /**
     * Waits until all component pods identified by the selector have rolled compared to the given snapshot.
     * <p>
     * This method polls until all relevant pods have new UIDs, indicating a restart.
     *
     * @param namespaceName the namespace containing the pods
     * @param selector      the label selector identifying the pods
     * @param snapshot      the previous snapshot of pod names to UIDs
     * @return the new snapshot after the pods have rolled
     */
    public static Map<String, String> waitForComponentPodsToRoll(String namespaceName, LabelSelector selector, Map<String, String> snapshot) {
        Wait.until("rolling update of component with selector: " + selector.toString(),
            TimeConstants.ROLLING_UPDATE_POLL_INTERVAL, PodUtils.getTimeoutForPodOperations(snapshot.size()), () -> {
                try {
                    return PodUtils.componentPodsHaveRolled(namespaceName, selector, snapshot);
                } catch (Exception e) {
                    LOGGER.error("An error has occurred during rolling update: {}", e.getMessage());
                    return false;
                }
            });

        LOGGER.info("Component matching selector [{}] has been successfully rolled", selector);
        return PodUtils.getPodSnapshotBySelector(namespaceName, selector);
    }

    /**
     * Waits for the Kafka custom resource to enter a {@code WARNING/TRUE} status condition.
     *
     * @param namespaceName the namespace containing the Kafka resource
     * @param clusterName   the name of the Kafka cluster
     */
    public static void waitForKafkaHasWarningStatus(String namespaceName, String clusterName) {
        waitForKafkaStatus(namespaceName, clusterName, ResourceStatus.WARNING, ConditionStatus.TRUE);
    }

    /**
     * Waits until the Kafka custom resource has no {@code WARNING/TRUE} status conditions.
     *
     * @param namespaceName the namespace containing the Kafka resource
     * @param clusterName   the name of the Kafka cluster
     */
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

    /**
     * Waits for a StatefulSet to reach the ready state.
     *
     * <p>This method continuously polls the specified StatefulSet in the given namespace until it is marked as ready.</p>
     * <p>It uses a predefined polling interval and timeout to determine readiness via Kubernetes resource status.</p>
     *
     * <p>This ensures that the StatefulSet has all its replicas ready before proceeding with further test steps.</p>
     */
    public static void waitForStatefulSetReady(String namespaceName, String statefulSetName) {
        Wait.until(String.format("StatefulSet %s/%s to be ready", namespaceName, statefulSetName), TimeConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM,
            () -> Readiness.isStatefulSetReady(ResourceUtils.getKubeResource(StatefulSet.class, namespaceName, statefulSetName))
        );
    }
}