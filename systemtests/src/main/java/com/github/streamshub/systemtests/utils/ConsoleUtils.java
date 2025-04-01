package com.github.streamshub.systemtests.utils;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.systemtests.Environment;
import io.skodjob.testframe.resources.KubeResourceManager;

public class ConsoleUtils {
    public static String getConsoleOperatorName() {
        return Environment.CONSOLE_DEPLOYMENT_NAME + "-operator";
    }
    public static void removeFinalizersInAllConsoleInstances() {
        ResourceUtils.listKubeResourceInAllNamespaces(Console.class).forEach(console ->
            KubeResourceManager.get().updateResource(console)
        );
    }
}
