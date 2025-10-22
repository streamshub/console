package com.github.streamshub.systemtests.setup.console;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.exceptions.OperatorSdkNotInstalledException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroup;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroupBuilder;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.ClusterServiceVersion;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SubscriptionBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

public class OlmConfig extends InstallConfig {
    private static final Logger LOGGER = LogWrapper.getLogger(OlmConfig.class);
    private String olmAppBundlePrefix = Environment.CONSOLE_DEPLOYMENT_NAME;
    private String packageName = Environment.CONSOLE_OLM_PACKAGE_NAME;
    private String catalogSourceName = Environment.CONSOLE_OLM_CATALOG_SOURCE_NAME;
    private String catalogSourceNamespace = Environment.CONSOLE_OLM_CATALOG_SOURCE_NAMESPACE;
    private String channelName = Environment.CONSOLE_OLM_CHANNEL_NAME;
    private String subscriptionName = Constants.CONSOLE_OLM_SUBSCRIPTION_NAME;

    public OlmConfig(String namespace) {
        super(namespace);
        LOGGER.info("Console Operator will be installed using OLM");
        if (!ClusterUtils.isOcp() &&
            ResourceUtils.getKubeResource(CustomResourceDefinition.class, "subscriptions.operators.coreos.com") == null) {
            throw new OperatorSdkNotInstalledException("Operator SDK is not installed on the current cluster. Cannot install Console Operator using subscriptions");
        }
    }

    @Override
    public void install() {
        KubeResourceManager.get().createOrUpdateResourceWithWait(getOlmOperatorGroup());
        KubeResourceManager.get().createOrUpdateResourceWithWait(getOlmSubscription());

        WaitUtils.waitForDeploymentWithPrefixIsReady(deploymentNamespace, olmAppBundlePrefix);

        // Get and set the deployment full name from known OLM bundle prefix
        deploymentName = ResourceUtils.listKubeResourcesByPrefix(Deployment.class, deploymentNamespace, olmAppBundlePrefix)
            .get(0)
            .getMetadata()
            .getName();
    }

    @Override
    public void delete() {
        KubeResourceManager.get().deleteResourceWithWait(getOlmOperatorGroup());
        KubeResourceManager.get().deleteResourceWithWait(getOlmSubscription());

        List<Deployment> deploymentList = ResourceUtils.listKubeResourcesByPrefix(Deployment.class, deploymentNamespace, deploymentName);
        if (!deploymentList.isEmpty()) {
            // delete csv
            String csvFullName = ResourceUtils.listKubeResourcesByPrefix(ClusterServiceVersion.class, deploymentNamespace, packageName).get(0).getMetadata().getName();
            ClusterServiceVersion csv = ResourceUtils.getKubeResource(ClusterServiceVersion.class, deploymentNamespace, csvFullName);
            KubeResourceManager.get().deleteResourceWithWait(csv);
            KubeResourceManager.get().deleteResourceWithWait(deploymentList.get(0));
        }
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    private Subscription getOlmSubscription() {
        return new SubscriptionBuilder()
            .editMetadata()
                .withNamespace(deploymentNamespace)
                .withName(subscriptionName)
            .endMetadata()
            .editSpec()
                .withName(packageName)
                .withSource(catalogSourceName)
                .withInstallPlanApproval("Automatic")
                .withChannel(channelName)
                .withSourceNamespace(catalogSourceNamespace)
            .endSpec()
            .build();
    }

    private OperatorGroup getOlmOperatorGroup() {
        return new OperatorGroupBuilder()
            .withNewMetadata()
                .withName("streamshub-operators")
                .withNamespace(deploymentNamespace)
            .endMetadata()
            .withNewSpec()
                .withTargetNamespaces(Collections.emptyList())
                .withUpgradeStrategy("Default")
            .endSpec()
            .build();
    }
}
