package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.logs.LogWrapper;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Pod;
import io.skodjob.testframe.TestFrameConstants;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class PodUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(PodUtils.class);

    private PodUtils() {}

    public static Map<String, String> getPodSnapshotBySelector(String namespace, LabelSelector podSelector) {
        return ResourceUtils.listKubeResourcesByLabelSelector(Pod.class, namespace, podSelector)
            .stream().collect(Collectors.toMap(pod -> pod.getMetadata().getName(), pod -> pod.getMetadata().getUid()));
    }

    public static boolean componentPodsHaveRolled(String namespace, LabelSelector selector, Map<String, String> previousSnapshot) {
        LOGGER.debug("Previous snapshot: {}", new TreeMap<>(previousSnapshot));
        Map<String, String> currentSnapshot = PodUtils.getPodSnapshotBySelector(namespace, selector);
        LOGGER.debug("Current snapshot: {}", new TreeMap<>(currentSnapshot));

        // Filter only currently available for roll
        currentSnapshot.keySet().retainAll(previousSnapshot.keySet());
        LOGGER.debug("Pod snapshots to verify: {}", new TreeMap<>(currentSnapshot));

        for (Map.Entry<String, String> currentPodSnapshot : currentSnapshot.entrySet()) {
            if (previousSnapshot.get(currentPodSnapshot.getKey()).equals(currentPodSnapshot.getValue())) {
                LOGGER.debug("Pod {}/{} has not been rolled yet", namespace, currentPodSnapshot.getKey());
                return false;
            }
        }
        LOGGER.debug("All Pods have rolled");
        return true;
    }

    public static long getTimeoutForPodOperations(int numberOfPods) {
        return TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM * Math.max(1, numberOfPods);
    }
}
