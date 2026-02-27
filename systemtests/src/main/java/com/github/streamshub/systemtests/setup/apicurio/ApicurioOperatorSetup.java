package com.github.streamshub.systemtests.setup.apicurio;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.ResourceItem;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public class ApicurioOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(ApicurioOperatorSetup.class);

    private static final String APICURIO_OPERATOR_WATCHED_NAMESPACES = "APICURIO_OPERATOR_WATCHED_NAMESPACES";
    private static final String APICURIO_OPERATOR_NAME = "apicurio-registry-operator-v" + Environment.APICURIO_VERSION;
    private static final String APICURIO_BUNDLE_URL = "https://github.com/Apicurio/apicurio-registry/releases/download/"
        + Environment.APICURIO_VERSION + "/apicurio-registry-operator-" + Environment.APICURIO_VERSION + ".tar.gz";
    private static final String APICURIO_TEMP_FILE_PREFIX = "apicurio_tmp";
    private static final String APICURIO_INSTALL_DIR_NAME = "install";
    private static final String APICURIO_INSTALL_FILE_NAME = "install.yaml";

    private Path extractedArchive;

    private final String operatorNamespace;
    private final String watchNamespace;
    private List<HasMetadata> allResources;

    public ApicurioOperatorSetup(String operatorNamespace, String watchNamespace) {
        this.operatorNamespace = operatorNamespace;
        this.watchNamespace = watchNamespace;

        try {
            extractedArchive = FileUtils.downloadAndExtractTarGz(APICURIO_BUNDLE_URL, APICURIO_TEMP_FILE_PREFIX);
            InputStream installYaml = FileUtils.loadYamlsFromPath(extractedArchive.resolve(APICURIO_INSTALL_DIR_NAME).resolve(APICURIO_INSTALL_FILE_NAME));
            // Skip Namespace resources — the test framework manages namespaces separately
            allResources = KubeResourceManager.get().kubeClient().getClient().load(installYaml).items().stream().toList();
            LOGGER.info("Loaded {} resources from Apicurio install.yaml", allResources.size());
        } catch (IOException e) {
            throw new SetupException("Unable to load Apicurio Registry operator resources: " + e.getMessage());
        }

        prepareApicurioCrs();
    }

    /**
     * Prepares Apicurio operator resources before deployment.
     *
     * <p>The following modifications are performed:</p>
     * <ul>
     *   <li>Sets the target namespace on namespaced resources (ServiceAccount, Deployment).</li>
     *   <li>Removes pod-level and container-level {@code securityContext} entries from
     *       {@link Deployment} resources when running on OpenShift.</li>
     *   <li>Rewrites subject namespaces in {@link ClusterRoleBinding} resources to the
     *       operator namespace.</li>
     *   <li>Sets the {@code WATCH_NAMESPACE} env var on the operator Deployment so it
     *       only reconciles ApicurioRegistry CRs in the configured watch namespace.</li>
     * </ul>
     */
    private void prepareApicurioCrs() {
        allResources.forEach(resource -> {
            String kind = resource.getKind();

            // Set namespace on namespaced resources
            if (HasMetadata.getKind(ServiceAccount.class).equals(kind) ||
                HasMetadata.getKind(Deployment.class).equals(kind)) {
                resource.getMetadata().setNamespace(operatorNamespace);
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
            if (HasMetadata.getKind(ClusterRoleBinding.class).equals(kind)) {
                ClusterRoleBinding crb = (ClusterRoleBinding) resource;
                if (crb.getSubjects() != null) {
                    crb.getSubjects().forEach(subject -> {
                        LOGGER.info("Replacing subject namespace with '{}' in ClusterRoleBinding: {}",
                            operatorNamespace, crb.getMetadata().getName());
                        subject.setNamespace(operatorNamespace);
                    });
                }
            }

            // Set APICURIO_OPERATOR_WATCHED_NAMESPACES env var on the operator Deployment
            if (HasMetadata.getKind(Deployment.class).equals(kind)) {
                Deployment deployment = (Deployment) resource;
                deployment.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
                    if (container.getEnv() != null) {
                        container.getEnv().stream()
                            .filter(env -> APICURIO_OPERATOR_WATCHED_NAMESPACES.equals(env.getName()))
                            .findFirst()
                            .ifPresentOrElse(
                                env -> {
                                    LOGGER.info("Overriding APICURIO_OPERATOR_WATCHED_NAMESPACES to '{}' in Deployment: {}", watchNamespace, deployment.getMetadata().getName());
                                    // Clear the OLM fieldRef and set a plain value instead
                                    env.setValueFrom(null);
                                    env.setValue(watchNamespace);
                                },
                                () -> {
                                    LOGGER.info("Adding APICURIO_OPERATOR_WATCHED_NAMESPACES='{}' to Deployment: {}", watchNamespace, deployment.getMetadata().getName());
                                    container.getEnv().add(
                                        new EnvVarBuilder()
                                            .withName(APICURIO_OPERATOR_WATCHED_NAMESPACES)
                                            .withValue(watchNamespace)
                                            .build()
                                    );
                                }
                            );
                    }
                });
            }
        });
    }

    public void setup() {
        LOGGER.info("----------- Install Apicurio Registry Operator -----------");

        LOGGER.info("Installing Apicurio Registry operator from: {}", APICURIO_BUNDLE_URL);
        allResources.forEach(resource -> KubeResourceManager.get().createOrUpdateResourceWithoutWait(resource));

        WaitUtils.waitForDeploymentWithPrefixIsReady(operatorNamespace, APICURIO_OPERATOR_NAME);
        // Allow resource manager to clean up on test teardown
        KubeResourceManager.get().pushToStack(new ResourceItem<>(this::teardown));
        LOGGER.info("Installation of Apicurio Registry operator completed");
    }

    public void teardown() {
        LOGGER.info("----------- Uninstall Apicurio Registry Operator -----------");
        allResources.forEach(resource -> KubeResourceManager.get().deleteResourceWithoutWait(resource));
    }
}
