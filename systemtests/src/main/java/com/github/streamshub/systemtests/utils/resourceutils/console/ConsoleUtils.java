package com.github.streamshub.systemtests.utils.resourceutils.console;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.dependents.ConsoleDeployment;
import com.github.streamshub.console.dependents.ConsoleIngress;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ConsoleUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(ConsoleUtils.class);

    private ConsoleUtils() {}

    public static String getConsoleUiUrl(String consoleInstanceName, boolean httpSecure) {
        return (httpSecure ? "https" : "http") + "://" + getConsoleUiHostname(consoleInstanceName);
    }

    public static String getConsoleUiHostname(String consoleInstanceName) {
        return consoleInstanceName + "." + ClusterUtils.getClusterDomain();
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

    public static void patchConsoleNginxBuffer(String namespace, String consoleInstance) {
        if (!ClusterUtils.isOcp()) {
            LOGGER.info("Edit console ingress, annotate for bigger buffer");
            WaitUtils.waitForIngressToBePresent(namespace, consoleInstance + "-" + ConsoleIngress.NAME);
            Ingress consoleIngress = ResourceUtils.getKubeResource(Ingress.class, namespace, consoleInstance + "-" + ConsoleIngress.NAME);
            // Add nginx ingress annotation to increase buffer
            // This is required to correctly receive bigger header containing keycloak token - session cookie
            consoleIngress = consoleIngress.edit()
                .editMetadata()
                .addToAnnotations("nginx.ingress.kubernetes.io/proxy-buffer-size", "16k")
                .addToAnnotations("nginx.ingress.kubernetes.io/proxy-buffers-number", "8")
                .addToAnnotations("nginx.ingress.kubernetes.io/proxy-busy-buffers-size", "16k")
                .endMetadata()
                .build();

            KubeResourceManager.get()
                .createOrUpdateResourceWithWait(consoleIngress);
        }
    }
}
