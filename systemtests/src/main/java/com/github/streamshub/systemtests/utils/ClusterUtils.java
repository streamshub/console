package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.exceptions.ClusterUnreachableException;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.config.v1.DNS;
import io.skodjob.testframe.executor.ExecResult;
import io.skodjob.testframe.resources.KubeResourceManager;

import java.util.Locale;

public class ClusterUtils {
    private ClusterUtils() {}

    public static void checkClusterHealth() {
        ExecResult result = KubeResourceManager.get().kubeCmdClient().exec(false, "cluster-info");
        if (!result.exitStatus() || !result.out().contains("Kubernetes control plane is running at") || result.out().toLowerCase(Locale.ENGLISH).contains("error")) {
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
                .replace(Constants.OPENSHIFT_CONSOLE_ROUTE_NAME + "-" + Constants.OPENSHIFT_CONSOLE, "");

        }
        return Constants.KUBE_DNS;
    }
}
