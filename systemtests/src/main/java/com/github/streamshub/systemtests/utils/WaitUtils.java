package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.clients.KafkaClients;
import com.github.streamshub.systemtests.constants.ResourceConditions;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.enums.ConditionStatus;
import com.github.streamshub.systemtests.enums.ResourceStatus;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.resourceutils.JobUtils;
import com.github.streamshub.systemtests.utils.resourceutils.KafkaNamingUtils;
import com.github.streamshub.systemtests.utils.resourceutils.PodUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.wait.Wait;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

import static com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils.listKubeResourcesByPrefix;

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
                List<Deployment> dep = listKubeResourcesByPrefix(Deployment.class, namespaceName, deploymentNamePrefix);
                if (dep.isEmpty() || dep.get(0) == null) {
                    return false;
                }
                return Readiness.isDeploymentReady(dep.get(0));
            });
    }

    /**
     * Waits for a Kubernetes Secret associated with a Kafka user to be created and contain non-empty data.
     *
     * @param namespace     the namespace where the Secret should be located
     * @param kafkaUserName the name of the Kafka user, used as the Secret name
     */
    public static void waitForSecretReady(String namespace, String kafkaUserName) {
        Wait.until(String.format("creation of Secret %s/%s", namespace, kafkaUserName),
            TestFrameConstants.GLOBAL_POLL_INTERVAL_1_SEC, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM,
            () -> {
                Secret secret = ResourceUtils.getKubeResource(Secret.class, namespace, kafkaUserName);
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
    public static void waitForPodsReadyAndStable(String namespaceName, LabelSelector selector, int expectPods, boolean containers) {
        waitForPodsReady(namespaceName, selector, expectPods, containers, () -> { });
        waitForPodStability(namespaceName, selector, expectPods);
    }

    /**
     * Waits until all pods matching the given label selector are ready, including optional container readiness.
     * Invokes the provided {@code onTimeout} callback if the timeout is reached.
     *
     * @param namespaceName the namespace in which the pods should exist
     * @param selector      the label selector used to find matching pods
     * @param expectPods    the expected number of pods
     * @param checkContainers    whether to verify that all containers are also ready
     * @param onTimeout     a runnable to invoke if the readiness check times out
     */
    public static void waitForPodsReady(String namespaceName, LabelSelector selector, int expectPods, boolean checkContainers, Runnable onTimeout) {
        Wait.until("readiness of all Pods matching: " + selector,
            TimeConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM,
            () -> arePodsReady(namespaceName, selector, expectPods, checkContainers),
            onTimeout
        );
    }

    public static void waitForPodStability(String namespaceName, LabelSelector selector, int expectedPods) {
        LOGGER.info("Waiting for Pod in namespace {} with selector {} to have {} stable replicas", namespaceName, selector, expectedPods);
        int[] stableCounter = {0};
        Wait.until("Pod: " + namespaceName + " with selector" + selector + " to be stable",
            TestFrameConstants.GLOBAL_POLL_INTERVAL_SHORT, TestFrameConstants.GLOBAL_TIMEOUT,
            () -> {
                if (ResourceUtils.listKubeResourcesByLabelSelector(Pod.class, namespaceName, selector).size() == expectedPods &&
                    arePodsReady(namespaceName, selector, expectedPods, true)) {
                    stableCounter[0]++;
                    if (stableCounter[0] == TimeConstants.GLOBAL_STABILITY_OFFSET_TIME) {
                        LOGGER.info("Pod replicas are stable for {} poll intervals", stableCounter[0]);
                        return true;
                    }
                } else {
                    LOGGER.info("Pod replicas are not stable. Resetting counter to zero");
                    stableCounter[0] = 0;
                    return false;
                }
                LOGGER.info("Pod replicas will be assumed stable in {} polls", TimeConstants.GLOBAL_STABILITY_OFFSET_TIME - stableCounter[0]);
                return false;
            });
    }

    /**
     * Checks if the expected number of Pods in the given namespace are ready, based on the provided label selector.
     * <p>
     * This method retrieves all Pods matching the label selector and verifies:
     * <ul>
     *     <li>The total number of Pods matches the expected count.</li>
     *     <li>Each Pod is in the Ready state.</li>
     *     <li>If {@code checkContainers} is true, it also verifies that all containers in each Pod are ready.</li>
     * </ul>
     * </p>
     *
     * @param namespaceName   the namespace in which to look for Pods
     * @param selector        the label selector used to filter the Pods
     * @param expectPods      the expected number of Pods
     * @param checkContainers whether to also check if all containers inside the Pods are ready
     * @return {@code true} if all expected Pods are ready (and containers, if checked), {@code false} otherwise
     */
    private static boolean arePodsReady(String namespaceName, LabelSelector selector, int expectPods, boolean checkContainers) {
        List<Pod> pods = ResourceUtils.listKubeResourcesByLabelSelector(Pod.class, namespaceName, selector);
        if (pods.isEmpty() && pods.size() != expectPods) {
            LOGGER.debug("Expected pods: {}/{} are not ready", namespaceName, selector);
            return false;
        }

        for (Pod pod : pods) {
            if (!isPodAndContainersReady(pod, namespaceName, checkContainers)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Checks whether the given Pod is ready, and optionally verifies that all containers within the Pod are ready.
     * <p>
     * The readiness of the Pod itself is checked first using {@link Readiness#isPodReady(Pod)}.
     * If {@code checkContainers} is {@code true}, the readiness of each container within the Pod
     * is also validated.
     * </p>
     *
     * @param pod             the Pod object to check
     * @param namespaceName   the namespace of the Pod, used for logging purposes
     * @param checkContainers if {@code true}, also checks readiness of each container inside the Pod
     * @return {@code true} if the Pod (and optionally all containers) are ready, {@code false} otherwise
     */
    private static boolean isPodAndContainersReady(Pod pod, String namespaceName, boolean checkContainers) {
        if (!Readiness.isPodReady(pod)) {
            LOGGER.debug("Pod not ready: {}/{}", namespaceName, pod.getMetadata().getName());
            return false;
        }

        if (checkContainers) {
            for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                if (Boolean.FALSE.equals(cs.getReady())) {
                    LOGGER.debug("Container: {} of Pod: {}/{} not ready", namespaceName, pod.getMetadata().getName(), cs.getName());
                    return false;
                }
            }
        }

        return true;
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
    public static void waitForKafkaHasAnnotationWithValue(String namespaceName, String clusterName, String annotationKey, String annotationValue) {
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
     * Waits until a Kafka custom resource in the given namespace no longer has the specified annotation key.
     * <p>
     * This method polls the Kafka resource's annotations at regular intervals and completes
     * when the annotation key is no longer present or the annotations are empty.
     * It uses a short global timeout and polling interval.
     * </p>
     *
     * @param namespaceName the namespace where the Kafka cluster resides
     * @param clusterName   the name of the Kafka cluster
     * @param annotationKey the annotation key to check for removal
     *
     * @throws AssertionError if the annotation key still exists after the timeout
     */
    public static void waitForKafkaHasNoAnnotationWithKey(String namespaceName, String clusterName, String annotationKey) {
        Wait.until(String.format("Kafka %s/%s has annotation %s", namespaceName, clusterName, annotationKey),
            TestFrameConstants.GLOBAL_POLL_INTERVAL_SHORT, TestFrameConstants.GLOBAL_TIMEOUT_SHORT,
            () -> {
                Map<String, String> anno = ResourceUtils.getKubeResource(Kafka.class, namespaceName, clusterName).getMetadata().getAnnotations();
                if (anno.isEmpty()) {
                    return true;
                }
                return !anno.containsKey(annotationKey);
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

    public static void waitForClientsSuccess(KafkaClients clients) {
        waitForClientsSuccess(clients.getNamespaceName(), clients.getProducerName(), clients.getConsumerName(), clients.getMessageCount(), true);
    }

    public static void waitForClientsSuccess(String namespace, String producerName, String consumerName, int messageCount, boolean deleteAfterSuccess) {
        LOGGER.info("Waiting for producer: {}/{} and consumer: {}/{} Jobs to finish successfully", namespace, producerName, namespace, consumerName);
        waitForClientSuccess(namespace, producerName, messageCount, deleteAfterSuccess);
        waitForClientSuccess(namespace, consumerName, messageCount, deleteAfterSuccess);
    }

    /**
     * Waits for a Kubernetes Job (representing a Kafka client) to complete successfully,
     * and optionally deletes the Job resource after successful completion.
     * <p>
     * This method polls the Job status periodically using {@code JobUtils.checkSucceededJobStatus}
     * until the Job has succeeded or the timeout (based on message count) is reached.
     * Logs the current Job status during waiting via {@code JobUtils.logCurrentJobStatus}.
     * </p>
     *
     * @param namespaceName       the namespace in which the Job is deployed
     * @param jobName             the name of the Job resource
     * @param messageCount        the expected number of messages (used to calculate the timeout duration)
     * @param deleteAfterSuccess  if true, deletes the Job resource after successful completion
     *
     * @throws AssertionError if the Job does not succeed within the timeout window
     */
    public static void waitForClientSuccess(String namespaceName, String jobName, int messageCount, boolean deleteAfterSuccess) {
        LOGGER.info("Waiting for client Job: {}/{} to finish successfully", namespaceName, jobName);
        Wait.until("client Job to finish successfully", TestFrameConstants.GLOBAL_POLL_INTERVAL_SHORT, TimeConstants.timeoutForClientFinishJob(messageCount),
            () -> JobUtils.checkSucceededJobStatus(namespaceName, jobName, 1),
            () -> JobUtils.logCurrentJobStatus(namespaceName, jobName));

        if (deleteAfterSuccess) {
            KubeResourceManager.get().deleteResourceWithWait(ResourceUtils.getKubeResource(Job.class, namespaceName, jobName));
        }
    }

    /**
     * Waits until the specified KafkaTopic resource in the given namespace has a non-null Topic ID,
     * then returns that ID.
     * <p>
     * This method continuously polls the KafkaTopic resource using {@code ResourceUtils.getKubeResource}
     * until the {@code status.topicId} field is populated or a timeout is reached.
     * </p>
     *
     * @param namespace the Kubernetes namespace where the KafkaTopic resides
     * @param topicName the name of the KafkaTopic resource to monitor
     * @return the {@code topicId} from the KafkaTopic's status once it becomes available
     *
     * @throws AssertionError if the topic ID is not available within the configured timeout
     */
    public static String waitForKafkaTopicToHaveIdAndReturn(String namespace, String topicName) {
        Wait.until(String.format("KafkaTopic %s/%s to have an ID", namespace, topicName),
            TimeConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS, TimeConstants.GLOBAL_STATUS_TIMEOUT,
            () -> {
                KafkaTopic topic = ResourceUtils.getKubeResource(KafkaTopic.class, namespace, topicName);
                return topic != null && topic.getStatus() != null && topic.getStatus().getTopicId() != null;
            });

        return ResourceUtils.getKubeResource(KafkaTopic.class, namespace, topicName).getStatus().getTopicId();
    }
}