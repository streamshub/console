package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.ClusterUnreachableException;
import io.fabric8.openshift.api.model.config.v1.DNS;
import io.skodjob.testframe.executor.ExecResult;
import io.skodjob.testframe.resources.KubeResourceManager;

import java.util.Locale;

public class ClusterUtils {
    private ClusterUtils() {}

    public static void checkClusterHealth() {
        ExecResult result = KubeResourceManager.get().kubeCmdClient().exec(false, false, "cluster-info");
        // Minikube on linux could throw ansi colors
        String output = result.out().replaceAll("\u001B\\[[;\\d]*m", "").toLowerCase(Locale.ENGLISH);

        if (!result.exitStatus() || !output.contains("kubernetes control plane is running") || output.toLowerCase(Locale.ENGLISH).contains("error")) {
            throw new ClusterUnreachableException(result);
        }
    }

    public static boolean isOcp() {
        return KubeResourceManager.get().kubeCmdClient().exec(false, false, "api-versions").out().contains("openshift.io");
    }

    public static String getClusterDomain() {
        if (isOcp()) {
            return "apps." + ResourceUtils.getKubeResource(DNS.class, "cluster").getSpec().getBaseDomain();
        }
        return Environment.CONSOLE_CLUSTER_DOMAIN;
    }
}
