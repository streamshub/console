package com.github.streamshub.systemtests.setup.console;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import org.apache.logging.log4j.Logger;

import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.resourcetypes.prometheus.ServiceMonitorType;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.SetupUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceOrder;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.skodjob.kubetest4j.resources.KubeResourceManager;

public class YamlConfig extends InstallConfig {
    private static final Logger LOGGER = LogWrapper.getLogger(YamlConfig.class);
    private List<HasMetadata> allResources = new ArrayList<>();

    public YamlConfig(String namespace, String operatorBundleUrl) {
        super(namespace);

        InputStream multiYaml = null;

        LOGGER.info("Console Operator will be installed using YAML bundle");
        try {
            multiYaml = FileUtils.getYamlFileFromURL(FileUtils.resolveLocation(operatorBundleUrl).toString());
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

        // Remove service monitor on non-openshift clusters if it does not have the CRDs
        if (!ServiceMonitorType.isAvailable() && !ClusterUtils.isOcp()) {
            allResources = allResources.stream()
                .filter(resource -> !(resource instanceof ServiceMonitor))
                .toList();
        }
    }

    @Override
    public void install() {
        allResources.forEach(r -> KubeResourceManager.get().createOrUpdateResourceWithWait(r));
    }

    @Override
    public void delete() {
        // ResourceManager handles this step and clears operator once the test leaves the scope of installation
    }

    @Override
    List<EnvVar> editEnv(UnaryOperator<List<EnvVar>> editor) {
        AtomicReference<List<EnvVar>> result = new AtomicReference<>();

        ResourceUtils.editKubeResource(Deployment.class, deploymentNamespace, deploymentName, dep -> {
            var container = dep.getSpec().getTemplate().getSpec().getContainers().get(0);
            var env = editor.apply(container.getEnv());
            result.set(env);

            return new DeploymentBuilder(dep)
                    .editSpec()
                        .editTemplate()
                            .editSpec()
                                .editContainer(0)
                                    .withEnv(env)
                                .endContainer()
                            .endSpec()
                        .endTemplate()
                    .endSpec()
                    .build();
        });

        return result.get();
    }
}
