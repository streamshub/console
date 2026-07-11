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
import io.skodjob.kubetest4j.resources.KubeResourceManager;
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
        LOGGER.debug("OLM config: package='{}', channel='{}', catalogSource='{}/{}', namespace='{}'",
            packageName, channelName, catalogSourceNamespace, catalogSourceName, namespace);
        if (!ClusterUtils.isOcp() &&
            ResourceUtils.getKubeResource(CustomResourceDefinition.class, "subscriptions.operators.coreos.com") == null) {
            throw new OperatorSdkNotInstalledException("Operator SDK is not installed on the current cluster. Cannot install Console Operator using subscriptions");
        }
    }

    @Override
    public void install() {

        if (!ResourceUtils.listKubeResourcesByPrefix(Deployment.class, deploymentNamespace, deploymentName).isEmpty() &&
            !Environment.DELETE_CONSOLE_OPERATOR_BEFORE_INSTALL) {
            LOGGER.info("Console Operator deployment '{}' already exists in namespace '{}', skipping OLM install", deploymentName, deploymentNamespace);
            return;
        }

        LOGGER.info("Creating OLM OperatorGroup and Subscription '{}' for package '{}' in namespace '{}'", subscriptionName, packageName, deploymentNamespace);
        KubeResourceManager.get().createOrUpdateResourceWithWait(getOlmOperatorGroup());
        KubeResourceManager.get().createOrUpdateResourceWithWait(getOlmSubscription());

        LOGGER.info("Waiting for Console Operator deployment with prefix '{}' to become ready in namespace '{}'", olmAppBundlePrefix, deploymentNamespace);
        WaitUtils.waitForDeploymentWithPrefixIsReady(deploymentNamespace, olmAppBundlePrefix);

        // Get and set the deployment full name from known OLM bundle prefix
        deploymentName = ResourceUtils.listKubeResourcesByPrefix(Deployment.class, deploymentNamespace, olmAppBundlePrefix)
            .getFirst()
            .getMetadata()
            .getName();
        LOGGER.debug("Resolved Console Operator deployment name to '{}'", deploymentName);
    }

    @Override
    public void delete() {
        LOGGER.info("Deleting OLM OperatorGroup and Subscription '{}' in namespace '{}'", subscriptionName, deploymentNamespace);
        KubeResourceManager.get().deleteResourceWithWait(getOlmOperatorGroup());
        KubeResourceManager.get().deleteResourceWithWait(getOlmSubscription());
        List<Deployment> deploymentList = ResourceUtils.listKubeResourcesByPrefix(Deployment.class, deploymentNamespace, deploymentName);
        if (!deploymentList.isEmpty()) {
            // delete csv
            String csvFullName = ResourceUtils.listKubeResourcesByPrefix(ClusterServiceVersion.class, deploymentNamespace, packageName).getFirst().getMetadata().getName();
            LOGGER.debug("Deleting ClusterServiceVersion '{}' in namespace '{}'", csvFullName, deploymentNamespace);
            ClusterServiceVersion csv = ResourceUtils.getKubeResource(ClusterServiceVersion.class, deploymentNamespace, csvFullName);
            KubeResourceManager.get().deleteResourceWithWait(csv);
            LOGGER.info("Deleting Console Operator deployment '{}' in namespace '{}'", deploymentList.getFirst().getMetadata().getName(), deploymentNamespace);
            KubeResourceManager.get().deleteResourceWithWait(deploymentList.getFirst());
        } else {
            LOGGER.debug("No Console Operator deployment found with prefix '{}' in namespace '{}', nothing to delete", deploymentName, deploymentNamespace);
        }
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    /**
     * Bumps the Subscription's channel to trigger an in-place OLM upgrade of the Console Operator,
     * without going through {@link #install()}'s "already installed, skip" guard.
     *
     * <p>{@link #install()} intentionally no-ops when a Deployment already exists and
     * {@link Environment#DELETE_CONSOLE_OPERATOR_BEFORE_INSTALL} is {@code false} - that guard exists
     * purely as a debugging convenience (skip redeploying between local runs) and has nothing to do
     * with a deliberate upgrade test switching channels on an existing Subscription. Patching the
     * Subscription directly here sidesteps that guard entirely: OLM picks up the channel change,
     * resolves the new CSV and rolls the Deployment on its own.
     *
     * @param channelName the OLM channel to switch the Subscription to
     */
    public void updateChannel(String channelName) {
        this.channelName = channelName;
        LOGGER.info("Bumping Console Operator Subscription '{}' to channel '{}' in namespace '{}'", subscriptionName, channelName, deploymentNamespace);
        KubeResourceManager.get().createOrUpdateResourceWithWait(getOlmSubscription());
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
