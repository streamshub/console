package com.github.streamshub.systemtests.utils.resourceutils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.Logger;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.ClusterUnreachableException;
import com.github.streamshub.systemtests.logs.LogWrapper;

import io.fabric8.openshift.api.model.config.v1.DNS;
import io.skodjob.kubetest4j.executor.ExecResult;
import io.skodjob.kubetest4j.resources.KubeResourceManager;

public class ClusterUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(ClusterUtils.class);
    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>(2);

    private ClusterUtils() {}

    public static void checkClusterHealth() {
        LOGGER.trace("Checking cluster health via 'cluster-info'");
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
        return (Boolean) CACHE.computeIfAbsent("isOCP", k -> {
            boolean isOcp = KubeResourceManager.get().kubeCmdClient().exec(false, false, "api-versions").out().contains("openshift.io");
            LOGGER.info("Cluster type detected as {}", isOcp ? "OpenShift" : "Kubernetes");
            return isOcp;
        });
    }

    public static String getClusterDomain() {
        return (String) CACHE.computeIfAbsent("clusterDomain", k -> {
            String domain = Environment.CONSOLE_CLUSTER_DOMAIN;

            if (domain.isBlank()) {
                if (isOcp()) {
                    var baseDomain = ResourceUtils.getKubeResource(DNS.class, "cluster").getSpec().getBaseDomain();
                    domain = "apps." + baseDomain;
                    LOGGER.info("CONSOLE_CLUSTER_DOMAIN was not set, derived domain '{}' from OpenShift base domain: {}", domain, baseDomain);
                } else {
                    var masterUrl = KubeResourceManager.get().kubeClient().getClient().getMasterUrl();
                    var masterHost = masterUrl.getHost();

                    try {
                        var address = InetAddress.getByName(masterHost);
                        domain = address.getHostAddress() + ".nip.io";
                        LOGGER.info("CONSOLE_CLUSTER_DOMAIN was not set, derived domain '{}' from Kubernetes master URL: {}", domain, masterUrl);
                    } catch (UnknownHostException e) {
                        throw new IllegalStateException("Environment variable CONSOLE_CLUSTER_DOMAIN must be set for non-OpenShift clusters");
                    }
                }
            } else {
                LOGGER.info("Using configured cluster domain: {}", domain);
            }

            return domain;
        });
    }
}
