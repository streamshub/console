package com.github.streamshub.systemtests.setup.console;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import org.apache.logging.log4j.Logger;

public class ConsoleOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(ConsoleOperatorSetup.class);

    private InstallConfig installConfig;

    public ConsoleOperatorSetup(String namespace) {
        switch (Environment.CONSOLE_INSTALL_TYPE) {
            case Yaml -> installConfig = new YamlConfig(namespace, Environment.CONSOLE_OPERATOR_BUNDLE_URL);
            case Olm -> installConfig = new OlmConfig(namespace);
            default -> throw new SetupException("Unknown installation type: " + Environment.CONSOLE_INSTALL_TYPE);
        }
    }

    public void setInstallConfig(InstallConfig installConfig) {
        this.installConfig = installConfig;
    }

    public void install() {
        install(true);
    }

    public void install(boolean deleteResourcesBeforeInstall) {
        LOGGER.info("----------- Install Console Operator -----------");
        if (deleteResourcesBeforeInstall &&
            !ResourceUtils.listKubeResourcesByPrefix(Deployment.class, installConfig.getDeploymentNamespace(), installConfig.getDeploymentName()).isEmpty()) {

            LOGGER.info("Clean up currently deployed operator to ensure correct operator is deployed");
            installConfig.delete();
        }

        installConfig.install();
    }
}
