package com.github.streamshub.systemtests.utils.resourceutils.console;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.dependents.ConsoleDeployment;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.openshift.api.model.Route;
import io.skodjob.testframe.resources.KubeResourceManager;

import java.util.List;

public class ConsoleUtils {
    private ConsoleUtils() {}

    public static String getConsoleUiUrl(String namespace, String consoleInstanceName, boolean httpSecure) {
        return (httpSecure ? "https" : "http") + "://" +
            (ClusterUtils.isOcp() ?
                ResourceUtils.listKubeResourcesByPrefix(Route.class, namespace, consoleInstanceName).get(0).getSpec().getHost() :
                consoleInstanceName + "." + ClusterUtils.getClusterDomain()
            );
    }

    public static String getConsoleDeploymentName(String instanceName) {
        return instanceName + "-" + ConsoleDeployment.NAME;
    }

    public static void removeConsoleInstancesFinalizers(String namespace) {
        List<Console> consoleInstances = ResourceUtils.getKubeResourceClient(Console.class).inNamespace(namespace).list().getItems();

        for (Console instance: consoleInstances) {
            instance.getMetadata().setFinalizers(null);
            KubeResourceManager.get().createOrUpdateResourceWithWait(instance);
        }
    }
}
