package com.github.streamshub.systemtests.setup.kroxylicious;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.SetupUtils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceOrder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.skodjob.kubetest4j.resources.KubeResourceManager;
import io.skodjob.kubetest4j.resources.ResourceItem;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public class KroxyliciousOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(KroxyliciousOperatorSetup.class);

    private static final String KROXYLICIOUS_OPERATOR_NAME = "kroxylicious-operator";
    private static final String KROXYLICIOUS_BUNDLE_URL = "https://github.com/kroxylicious/kroxylicious/releases/download/v" + Environment.KROXYLICIOUS_VERSION + "/kroxylicious-operator-" + Environment.KROXYLICIOUS_VERSION + ".tar.gz";
    private static final String KROXY_TEMP_FILE_PREFIX = "kroxy_tmp";
    private static final String KROXY_INSTALL_DIR_NAME = "install";

    private final String deploymentNamespace;
    private final String deploymentName;
    private List<HasMetadata> allResources;

    public KroxyliciousOperatorSetup(String deploymentNamespace) {
        this.deploymentNamespace = deploymentNamespace;
        this.deploymentName = KROXYLICIOUS_OPERATOR_NAME;

        InputStream multiYaml = null;
        try {
            LOGGER.debug("Downloading Kroxylicious operator bundle from '{}'", KROXYLICIOUS_BUNDLE_URL);
            Path extractedArchive = FileUtils.downloadAndExtractTarGz(KROXYLICIOUS_BUNDLE_URL, KROXY_TEMP_FILE_PREFIX);
            multiYaml = FileUtils.loadYamlsFromPath(extractedArchive.resolve(KROXY_INSTALL_DIR_NAME));
        } catch (IOException e) {
            throw new SetupException("Unable to load Kroxylicious resources: " + e.getMessage());
        }

        allResources = ResourceOrder.sort(KubeResourceManager.get().kubeClient().getClient().load(multiYaml).items());
        LOGGER.info("Loaded {} resources from Kroxy archive", allResources.size());
        prepareKroxyliciousCrs();
    }

    private void prepareKroxyliciousCrs() {
        LOGGER.debug("Setting namespace '{}' on {} Kroxylicious operator resources", deploymentNamespace, allResources.size());
        allResources.forEach(resource -> {
            SetupUtils.setNamespaceOnNamespacedResources(resource, deploymentNamespace);
            SetupUtils.removeSecurityContexts(resource);
            SetupUtils.fixClusterRoleBindingNamespace(resource, deploymentNamespace);
        });
    }

    public void setup() {
        LOGGER.info("----------- Install Kroxylicious Cluster Operator -----------");

        LOGGER.debug("Applying {} Kroxylicious operator resources to namespace '{}'", allResources.size(), deploymentNamespace);
        allResources.forEach(resource -> KubeResourceManager.get().createOrUpdateResourceWithoutWait(resource));

        //Additional check that Kroxy deployment was installed
        LOGGER.debug("Waiting for Kroxylicious operator deployment '{}' to become ready in namespace '{}'", deploymentName, deploymentNamespace);
        WaitUtils.waitForDeploymentWithPrefixIsReady(deploymentNamespace, deploymentName);

        // Allow resource manager delete
        KubeResourceManager.get().pushToStack(new ResourceItem<>(this::teardown));
        LOGGER.info("Installation of Kroxylicious operator completed");
    }

    public void teardown() {
        LOGGER.info("----------- Uninstall Kroxylicious Cluster Operator -----------");
        LOGGER.debug("Deleting {} Kroxylicious operator resources from namespace '{}'", allResources.size(), deploymentNamespace);
        allResources.forEach(resource -> KubeResourceManager.get().deleteResourceWithoutWait(resource));
    }
}
