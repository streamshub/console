package com.github.streamshub.systemtests.setup.prometheus;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.SetupUtils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceOrder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.ResourceItem;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PrometheusOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(PrometheusOperatorSetup.class);
    private static final String PROMETHEUS_OPERATOR_NAME = "prometheus-operator";
    private static final String PROMETHEUS_BUNDLE_URL = "https://github.com/prometheus-operator/prometheus-operator/releases/download/v" +
        Environment.PROMETHEUS_VERSION +
        "/bundle.yaml";

    private final String deploymentNamespace;
    private final String deploymentName;
    private List<HasMetadata> allResources;

    public PrometheusOperatorSetup(String deploymentNamespace) {
        this.deploymentNamespace = deploymentNamespace;
        this.deploymentName = PROMETHEUS_OPERATOR_NAME;

        InputStream multiYaml = null;
        try {
            multiYaml = FileUtils.getYamlFileFromURL(PROMETHEUS_BUNDLE_URL);
        } catch (IOException e) {
            throw new SetupException("Unable to load prometheus CRs: " + e.getMessage());
        }
        allResources = ResourceOrder.sort(KubeResourceManager.get().kubeClient().getClient().load(multiYaml).items());
        LOGGER.info("Loaded {} resources from Prometheus operator YAML", allResources.size());
        preparePrometheusCrs();
    }

    private void preparePrometheusCrs() {
        allResources.forEach(resource -> {
            SetupUtils.setNamespaceOnNamespacedResources(resource, deploymentNamespace);
            SetupUtils.removeSecurityContexts(resource);
            SetupUtils.fixClusterRoleBindingNamespace(resource, deploymentNamespace);
            SetupUtils.fixRoleBindingNamespace(resource, deploymentNamespace);
        });
    }

    public void setup() {
        LOGGER.info("----------- Install Prometheus Cluster Operator -----------");

        LOGGER.info("Install Prometheus Using YAML");
        allResources.forEach(resource -> KubeResourceManager.get().createOrUpdateResourceWithoutWait(resource));

        //Additional check that Prometheus deployment was installed
        WaitUtils.waitForDeploymentWithPrefixIsReady(deploymentNamespace, deploymentName);

        // Allow resource manager delete
        KubeResourceManager.get().pushToStack(new ResourceItem<>(this::teardown));
        LOGGER.info("Installation of Prometheus completed");
    }

    public void teardown() {
        LOGGER.info("----------- Uninstall Prometheus Cluster Operator -----------");
        allResources.forEach(resource -> KubeResourceManager.get().deleteResourceWithoutWait(resource));
    }
}
