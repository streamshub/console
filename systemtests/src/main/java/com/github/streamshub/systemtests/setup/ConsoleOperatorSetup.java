package com.github.streamshub.systemtests.setup;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.ExampleFilePaths;
import com.github.streamshub.systemtests.exceptions.OperatorSdkNotInstalledException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.ClusterUtils;
import com.github.streamshub.systemtests.utils.ResourceUtils;
import com.github.streamshub.systemtests.utils.SetupUtils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroup;
import io.fabric8.openshift.api.model.operatorhub.v1.OperatorGroupBuilder;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SubscriptionBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.utils.TestFrameUtils;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

public class ConsoleOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(ConsoleOperatorSetup.class);

    private final String deploymentNamespace;
    private String operatorDeploymentName;
    private List<String> consoleBundleResources;

    public ConsoleOperatorSetup(String namespace) {
        this.operatorDeploymentName = Environment.CONSOLE_DEPLOYMENT_NAME + "-operator";
        this.deploymentNamespace = namespace;
        if (SetupUtils.isYamlInstall()) {
            this.consoleBundleResources = Arrays.stream(SetupUtils.getYamlContent(Environment.CONSOLE_OPERATOR_BUNDLE_URL)
                .split("---"))
                .toList();
        }
    }

    public void setup() {
        LOGGER.info("----------- Setup Console Operator -----------");

        if (!ResourceUtils.listKubeResourcesByPrefix(Deployment.class, deploymentNamespace, operatorDeploymentName).isEmpty()) {
            LOGGER.warn("Console Operator is already deployed. Skipping deployment");
            return;
        }

        if (SetupUtils.isOlmInstall()) {
            LOGGER.info("Setup Console Operator using OLM");
            // Check for non OpenShift clusters
            if (!ClusterUtils.isOcp() &&
                ResourceUtils.getKubeResource(CustomResourceDefinition.class, "subscriptions.operators.coreos.com") == null) {
                throw new OperatorSdkNotInstalledException("Operator SDK is not installed on the current cluster. Cannot install Console Operator using subscriptions");
            }
            KubeResourceManager.get().createResourceWithWait(getOlmOperatorGroup());
            KubeResourceManager.get().createResourceWithWait(getOlmSubscription());
            WaitUtils.waitForDeploymentWithPrefixIsReady(deploymentNamespace, operatorDeploymentName);

            // Set full name in case of OLM, as it is different from YAML installation.
            // Yaml deployment name is used here to get the full operator deployment name
            operatorDeploymentName = ResourceUtils.listKubeResourcesByPrefix(Deployment.class, deploymentNamespace, operatorDeploymentName)
                .get(0)
                .getMetadata()
                .getName();

        } else if (SetupUtils.isYamlInstall()) {
            LOGGER.info("Setup Console Operator using YAML files");
            KubeResourceManager.get().createResourceWithoutWait(getBundleCrds());
            KubeResourceManager.get().createResourceWithoutWait(getBundleServiceAccount());
            KubeResourceManager.get().createResourceWithoutWait(getBundleClusterRoles());
            KubeResourceManager.get().createResourceWithoutWait(getBundleClusterRoleBindings());
            KubeResourceManager.get().createResourceWithoutWait(getBundleRoleBindings());
            KubeResourceManager.get().createResourceWithWait(getBundleDeployment());
        }
    }

    // ------
    // Bundle
    // ------
    private CustomResourceDefinition[] getBundleCrds() {
        return consoleBundleResources.stream()
            .filter(r -> SetupUtils.isOfKind(r, CustomResourceDefinition.class))
            .map(content -> new CustomResourceDefinitionBuilder(TestFrameUtils.configFromYaml(content, CustomResourceDefinition.class))
                .build())
            .toArray(CustomResourceDefinition[]::new);
    }

    private ServiceAccount getBundleServiceAccount() {
        return consoleBundleResources.stream()
            .filter(r -> SetupUtils.isOfKind(r, ServiceAccount.class))
            .map(content -> new ServiceAccountBuilder(TestFrameUtils.configFromYaml(content, ServiceAccount.class))
                .editMetadata()
                    .withNamespace(deploymentNamespace)
                .endMetadata()
                .build())
            .toList()
            .get(0);
    }

    private ClusterRole[] getBundleClusterRoles() {
        return consoleBundleResources.stream()
            .filter(r -> SetupUtils.isOfKind(r, ClusterRole.class))
            .map(content -> new ClusterRoleBuilder(TestFrameUtils.configFromYaml(content, ClusterRole.class))
                .editMetadata()
                    .withNamespace(deploymentNamespace)
                .endMetadata()
                .build())
            .toArray(ClusterRole[]::new);
    }

    private ClusterRoleBinding[] getBundleClusterRoleBindings() {
        return consoleBundleResources.stream()
            .filter(r -> SetupUtils.isOfKind(r, ClusterRoleBinding.class))
            .map(content -> new ClusterRoleBindingBuilder(TestFrameUtils.configFromYaml(content, ClusterRoleBinding.class))
                .editMetadata()
                    .withNamespace(deploymentNamespace)
                .endMetadata()
                .editFirstSubject()
                    .withNamespace(deploymentNamespace)
                .endSubject()
                .build())
            .toArray(ClusterRoleBinding[]::new);
    }

    private RoleBinding[] getBundleRoleBindings() {
        return consoleBundleResources.stream()
            .filter(r -> SetupUtils.isOfKind(r, RoleBinding.class))
            .map(content -> new RoleBindingBuilder(TestFrameUtils.configFromYaml(content, RoleBinding.class))
                .editMetadata()
                    .withNamespace(deploymentNamespace)
                .endMetadata()
                .editFirstSubject()
                    .withNamespace(deploymentNamespace)
                .endSubject()
                .build())
            .toArray(RoleBinding[]::new);
    }

    private Deployment getBundleDeployment() {
        DeploymentBuilder consoleOperator = consoleBundleResources.stream()
            .filter(r -> SetupUtils.isOfKind(r, Deployment.class))
            .map(content -> new DeploymentBuilder(TestFrameUtils.configFromYaml(content, Deployment.class))
                .editMetadata()
                    .withNamespace(deploymentNamespace)
                    .withName(operatorDeploymentName)
                .endMetadata()
                .editSpec()
                    .editTemplate()
                        .editMetadata()
                            .withNamespace(deploymentNamespace)
                        .endMetadata()
                    .endTemplate()
                .endSpec())
                .toList()
                .get(0);

        // Override Console images if provided
        if (!Environment.CONSOLE_OPERATOR_IMAGE.isEmpty()) {
            consoleOperator = consoleOperator.editSpec()
                .editTemplate()
                    .editSpec()
                        .editFirstContainer()
                            .withImage(Environment.CONSOLE_OPERATOR_IMAGE)
                        .endContainer()
                    .endSpec()
                .endTemplate()
                .endSpec();
        }

        if (!Environment.CONSOLE_API_IMAGE.isEmpty()) {
            consoleOperator = consoleOperator.editSpec()
                .editTemplate()
                    .editSpec()
                        .editFirstContainer()
                            .addNewEnvLike(new EnvVarBuilder()
                                .withName("CONSOLE_DEPLOYMENT_DEFAULT_API_IMAGE")
                                .withValue(Environment.CONSOLE_API_IMAGE)
                                .build())
                            .endEnv()
                        .endContainer()
                    .endSpec()
                .endTemplate()
                .endSpec();
        }

        if (!Environment.CONSOLE_UI_IMAGE.isEmpty()) {
            consoleOperator = consoleOperator.editSpec()
                .editTemplate()
                    .editSpec()
                        .editFirstContainer()
                            .addNewEnvLike(new EnvVarBuilder()
                                .withName("CONSOLE_DEPLOYMENT_DEFAULT_UI_IMAGE")
                                .withValue(Environment.CONSOLE_UI_IMAGE)
                                .build())
                            .endEnv()
                        .endContainer()
                    .endSpec()
                .endTemplate()
                .endSpec();
        }

        return consoleOperator.build();
    }

    // ------
    // OLM
    // ------
    private OperatorGroup getOlmOperatorGroup() {
        return new OperatorGroupBuilder(
            TestFrameUtils.configFromYaml(SetupUtils.getYamlContent(ExampleFilePaths.CONSOLE_OPERATOR_GROUP_YAML), OperatorGroup.class))
            .editMetadata()
                .withNamespace(deploymentNamespace)
            .endMetadata()
            .build();
    }

    private Subscription getOlmSubscription() {
        return new SubscriptionBuilder(TestFrameUtils.configFromYaml(SetupUtils.getYamlContent(ExampleFilePaths.CONSOLE_OPERATOR_SUBSCRIPTION_YAML), Subscription.class))
            .editMetadata()
                .withNamespace(deploymentNamespace)
                .withName(Constants.CONSOLE_OPERATOR_SUBSCRIPTION_NAME)
            .endMetadata()
            .editSpec()
                .withName(Environment.CONSOLE_OLM_PACKAGE_NAME)
                .withSource(Environment.CONSOLE_CATALOG_SOURCE_NAME)
                .withInstallPlanApproval("Automatic")
                .withChannel(Environment.OLM_OPERATOR_CHANNEL)
                .withSourceNamespace(Environment.OLM_CATALOG_NAMESPACE)
            .endSpec()
            .build();
    }
}
