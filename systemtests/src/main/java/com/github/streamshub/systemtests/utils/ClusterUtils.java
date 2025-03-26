package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.exceptions.ClusterUnreachableException;
import io.skodjob.testframe.executor.ExecResult;
import io.skodjob.testframe.resources.KubeResourceManager;

import java.util.Locale;

public class ClusterUtils {
    private ClusterUtils() {}

    public static void checkClusterHealth() {
        ExecResult result = KubeResourceManager.getKubeCmdClient().exec(false, "cluster-info");
        if (!result.exitStatus() || !result.out().contains("Kubernetes control plane is running at") || result.out().toLowerCase(Locale.ENGLISH).contains("error")) {
            throw new ClusterUnreachableException(result);
        }
    }

    public static boolean isOcp() {
        return KubeResourceManager.getKubeCmdClient().exec(false, "api-versions").out().contains("openshift.io");
    }
}
