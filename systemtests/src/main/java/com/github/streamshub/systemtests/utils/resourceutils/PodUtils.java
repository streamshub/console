package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.logs.LogWrapper;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Pod;
import io.skodjob.kubetest4j.KubeTestConstants;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class PodUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(PodUtils.class);

    private PodUtils() {}

    /**
     * Retrieves a snapshot of the current pods in the given namespace that match the specified label selector.
     * <p>
     * The snapshot is represented as a map where the keys are pod names and the values are their UIDs.
     *
     * @param namespace   the Kubernetes namespace to search in
     * @param podSelector the label selector to filter pods
     * @return a map of pod names to their UIDs
     */
    public static Map<String, String> getPodSnapshotBySelector(String namespace, LabelSelector podSelector) {
        Map<String, String> snapshot = ResourceUtils.listKubeResourcesByLabelSelector(Pod.class, namespace, podSelector)
            .stream().collect(Collectors.toMap(pod -> pod.getMetadata().getName(), pod -> pod.getMetadata().getUid()));
        LOGGER.trace("Pod snapshot in namespace {} with selector {} contains {} pod(s)", namespace, podSelector, snapshot.size());
        return snapshot;
    }

    /**
     * Determines whether all component pods identified by the label selector have rolled (i.e., restarted with new UIDs)
     * compared to the previously recorded snapshot.
     * <p>
     * This method compares the UIDs of pods with the same names from the previous snapshot. If all such UIDs differ,
     * it means the pods have rolled.
     *
     * @param namespace        the Kubernetes namespace containing the pods
     * @param selector         the label selector to identify the component pods
     * @param previousSnapshot a snapshot of pod names to UIDs taken before a rolling operation
     * @return {@code true} if all relevant pods have rolled, {@code false} otherwise
     */
    public static boolean componentPodsHaveRolled(String namespace, LabelSelector selector, Map<String, String> previousSnapshot) {
        LOGGER.trace("Previous snapshot: {}", new TreeMap<>(previousSnapshot));
        Map<String, String> currentSnapshot = PodUtils.getPodSnapshotBySelector(namespace, selector);
        LOGGER.trace("Current snapshot: {}", new TreeMap<>(currentSnapshot));

        // Filter only currently available for roll
        currentSnapshot.keySet().retainAll(previousSnapshot.keySet());
        LOGGER.trace("Pod snapshots to verify: {}", new TreeMap<>(currentSnapshot));

        for (Map.Entry<String, String> currentPodSnapshot : currentSnapshot.entrySet()) {
            if (previousSnapshot.get(currentPodSnapshot.getKey()).equals(currentPodSnapshot.getValue())) {
                LOGGER.trace("Pod {}/{} has not been rolled yet", namespace, currentPodSnapshot.getKey());
                return false;
            }
        }
        LOGGER.trace("All Pods have rolled");
        return true;
    }

    /**
     * Calculates a timeout value for operations on a specified number of pods.
     * <p>
     * The timeout is scaled linearly based on the number of pods and a global medium timeout constant.
     *
     * @param numberOfPods the number of pods the operation will act on
     * @return the calculated timeout value in milliseconds
     */
    public static long getTimeoutForPodOperations(int numberOfPods) {
        return KubeTestConstants.GLOBAL_TIMEOUT_MEDIUM * Math.max(1, numberOfPods);
    }
}
