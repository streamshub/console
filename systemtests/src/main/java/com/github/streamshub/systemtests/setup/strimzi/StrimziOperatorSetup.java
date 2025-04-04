package com.github.streamshub.systemtests.setup.strimzi;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.marcnuri.helm.Helm;
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
        Helm.install(CHART)
            .withName(deploymentName)
            .withNamespace(deploymentNamespace)
            .withVersion(Environment.STRIMZI_OPERATOR_VERSION)
            .set("watchAnyNamespace", true)
            .waitReady()
            .call();
    }

    public void uninstall() {
        LOGGER.info("----------- Uninstall Strimzi Cluster Operator -----------");
        Helm.uninstall(deploymentName).call();
    }
}
