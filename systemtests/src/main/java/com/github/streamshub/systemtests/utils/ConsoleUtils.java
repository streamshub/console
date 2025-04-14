package com.github.streamshub.systemtests.utils;

import io.fabric8.openshift.api.model.Route;

public class ConsoleUtils {
    private ConsoleUtils() {}

    public static String getConsoleUiUrl(String namespace, String consoleInstanceName, boolean httpSecure) {
        return (httpSecure ? "https" : "http") + "://" +
            (ClusterUtils.isOcp() ?
                ResourceUtils.listKubeResourcesByPrefix(Route.class, namespace, consoleInstanceName).get(0).getSpec().getHost() :
                consoleInstanceName + "." + ClusterUtils.getDefaultClusterHostname()
            );
    }
}
