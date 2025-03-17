package com.github.streamshub.systemtests.cluster;

import com.github.streamshub.systemtests.cluster.exception.KubeClusterException;
import com.github.streamshub.systemtests.cluster.exception.NoClusterException;
import io.skodjob.testframe.executor.Exec;
import io.skodjob.testframe.executor.ExecResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class Kubernetes {

    public static final String CMD = "kubectl";
    private static final Logger LOGGER = LogManager.getLogger(Kubernetes.class);
    private static Kubernetes instance;

    private Kubernetes() {
        if (isAvailable()) {
            LOGGER.info("kubectl is installed");
            if (isClusterUp()) {
                LOGGER.debug("Cluster is running");
            } else {
                throw new NoClusterException("Cluster is not running");
            }
        } else {
            throw new NoClusterException("Unable to find a cluster");
        }
    }

    public static synchronized Kubernetes getInstance() {
        if (instance == null) {
            instance = new Kubernetes();
        }
        return instance;
    }

    public boolean isAvailable() {
        return Exec.isExecutableOnPath(CMD);
    }

    public boolean isClusterUp() {
        List<String> cmd = Arrays.asList(CMD, "cluster-info");
        try {
            return Exec.exec(cmd).returnCode() == 0;
        } catch (KubeClusterException e) {
            String commandLine = String.join(" ", cmd);
            LOGGER.error("'{}' failed. Please double check connectivity to your cluster!", commandLine, e);
            return false;
        }
    }

    public String toString() {
        return CMD;
    }

    public boolean isOpenshift() {
        List<String> cmd = Arrays.asList(CMD, "api-versions");
        try {
            ExecResult result = Exec.exec(cmd);
            if (!(result.returnCode() == 0)) {
                throw new KubeClusterException("Something went wrong when executing " + cmd + " command: " + result.err());
            }
            return result.out().contains("openshift.io");
        } catch (KubeClusterException e) {
            LOGGER.error("Failed whilst sniffing for OpenShift: ", e);
            throw e;
        }
    }

    public boolean isMinikube() {
        List<String> cmd = Arrays.asList("kubectl", "get", "nodes", "-o", "jsonpath='{.items[*].metadata.labels}'");
        try {
            return Exec.exec(cmd).out().contains("minikube.k8s.io");
        } catch (KubeClusterException e) {
            LOGGER.debug("'" + String.join(" ", cmd) + "' failed. Please double check connectivity to your cluster!");
            LOGGER.debug(String.valueOf(e));
            return false;
        }
    }
}
