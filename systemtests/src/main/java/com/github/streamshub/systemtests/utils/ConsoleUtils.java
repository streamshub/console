package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.logs.LogWrapper;
import io.fabric8.openshift.api.model.Route;
import org.apache.logging.log4j.Logger;

public class ConsoleUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(ConsoleUtils.class);
    private ConsoleUtils() {}

    public static String getConsoleUiUrl(String namespace, String consoleInstanceName, boolean httpSecure) {
        return (httpSecure ? "https" : "http") + "://" +
            (ClusterUtils.isOcp() ?
                ResourceUtils.listKubeResourcesByPrefix(Route.class, namespace, consoleInstanceName).get(0).getSpec().getHost() :
                consoleInstanceName + "." + ClusterUtils.getClusterDomain()
            );
    }
}
