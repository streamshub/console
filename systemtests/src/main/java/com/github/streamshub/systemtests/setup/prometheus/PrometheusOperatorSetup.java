package com.github.streamshub.systemtests.setup.prometheus;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.marcnuri.helm.Helm;
import io.skodjob.testframe.TestFrameEnv;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.ResourceItem;
import org.apache.logging.log4j.Logger;

import java.nio.file.Path;

public class PrometheusOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(PrometheusOperatorSetup.class);
    private static final String PROMETHEUS_OPERATOR_NAME = "cstm-prometheus";
    private static final String OLM_CHANNEL = "beta";
    private static final String COMMUNITY_CATALOG = "community-operators";
    private static final String COMMUNITY_CATALOG_NAMESPACE = "openshift-marketplace";
    private static final String SUBSCRIPTION_NAME = "prometheus-sub";
    private final String deploymentNamespace;
    private final String deploymentName;
    private final String catalogNamespace;
    
    public PrometheusOperatorSetup(String deploymentNamespace) {
        this.deploymentNamespace = deploymentNamespace;
        this.deploymentName = PROMETHEUS_OPERATOR_NAME;
        this.catalogNamespace = ClusterUtils.isOcp()? COMMUNITY_CATALOG_NAMESPACE : Constants.OLM_MINIKUBE_NAMESPACE;
    }
    
    public void install() {
        LOGGER.info("----------- Install Prometheus Cluster Operator -----------");

        LOGGER.info("Install Prometheus Using Helm charts");
        // Subscription subscription = new SubscriptionBuilder()
        //     .withNewMetadata()
        //         .withNamespace(deploymentNamespace)
        //         .withName(SUBSCRIPTION_NAME)
        //     .endMetadata()
        //     .withNewSpec()
        //         .withName("prometheus")
        //         .withChannel(OLM_CHANNEL)
        //         .withSource(COMMUNITY_CATALOG)
        //         .withSourceNamespace(catalogNamespace)
        //     .endSpec()
        //     .build();
        //
        // KubeResourceManager.get().createOrUpdateResourceWithoutWait(subscription);
        //
        Helm.install("oci://ghcr.io/prometheus-community/charts/prometheus")
            .withName(deploymentName)
            .withNamespace(deploymentNamespace)
            .withVersion("28.8.0")
            .set("watchAnyNamespace", true)
            .withValuesFile(Path.of(TestFrameEnv.USER_PATH + "/src/main/java/com/github/streamshub/systemtests/setup/prometheus/values.yaml"))
            .waitReady()
            .call();

        //Additional check that Prometheus deployment was installed
        WaitUtils.waitForDeploymentWithPrefixIsReady(deploymentNamespace, deploymentName);


        // Allow resource manager delete
        KubeResourceManager.get().pushToStack(new ResourceItem<>(this::uninstall));
        LOGGER.info("Installation of Prometheus completed");
    }

    public void uninstall() {
        LOGGER.info("----------- Uninstall Prometheus Cluster Operator -----------");
        // In case namespace is deleted before operator, ignore error that the release is not present (it's uninstalled)
        Helm.uninstall(deploymentName).ignoreNotFound().call();
    }

    public String getDeploymentName() {
        return this.deploymentName;
    }
}
