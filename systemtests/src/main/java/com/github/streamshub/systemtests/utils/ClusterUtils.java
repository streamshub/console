package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.exceptions.ClusterUnreachableException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.config.v1.DNS;
import io.skodjob.testframe.executor.ExecResult;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;

public class ClusterUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(ClusterUtils.class);
    private ClusterUtils() {}

    public static void checkClusterHealth() {
        ExecResult result = KubeResourceManager.get().kubeCmdClient().exec(false, "cluster-info");
        // Minikube on linux could throw ansi colors
        String output = result.out().replaceAll("\u001B\\[[;\\d]*m", "").toLowerCase(Locale.ENGLISH);

        if (!result.exitStatus() || !output.contains("kubernetes control plane is running") || output.toLowerCase(Locale.ENGLISH).contains("error")) {
            throw new ClusterUnreachableException(result);
        }
    }

    public static boolean isOcp() {
        return KubeResourceManager.get().kubeCmdClient().exec(false, "api-versions").out().contains("openshift.io");
    }

    public static String getClusterDomain() {
        if (isOcp()) {
            return "apps." + ResourceUtils.getKubeResource(DNS.class, "cluster").getSpec().getBaseDomain();
        }
        return Constants.KUBE_DNS;
    }

    public static String getDefaultClusterHostname() {
        if (isOcp()) {
            return ResourceUtils.getKubeResource(Route.class, Constants.OPENSHIFT_CONSOLE, Constants.OPENSHIFT_CONSOLE_ROUTE_NAME)
                .getSpec()
                .getHost()
                .replace(Constants.OPENSHIFT_CONSOLE_ROUTE_NAME + "-" + Constants.OPENSHIFT_CONSOLE + ".", "");

        }
        return Constants.KUBE_DNS;
    }
}
