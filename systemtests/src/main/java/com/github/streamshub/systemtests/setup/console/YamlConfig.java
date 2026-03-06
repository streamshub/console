package com.github.streamshub.systemtests.setup.console;

import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.SetupUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class YamlConfig extends InstallConfig {
    private static final Logger LOGGER = LogWrapper.getLogger(YamlConfig.class);
    private List<HasMetadata> consoleBundleResources;

    public YamlConfig(String namespace, String operatorBundleUrl) {
        super(namespace);

        InputStream multiYaml = null;

        LOGGER.info("Console Operator will be installed using YAML bundle");
        try {
            multiYaml = FileUtils.getYamlFileFromURL(operatorBundleUrl);
        } catch (IOException e) {
            throw new SetupException("Cannot get Console YAML resources: ", e);
        }
        this.consoleBundleResources = KubeResourceManager.get().kubeClient().getClient().load(multiYaml).items();
        LOGGER.info("Loaded {} resources from YAML", consoleBundleResources.size());
        prepareConsoleCrs();
    }

    private void prepareConsoleCrs() {
        consoleBundleResources.forEach(resource -> {
            SetupUtils.setNamespaceOnNamespacedResources(resource, this.deploymentNamespace);
            SetupUtils.removeSecurityContexts(resource);
            SetupUtils.fixClusterRoleBindingNamespace(resource, this.deploymentNamespace);
        });
    }

    @Override
    public void install() {
        // Crds
        KubeResourceManager.get().createOrUpdateResourceWithWait(
            ResourceUtils.getResourcesArrayFromListOfResources(consoleBundleResources, CustomResourceDefinition.class));
        // ServiceAccount
        KubeResourceManager.get().createOrUpdateResourceWithWait(
            ResourceUtils.getResourceFromListOfResources(consoleBundleResources, ServiceAccount.class));
        // ClusterRoles
        KubeResourceManager.get().createOrUpdateResourceWithWait(
            ResourceUtils.getResourcesArrayFromListOfResources(consoleBundleResources, ClusterRole.class));
        // ClusterRoleBindings
        KubeResourceManager.get().createOrUpdateResourceWithWait(
            ResourceUtils.getResourcesArrayFromListOfResources(consoleBundleResources, ClusterRoleBinding.class));
        // RoleBindings
        KubeResourceManager.get().createOrUpdateResourceWithWait(
            ResourceUtils.getResourcesArrayFromListOfResources(consoleBundleResources, RoleBinding.class));
        // Deployment
        KubeResourceManager.get().createOrUpdateResourceWithWait(getBundleDeployment());
        // Service
        KubeResourceManager.get().createOrUpdateResourceWithWait(
            ResourceUtils.getResourceFromListOfResources(consoleBundleResources, Service.class));
        // ServiceMonitor
        KubeResourceManager.get().createOrUpdateResourceWithWait(
            ResourceUtils.getResourceFromListOfResources(consoleBundleResources, ServiceMonitor.class));
    }

    @Override
    public void delete() {
        // ResourceManager handles this step and clears operator once the test leaves the scope of installation
    }

    private Deployment getBundleDeployment() {
        return  new DeploymentBuilder(ResourceUtils.getResourceFromListOfResources(consoleBundleResources, Deployment.class))
            .editMetadata()
                .withName(deploymentName)
            .endMetadata()
            .build();
    }
}
