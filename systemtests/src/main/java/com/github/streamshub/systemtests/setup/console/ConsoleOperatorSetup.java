package com.github.streamshub.systemtests.setup.console;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.Logger;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.client.readiness.Readiness;
import io.skodjob.kubetest4j.resources.KubeResourceManager;

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

    /**
     * Edit the operator's environment variables using the provided EnvVar list editor.
     * The actual list of EnvVars will be updated on either an OLM Subscription or directly
     * on the Deployment depending on how the operator has been deployed.
     * 
     * This method will wait until the deployment is Ready=True and the expected EnvVars
     * are present.
     */
    public void editEnv(UnaryOperator<List<EnvVar>> editor) {
        var expectedEnv = installConfig.editEnv(editor);
        var k8s = KubeResourceManager.get().kubeClient().getClient();

        LOGGER.info("Waiting for operator deployment to have updated envs and be ready");
        k8s.apps().deployments()
            .inNamespace(installConfig.getDeploymentNamespace())
            .withName(installConfig.getDeploymentName())
            .waitUntilCondition(
                    deployment -> {
                        if (Readiness.isDeploymentReady(deployment)) {
                            var actualEnv = deployment.getSpec().getTemplate().getSpec().getContainers().get(0).getEnv();
                            if (actualEnv.containsAll(expectedEnv)) {
                                LOGGER.info("Operator deployment updated and ready: {}", actualEnv);
                                return true;
                            }
                            return false;
                        }

                        return false;
                    },
                    60, TimeUnit.SECONDS);
    }
}
