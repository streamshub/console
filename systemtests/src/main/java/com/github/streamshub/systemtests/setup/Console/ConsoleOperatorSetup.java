package com.github.streamshub.systemtests.setup.Console;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.ResourceUtils;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.apache.logging.log4j.Logger;

public class ConsoleOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(ConsoleOperatorSetup.class);

    private InstallConfig installConfig;

    public ConsoleOperatorSetup() {
        switch (Environment.CONSOLE_INSTALL_TYPE) {
            case Yaml -> installConfig = new YamlConfig();
            case Olm -> installConfig = new OlmConfig();
            case Unknown -> throw new SetupException("Unknown installation type: " + Environment.CONSOLE_INSTALL_TYPE);
        }
    }

    public void install() {
        LOGGER.info("----------- Install Console Operator -----------");
        if (!ResourceUtils.listKubeResourcesByPrefix(Deployment.class, installConfig.getDeploymentNamespace(), installConfig.getDeploymentName()).isEmpty()) {
            LOGGER.warn("Console Operator is already deployed. Skipping deployment");
            return;
        }
        installConfig.install();
    }

    public ConsoleOperatorSetup inNamespace(String namespace) {
        installConfig.setDeploymentNamespace(namespace);
        return this;
    }

    public ConsoleOperatorSetup withName(String name) {
        installConfig.setDeploymentName(name);
        return this;
    }
}
