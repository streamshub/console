package com.github.streamshub.systemtests.utils.resourceutils;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.ClusterUnreachableException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.fabric8.openshift.api.model.config.v1.DNS;
import io.skodjob.kubetest4j.executor.ExecResult;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

public class ClusterUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(ClusterUtils.class);

    private ClusterUtils() {}

    public static void checkClusterHealth() {
        LOGGER.debug("Checking cluster health via 'cluster-info'");
        ExecResult result = KubeResourceManager.get().kubeCmdClient().exec(false, false, "cluster-info");
        // Minikube on linux could throw ansi colors
        String output = result.out().replaceAll("\u001B\\[[;\\d]*m", "").toLowerCase(Locale.ENGLISH);

        if (!result.exitStatus() || !output.contains("kubernetes control plane is running") || output.toLowerCase(Locale.ENGLISH).contains("error")) {
            LOGGER.error("Cluster health check failed, exitStatus={}, output={}", result.exitStatus(), output);
            throw new ClusterUnreachableException(result);
        }
        LOGGER.info("Cluster health check passed, Kubernetes control plane is running");
    }

    public static boolean isOcp() {
        boolean isOcp = KubeResourceManager.get().kubeCmdClient().exec(false, false, "api-versions").out().contains("openshift.io");
        LOGGER.debug("Cluster type detected as {}", isOcp ? "OpenShift" : "Kubernetes");
        return isOcp;
    }

    public static String getClusterDomain() {
        if (isOcp()) {
            String domain = "apps." + ResourceUtils.getKubeResource(DNS.class, "cluster").getSpec().getBaseDomain();
            LOGGER.debug("Resolved OpenShift cluster domain: {}", domain);
            return domain;
        }
        LOGGER.debug("Using configured cluster domain: {}", Environment.CONSOLE_CLUSTER_DOMAIN);
        return Environment.CONSOLE_CLUSTER_DOMAIN;
    }
}
