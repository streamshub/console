package com.github.streamshub.systemtests.setup.console;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.FileUtils;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBuilder;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBindingBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class YamlConfig extends InstallConfig {
    private static final Logger LOGGER = LogWrapper.getLogger(YamlConfig.class);
    private List<HasMetadata> consoleBundleResources;

    public YamlConfig(String namespace, String operatorBundleUrl) {
        super(namespace);
        LOGGER.info("Console Operator will be installed using YAML bundle");
        // Need to replace streamed content due to KubernetesException being thrown during load of released YAML
        // that contains `namespace: ${NAMESPACE}` where $ is an unknown symbol that cannot be parsed
        try (InputStream yamlContentStream = FileUtils.resolveLocation(operatorBundleUrl).openStream()) {
            InputStream replacedStream = new ByteArrayInputStream(new String(yamlContentStream.readAllBytes(), StandardCharsets.UTF_8)
                .replace("${NAMESPACE}", "NAMESPACE")
                .getBytes(StandardCharsets.UTF_8));
            this.consoleBundleResources = KubeResourceManager.get()
                .kubeClient()
                .getClient()
                .load(replacedStream)
                .items();
        } catch (IOException e) {
            throw new SetupException("Cannot get Console YAML resources: ", e);
        }
    }

    @Override
    public void install() {
        KubeResourceManager.get().createOrUpdateResourceWithWait(getBundleCrds());
        KubeResourceManager.get().createOrUpdateResourceWithWait(getBundleServiceAccount());
        KubeResourceManager.get().createOrUpdateResourceWithWait(getBundleClusterRoles());
        KubeResourceManager.get().createOrUpdateResourceWithWait(getBundleClusterRoleBindings());
        KubeResourceManager.get().createOrUpdateResourceWithWait(getBundleRoleBindings());
        KubeResourceManager.get().createOrUpdateResourceWithWait(getBundleDeployment());
    }

    @Override
    public void delete() {
        KubeResourceManager.get().deleteResourceWithWait(getBundleCrds());
        KubeResourceManager.get().deleteResourceWithWait(getBundleServiceAccount());
        KubeResourceManager.get().deleteResourceWithWait(getBundleClusterRoles());
        KubeResourceManager.get().deleteResourceWithWait(getBundleClusterRoleBindings());
        KubeResourceManager.get().deleteResourceWithWait(getBundleRoleBindings());
        KubeResourceManager.get().deleteResourceWithWait(getBundleDeployment());
    }

    private CustomResourceDefinition[] getBundleCrds() {
        return ResourceUtils.getResourcesStreamFromListOfResources(consoleBundleResources, CustomResourceDefinition.class)
            .toArray(CustomResourceDefinition[]::new);
    }

    private ServiceAccount getBundleServiceAccount() {
        return new ServiceAccountBuilder(ResourceUtils.getResourceFromListOfResources(consoleBundleResources, ServiceAccount.class))
            .editMetadata()
                .withNamespace(deploymentNamespace)
            .endMetadata()
            .build();
    }

    private ClusterRole[] getBundleClusterRoles() {
        return ResourceUtils.getResourcesStreamFromListOfResources(consoleBundleResources, ClusterRole.class)
            .map(r -> new ClusterRoleBuilder(r)
                .editMetadata()
                    .withNamespace(deploymentNamespace)
                .endMetadata()
                .build())
            .toArray(ClusterRole[]::new);
    }

    private ClusterRoleBinding[] getBundleClusterRoleBindings() {
        return ResourceUtils.getResourcesStreamFromListOfResources(consoleBundleResources, ClusterRoleBinding.class)
            .map(r -> new ClusterRoleBindingBuilder(r)
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
        return ResourceUtils.getResourcesStreamFromListOfResources(consoleBundleResources, RoleBinding.class)
            .map(r -> new RoleBindingBuilder(r)
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
        DeploymentBuilder consoleOperator = new DeploymentBuilder(ResourceUtils.getResourceFromListOfResources(consoleBundleResources, Deployment.class))
                .editMetadata()
                    .withNamespace(deploymentNamespace)
                    .withName(deploymentName)
                .endMetadata()
                .editSpec()
                    .editTemplate()
                        .editMetadata()
                            .withNamespace(deploymentNamespace)
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
}
