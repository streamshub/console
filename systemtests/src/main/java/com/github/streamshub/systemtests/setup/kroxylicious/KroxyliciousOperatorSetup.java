package com.github.streamshub.systemtests.setup.kroxylicious;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.ResourceItem;
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
    private static final String KROXY_EXAMPLES_DIR_NAME = "examples";
    private Path extractedArchive;

    private final String deploymentNamespace;
    private final String deploymentName;
    private List<HasMetadata> allResources;

    public KroxyliciousOperatorSetup(String deploymentNamespace) {
        this.deploymentNamespace = deploymentNamespace;
        this.deploymentName = KROXYLICIOUS_OPERATOR_NAME;

        try {
            extractedArchive = FileUtils.downloadAndExtractTarGz(KROXYLICIOUS_BUNDLE_URL, KROXY_TEMP_FILE_PREFIX);

            InputStream multiYaml = FileUtils.loadYamlsFromPath(extractedArchive.resolve(KROXY_INSTALL_DIR_NAME));

            // Skip Namespace resource — the test framework manages namespaces separately
            allResources = KubeResourceManager.get()
                .kubeClient()
                .getClient()
                .load(multiYaml)
                .items()
                .stream()
                .filter(resource -> !HasMetadata.getKind(Namespace.class).equals(resource.getKind()))
                .toList();

            LOGGER.info("Loaded {} resources from archive", allResources.size());
        } catch (IOException e) {
            throw new SetupException("Unable to load Kroxylicious resources: " + e.getMessage());
        }
        prepareKroxyliciousCrs();
    }

    private void prepareKroxyliciousCrs() {
        allResources.forEach(resource -> {
            String kind = resource.getKind();

            // Resources must have namespace set for creation
            if (HasMetadata.getKind(ServiceAccount.class).equals(kind) ||
                HasMetadata.getKind(Deployment.class).equals(kind)) {
                resource.getMetadata().setNamespace(deploymentNamespace);
            }

            // Remove securityContext from Deployments on OpenShift clusters
            if (HasMetadata.getKind(Deployment.class).equals(kind) && ClusterUtils.isOcp()) {
                Deployment deployment = (Deployment) resource;
                LOGGER.info("Removing securityContext from Deployment: {}",
                    deployment.getMetadata().getName());

                PodSpec podSpec = deployment.getSpec().getTemplate().getSpec();

                if (podSpec.getSecurityContext() != null) {
                    podSpec.setSecurityContext(null);
                }

                podSpec.getContainers().forEach(container -> {
                    if (container.getSecurityContext() != null) {
                        container.setSecurityContext(null);
                    }
                });

                if (podSpec.getInitContainers() != null) {
                    podSpec.getInitContainers().forEach(container -> {
                        if (container.getSecurityContext() != null) {
                            container.setSecurityContext(null);
                        }
                    });
                }
            }

            // Fix ClusterRoleBinding subjects namespace
            // The kroxylicious install YAMLs hardcode 'kroxylicious-operator' as the subject namespace, so replace that
            if (HasMetadata.getKind(ClusterRoleBinding.class).equals(kind)) {
                ClusterRoleBinding crb = (ClusterRoleBinding) resource;
                if (crb.getSubjects() != null) {
                    crb.getSubjects().forEach(subject -> {
                        subject.setNamespace(deploymentNamespace);
                    });
                }
            }
        });
    }

    public void setup() {
        LOGGER.info("----------- Install Kroxylicious Cluster Operator -----------");

        LOGGER.info("Install Kroxylicious Using YAML");
        allResources.forEach(resource -> KubeResourceManager.get().createOrUpdateResourceWithoutWait(resource));

        //Additional check that Kroxy deployment was installed
        WaitUtils.waitForDeploymentWithPrefixIsReady(deploymentNamespace, deploymentName);

        // Allow resource manager delete
        KubeResourceManager.get().pushToStack(new ResourceItem<>(this::teardown));
        LOGGER.info("Installation of Kroxylicious operator completed");
    }

    public void teardown() {
        LOGGER.info("----------- Uninstall Kroxylicious Cluster Operator -----------");
        allResources.forEach(resource -> KubeResourceManager.get().deleteResourceWithoutWait(resource));
    }
}
