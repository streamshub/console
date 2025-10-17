package com.github.streamshub.systemtests.setup.strimzi;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.marcnuri.helm.Helm;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.ResourceItem;
import org.apache.logging.log4j.Logger;

public class StrimziOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(StrimziOperatorSetup.class);
    private static final String CHART = "oci://quay.io/strimzi-helm/strimzi-kafka-operator";
    private final String deploymentNamespace;
    private final String deploymentName;

    public StrimziOperatorSetup(String deploymentNamespace) {
        this.deploymentNamespace = deploymentNamespace;
        this.deploymentName = Environment.STRIMZI_OPERATOR_NAME;
    }

    public void install() {
        LOGGER.info("----------- Install Strimzi Cluster Operator -----------");
        if (Environment.SKIP_STRIMZI_INSTALLATION ||
            ResourceUtils.getKubeResource(Deployment.class, deploymentNamespace, deploymentName) != null) {
            LOGGER.warn("Strimzi Operator is already installed or it's installation was skipped with SKIP_STRIMZI_INSTALLATION");
            return;
        }

        LOGGER.info("Install Strimzi Using Helm charts");
        Helm.install(CHART)
            .withName(deploymentName)
            .withNamespace(deploymentNamespace)
            .withVersion(Environment.STRIMZI_OPERATOR_VERSION)
            .set("watchAnyNamespace", true)
            .waitReady()
            .call();

        // Additional check that Strimzi deployment was installed
        WaitUtils.waitForDeploymentWithPrefixIsReady(deploymentNamespace, deploymentName);

        // Allow resource manager delete
        KubeResourceManager.get().pushToStack(new ResourceItem<>(this::uninstall));
        LOGGER.info("Installation of Strimzi completed");
    }

    public void uninstall() {
        LOGGER.info("----------- Uninstall Strimzi Cluster Operator -----------");
        // In case namespace is deleted before operator, ignore error that the release is not present (it's uninstalled)
        Helm.uninstall(deploymentName).ignoreNotFound().call();
    }
}
