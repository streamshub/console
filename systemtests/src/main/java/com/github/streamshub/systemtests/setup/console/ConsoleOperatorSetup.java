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


    /**
     * Installs the Console Operator.
     *
     * <p>Deletion of an existing operator deployment before install is controlled
     * by two independent gates — both must be {@code true} for deletion to occur:
     * <ul>
     *   <li>{@code deleteBeforeInstall} — set by the caller. Pass {@code false} to
     *       skip deletion unconditionally, e.g. in upgrade tests where the existing
     *       deployment must be preserved to verify in-place upgrades.</li>
     *   <li>{@link Environment#DELETE_CONSOLE_OPERATOR_BEFORE_INSTALL} — set via
     *       environment variable. Set to {@code false} when debugging to prevent
     *       the operator from being redeployed between test runs.</li>
     * </ul>
     *
     * @param deleteBeforeInstall whether the caller wants the existing operator
     *                            deleted before installing; when {@code false} the
     *                            env var is ignored entirely
     */
    public void install(boolean deleteBeforeInstall) {
        LOGGER.info("----------- Install Console Operator -----------");
        if (deleteBeforeInstall && Environment.DELETE_CONSOLE_OPERATOR_BEFORE_INSTALL &&
            !ResourceUtils.listKubeResourcesByPrefix(Deployment.class, installConfig.getDeploymentNamespace(), installConfig.getDeploymentName()).isEmpty()) {

            LOGGER.info("Clean up currently deployed operator to ensure correct operator is deployed");
            installConfig.delete();
        }

        installConfig.install();
    }
}
