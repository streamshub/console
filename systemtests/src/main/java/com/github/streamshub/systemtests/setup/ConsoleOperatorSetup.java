package com.github.streamshub.systemtests.setup;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.ExampleFilePaths;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.ClusterUtils;
import com.github.streamshub.systemtests.utils.ConsoleUtils;
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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConsoleOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(ConsoleOperatorSetup.class);

    private final String deploymentNamespace;
    private final String operatorDeploymentName = ConsoleUtils.getConsoleOperatorName();
    private List<File> consoleBundleResources = getConsoleBundleResources();

    public ConsoleOperatorSetup(String namespace) {
        this.deploymentNamespace = namespace;
        if (!Environment.INSTALL_USING_OLM) {
            this.consoleBundleResources = getConsoleBundleResources();
        }
    }

    public void setup() {
        LOGGER.info("----------- Setup Console Operator -----------");

        if (!ResourceUtils.listKubeResourcesByPrefix(Deployment.class, this.deploymentNamespace, this.operatorDeploymentName).isEmpty()) {
            LOGGER.warn("Console Operator is already deployed. Skipping deployment");
            return;
        }

        if (Environment.INSTALL_USING_OLM) {
            LOGGER.info("Setup Console Operator using OLM");
            // Check for non OpenShift clusters
            if (!ClusterUtils.isOcp() &&
                ResourceUtils.getKubeResource(CustomResourceDefinition.class, "subscriptions.operators.coreos.com") == null) {
                throw new RuntimeException("Operator SDK is not installed on the current cluster. Cannot install Console Operator using subscriptions");
            }
            KubeResourceManager.get().createResourceWithWait(getOlmOperatorGroup());
            KubeResourceManager.get().createResourceWithWait(getOlmSubscription());
            WaitUtils.waitForDeploymentWithPrefixIsReady(this.deploymentNamespace, this.operatorDeploymentName);
        } else {
            LOGGER.info("Setup Console Operator using YAML files");
            KubeResourceManager.get().createResourceWithoutWait(getBundleCrds());
            KubeResourceManager.get().createResourceWithoutWait(getBundleServiceAccount());
            KubeResourceManager.get().createResourceWithoutWait(getBundleClusterRoles());
            KubeResourceManager.get().createResourceWithoutWait(getBundleClusterRoleBindings());
            KubeResourceManager.get().createResourceWithoutWait(getBundleRoleBindings());
            KubeResourceManager.get().createResourceWithWait(getBundleDeployment());
        }
    }

    public void teardown() {
        LOGGER.info("----------- Teardown Console Operator -----------");
        // Console CRD
        ConsoleUtils.removeFinalizersInAllConsoleInstances();
        if (Environment.INSTALL_USING_OLM) {

        }
    }

    // ------
    // Bundle
    // ------
    private List<File> getConsoleBundleResources() {
        List<File> tempFiles = new ArrayList<>();
        String[] yamlFiles;

        if (Environment.CONSOLE_OPERATOR_BUNDLE_URL.startsWith("http://") || Environment.CONSOLE_OPERATOR_BUNDLE_URL.startsWith("https://")) {
            yamlFiles = SetupUtils.getYamlContentFromUrl(Environment.CONSOLE_OPERATOR_BUNDLE_URL).split("---");
        } else {
            yamlFiles = SetupUtils.getYamlContentFromFile(Environment.CONSOLE_OPERATOR_BUNDLE_URL).split("---");
        }

        // Separate one file into multiple YAML files for easier object mapping
        for (String file : yamlFiles) {
            if (!file.trim().isEmpty()) {
                String fileName = "";
                // Matches resource kind name to be used as a YAML file name
                Matcher matcher = Pattern.compile("\\nkind\\: (\\w+)\\n").matcher(file);

                while (matcher.find()) {
                    fileName = matcher.group(1);
                }

                Path tempFile = null;

                try {
                    tempFile = Files.createTempFile("console-" + fileName + "-tmp_", ".yaml");
                    Files.write(tempFile, file.trim().getBytes(StandardCharsets.UTF_8));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                tempFiles.add(tempFile.toFile());
                LOGGER.debug("Created temp file: " + tempFile.toAbsolutePath());
            }
        }

        return tempFiles;
    }

    private CustomResourceDefinition[] getBundleCrds() {
        return this.consoleBundleResources.stream()
            .filter(file -> file.getName().contains("-CustomResourceDefinition-"))
            .map(crd -> new CustomResourceDefinitionBuilder(TestFrameUtils.configFromYaml(crd, CustomResourceDefinition.class)).build())
            .toArray(CustomResourceDefinition[]::new);
    }

    private ServiceAccount getBundleServiceAccount() {
        return new ServiceAccountBuilder(TestFrameUtils.configFromYaml(this.consoleBundleResources.stream()
            .filter(file -> file.getName().contains("-ServiceAccount-"))
            .toList().get(0), ServiceAccount.class))
                .editMetadata()
                    .withNamespace(this.deploymentNamespace)
                .endMetadata()
                .build();
    }

    private ClusterRole[] getBundleClusterRoles() {
        return this.consoleBundleResources.stream()
            .filter(file -> file.getName().contains("-ClusterRole-"))
            .map(cr -> new ClusterRoleBuilder(TestFrameUtils.configFromYaml(cr, ClusterRole.class))
                .editMetadata()
                    .withNamespace(this.deploymentNamespace)
                .endMetadata()
                .build())
            .toArray(ClusterRole[]::new);
    }

    private ClusterRoleBinding[] getBundleClusterRoleBindings() {
        return this.consoleBundleResources.stream()
            .filter(file -> file.getName().contains("-ClusterRoleBinding-"))
            .map(crb -> new ClusterRoleBindingBuilder(TestFrameUtils.configFromYaml(crb, ClusterRoleBinding.class))
                .editMetadata()
                    .withNamespace(this.deploymentNamespace)
                .endMetadata()
                .editFirstSubject()
                    .withNamespace(this.deploymentNamespace)
                .endSubject()
                .build())
            .toArray(ClusterRoleBinding[]::new);
    }

    private RoleBinding[] getBundleRoleBindings() {
        return this.consoleBundleResources.stream()
            .filter(file -> file.getName().contains("-RoleBinding-"))
            .map(rb -> new RoleBindingBuilder(TestFrameUtils.configFromYaml(rb, RoleBinding.class))
                .editMetadata()
                    .withNamespace(this.deploymentNamespace)
                .endMetadata()
                .editFirstSubject()
                    .withNamespace(this.deploymentNamespace)
                .endSubject()
                .build())
            .toArray(RoleBinding[]::new);
    }

    private Deployment getBundleDeployment() {
        DeploymentBuilder consoleOperator = new DeploymentBuilder(TestFrameUtils.configFromYaml(
                this.consoleBundleResources.stream()
                    .filter(file -> file.getName().contains("-Deployment-"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No Console Operator deployment YAML found")),
                Deployment.class))
                .editMetadata()
                    .withNamespace(this.deploymentNamespace)
                    .withName(ConsoleUtils.getConsoleOperatorName())
                .endMetadata()
                .editSpec()
                    .editTemplate()
                        .editMetadata()
                            .withNamespace(this.deploymentNamespace)
                        .endMetadata()
                    .endTemplate()
                .endSpec();

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
            TestFrameUtils.configFromYaml(SetupUtils.getYamlContentFromFile(ExampleFilePaths.CONSOLE_OPERATOR_GROUP_YAML), OperatorGroup.class))
            .editMetadata()
                .withNamespace(this.deploymentNamespace)
            .endMetadata()
            .build();
    }

    private Subscription getOlmSubscription() {
        return new SubscriptionBuilder(TestFrameUtils.configFromYaml(SetupUtils.getYamlContentFromFile(ExampleFilePaths.CONSOLE_OPERATOR_SUBSCRIPTION_YAML), Subscription.class))
            .editMetadata()
                .withNamespace(this.deploymentNamespace)
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
