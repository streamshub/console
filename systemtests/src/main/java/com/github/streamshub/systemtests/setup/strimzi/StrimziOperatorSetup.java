package com.github.streamshub.systemtests.setup.strimzi;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.marcnuri.helm.Helm;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import io.skodjob.kubetest4j.resources.ResourceItem;
import org.apache.logging.log4j.Logger;

public class StrimziOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(StrimziOperatorSetup.class);
    private static final String CHART = "oci://quay.io/strimzi-helm/strimzi-kafka-operator";
    private final String deploymentNamespace;
    private final String deploymentName;

    public StrimziOperatorSetup(String deploymentNamespace) {
        this.deploymentNamespace = deploymentNamespace;
        this.deploymentName = Environment.STRIMZI_OPERATOR_NAME;
        LOGGER.debug("Prepared Strimzi Operator setup for deployment [{}] in namespace [{}]", deploymentName, deploymentNamespace);
    }

    public void install() {
        LOGGER.info("----------- Install Strimzi Cluster Operator -----------");
        if (Environment.SKIP_STRIMZI_INSTALLATION ||
            ResourceUtils.getKubeResource(Deployment.class, deploymentNamespace, deploymentName) != null) {
            LOGGER.warn("Strimzi Operator [{}] in namespace [{}] is already installed, or installation was skipped via SKIP_STRIMZI_INSTALLATION={}",
                deploymentName, deploymentNamespace, Environment.SKIP_STRIMZI_INSTALLATION);
            return;
        }

        LOGGER.info("Installing Strimzi Operator [{}] version [{}] via Helm chart [{}] into namespace [{}]",
            deploymentName, Environment.STRIMZI_OPERATOR_VERSION, CHART, deploymentNamespace);
        Helm.install(CHART)
            .withName(deploymentName)
            .withNamespace(deploymentNamespace)
            .withVersion(Environment.STRIMZI_OPERATOR_VERSION)
            .set("watchAnyNamespace", true)
            .waitReady()
            .call();

        // Additional check that Strimzi deployment was installed
        LOGGER.debug("Waiting for Strimzi Operator deployment [{}] in namespace [{}] to become ready", deploymentName, deploymentNamespace);
        WaitUtils.waitForDeploymentWithPrefixIsReady(deploymentNamespace, deploymentName);

        // Allow resource manager delete
        KubeResourceManager.get().pushToStack(new ResourceItem<>(this::uninstall));
        LOGGER.info("Strimzi Operator [{}] installation completed in namespace [{}]", deploymentName, deploymentNamespace);
    }

    public void uninstall() {
        LOGGER.info("----------- Uninstall Strimzi Cluster Operator -----------");
        LOGGER.info("Uninstalling Strimzi Operator [{}] from namespace [{}]", deploymentName, deploymentNamespace);
        // In case namespace is deleted before operator, ignore error that the release is not present (it's uninstalled)
        Helm.uninstall(deploymentName).ignoreNotFound().call();
    }
}
