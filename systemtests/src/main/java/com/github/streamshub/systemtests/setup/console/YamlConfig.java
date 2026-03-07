package com.github.streamshub.systemtests.setup.console;

import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.SetupUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceOrder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class YamlConfig extends InstallConfig {
    private static final Logger LOGGER = LogWrapper.getLogger(YamlConfig.class);
    private List<HasMetadata> allResources;

    public YamlConfig(String namespace, String operatorBundleUrl) {
        super(namespace);

        InputStream multiYaml = null;

        LOGGER.info("Console Operator will be installed using YAML bundle");
        try {
            multiYaml = FileUtils.getYamlFileFromURL(operatorBundleUrl);
        } catch (IOException e) {
            throw new SetupException("Cannot get Console YAML resources: ", e);
        }
        allResources = ResourceOrder.sort(KubeResourceManager.get().kubeClient().getClient().load(multiYaml).items());
        LOGGER.info("Loaded {} resources from Console operator YAML", allResources.size());
        prepareConsoleCrs();
    }

    private void prepareConsoleCrs() {
        allResources.forEach(resource -> {
            SetupUtils.setNamespaceOnNamespacedResources(resource, this.deploymentNamespace);
            SetupUtils.removeSecurityContexts(resource);
            SetupUtils.fixClusterRoleBindingNamespace(resource, this.deploymentNamespace);
            if (resource instanceof Deployment deployment) {
                deployment.getMetadata().setName(deploymentName);
            }
        });
    }

    @Override
    public void install() {
        allResources.forEach(r -> KubeResourceManager.get().createOrUpdateResourceWithWait(r));
    }

    @Override
    public void delete() {
        // ResourceManager handles this step and clears operator once the test leaves the scope of installation
    }
}
