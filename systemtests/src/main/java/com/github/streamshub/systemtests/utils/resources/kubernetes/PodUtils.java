package com.github.streamshub.systemtests.utils.resources.kubernetes;

import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.resources.ResourceOperation;
import com.github.streamshub.systemtests.utils.CommonUtils;
import io.fabric8.kubernetes.api.model.ContainerStatus;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.PodResource;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class PodUtils {

    private static final Logger LOGGER = LogManager.getLogger(PodUtils.class);

    // -------------
    // Client
    // -------------
    private static MixedOperation<Pod, PodList, PodResource> podClient() {
        return KubeResourceManager.getKubeClient().getClient().pods();
    }

    // -------------
    // Get
    // -------------
    public static Pod getPod(String namespace, String name) {
        return podClient().inNamespace(namespace).withName(name).get();
    }

    public static Pod getPodByPrefix(String namespace, String prefix) {
        List<Pod> pods = podClient().inNamespace(namespace).list().getItems();
        if (!pods.isEmpty()) {
            List<Pod> foundPods = pods.stream().filter(p -> p.getMetadata().getName().startsWith(prefix)).toList();
            if (!foundPods.isEmpty()) {
                return foundPods.get(0);
            }
            return null;
        }
        return null;
    }

    public static String getPodNameByPrefix(String namespace, String prefix) {
        return getPodByPrefix(namespace, prefix).getMetadata().getName();
    }

    public static String getLogFromPod(String namespace, String podName) {
        return podClient().inNamespace(namespace).withName(podName).getLog();
    }

    public static String getLogFromContainer(String namespace, String podName, String containerName) {
        return podClient().inNamespace(namespace).withName(podName).inContainer(containerName).getLog();
    }

    // -------------
    // List
    // -------------
    public static List<Pod> listPods(String namespaceName) {
        return podClient().inNamespace(namespaceName).list().getItems();
    }

    public static List<Pod> listPods(String namespaceName, Map<String, String> labelSelector) {
        return podClient().inNamespace(namespaceName).withLabels(labelSelector).list().getItems();
    }

    public static List<Pod> listPods(String namespaceName, LabelSelector selector) {
        return podClient().inNamespace(namespaceName).withLabelSelector(selector).list().getItems();
    }

    public static List<Pod> listPodsByPrefixInName(String namespace, String prefixName) {
        return listPods(namespace)
            .stream().filter(p -> p.getMetadata().getName().startsWith(prefixName))
            .collect(Collectors.toList());
    }

    public static List<String> listPodNamesInSpecificNamespace(String namespaceName, String key, String value) {
        return listPods(namespaceName, Collections.singletonMap(key, value)).stream()
            .map(pod -> pod.getMetadata().getName())
            .collect(Collectors.toList());
    }

    // -------------
    // ReadyStatus
    // -------------
    public static boolean isReady(String namespace, String podName) {
        return podClient().inNamespace(namespace).withName(podName).isReady();
    }

    // -------------
    // Wait
    // -------------
    public static void waitForPodsReady(String namespaceName, LabelSelector selector, int expectPods, boolean containers) {
        waitForPodsReady(namespaceName, selector, expectPods, containers, () -> { });
    }

    public static void waitForPodsReady(String namespaceName, LabelSelector selector, int expectPods, boolean containers, Runnable onTimeout) {
        CommonUtils.waitFor("readiness of all Pods matching: " + selector,
            TimeConstants.POLL_INTERVAL_FOR_RESOURCE_READINESS, ResourceOperation.timeoutForPodsOperation(expectPods),
            () -> {
                List<Pod> pods = listPods(namespaceName, selector);
                if (pods.isEmpty() && expectPods == 0) {
                    LOGGER.debug("Expected Pods are ready");
                    return true;
                }
                if (pods.isEmpty()) {
                    LOGGER.debug("Pods matching: {}/{} are not ready", namespaceName, selector);
                    return false;
                }
                if (pods.size() != expectPods) {
                    LOGGER.debug("Expected Pods: {}/{} are not ready - ready: {} out of {}", namespaceName, selector, pods.size(), expectPods);
                    return false;
                }
                for (Pod pod : pods) {
                    if (!Readiness.isPodReady(pod)) {
                        LOGGER.debug("Pod not ready: {}/{}", namespaceName, pod.getMetadata().getName());
                        return false;
                    } else {
                        if (containers) {
                            for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                                if (!Boolean.TRUE.equals(cs.getReady())) {
                                    LOGGER.debug("Container: {} of Pod: {}/{} not ready", namespaceName, pod.getMetadata().getName(), cs.getName());
                                    return false;
                                }
                            }
                        }
                    }
                }
                return true;
            }, onTimeout);
    }

    public static void waitForPodRoll(String namespace, String podName, String oldUid) {
        CommonUtils.waitFor("Pod to roll " + namespace + "/" + podName, TimeConstants.GLOBAL_POLL_INTERVAL, TimeConstants.GLOBAL_TIMEOUT,
            () -> {
                Pod pod = getPodByPrefix(namespace, podName);
                if (pod != null) {
                    return !Objects.equals(oldUid, pod.getMetadata().getUid());
                }
                LOGGER.debug("Pod {}/{} has not rolled yet", namespace, podName);
                return false;
            }
        );
    }

    public static void waitForPodRoll(String namespace, String podName, Map<String, String> podSnapshot) {
        waitForPodRoll(namespace, podName, podSnapshot, TimeConstants.GLOBAL_POLL_INTERVAL, TimeConstants.GLOBAL_TIMEOUT);
    }

    public static void waitForPodRoll(String namespace, String podName, Map<String, String> podSnapshot, long poll, long timeout) {
        CommonUtils.waitFor("Pod to roll " + namespace + "/" + podName, poll, timeout,
            () -> {
                Pod pod = getPodByPrefix(namespace, podName);
                if (pod != null) {
                    return !Objects.equals(podSnapshot, podSnapshot(pod));
                }
                return false;
            }
        );
    }

    public static void waitForLogInPod(String namespace, String podName, String containerName, String containsLog) {
        CommonUtils.waitFor("pod " + namespace + "/" + podName  + "/" + containerName + " to contain log " + containsLog, TimeConstants.GLOBAL_POLL_INTERVAL_MEDIUM, TimeConstants.GLOBAL_TIMEOUT,
            () -> {
                // First check pod is present and ready
                Pod pod = PodUtils.getPod(namespace, podName);
                if (pod == null || !PodUtils.isReady(namespace, podName)) {
                    LOGGER.warn("Pod {}/{} is not ready, cannot get container logs", namespace, podName);
                    return false;
                }
                // Then check container started
                List<ContainerStatus> containerStatus = pod.getStatus().getContainerStatuses().stream()
                    .filter(cs -> cs.getName().equals(containerName))
                    .toList();

                if (containerStatus.isEmpty() || !containerStatus.get(0).getReady() || !containerStatus.get(0).getStarted() || containerStatus.get(0).getState().getRunning() == null || containerStatus.get(0).getState().getWaiting() != null) {
                    LOGGER.warn("Pod {}/{} does not have container prepared", namespace, podName);
                    return false;
                }

                // Then check it's log is present
                String log = PodUtils.getLogFromContainer(namespace, podName, containerName);
                if (log == null) {
                    LOGGER.warn("Pod {}/{} container {} does not contain any log yet", namespace, podName, containerName);
                    return false;
                }
                // Finally check inside of the log
                return log.contains(containsLog);
            },
            () -> LOGGER.warn("Pod {}/{} contains log: {}", namespace, podName,  PodUtils.getLogFromContainer(namespace, podName, containerName))
        );
    }

    // -------------
    // Delete
    // -------------
    public static void deletePodByPrefix(String namespace, String prefix) {
        podClient().inNamespace(namespace).resource(getPodByPrefix(namespace, prefix)).delete();
    }

    // -------------
    // Snapshot Util
    // -------------
    public static Map<String, String> podSnapshot(String namespaceName, LabelSelector selector) {
        return listPods(namespaceName, selector).stream()
            .collect(
                Collectors.toMap(pod -> pod.getMetadata().getName(),
                    pod -> pod.getMetadata().getUid()));
    }

    public static Map<String, String> podSnapshot(Pod pod) {
        return Map.of(pod.getMetadata().getName(), pod.getMetadata().getUid());
    }
}
