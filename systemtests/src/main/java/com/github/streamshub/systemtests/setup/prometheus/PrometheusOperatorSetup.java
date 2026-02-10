package com.github.streamshub.systemtests.setup.prometheus;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.ResourceItem;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class PrometheusOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(PrometheusOperatorSetup.class);
    private static final String PROMETHEUS_OPERATOR_NAME = "prometheus-operator";
    private static final String PROMETHEUS_BUNDLE_URL = "https://github.com/prometheus-operator/prometheus-operator/releases/download/v0.89.0/bundle.yaml";

    private final String deploymentNamespace;
    private final String deploymentName;
    private List<HasMetadata> allResources;

    public PrometheusOperatorSetup(String deploymentNamespace) {
        this.deploymentNamespace = deploymentNamespace;
        this.deploymentName = PROMETHEUS_OPERATOR_NAME;

        InputStream multiYaml = null;
        try {
            multiYaml = FileUtils.getDeploymentFileFromURL(PROMETHEUS_BUNDLE_URL);
        } catch (IOException e) {
            throw new SetupException("Unable to load prometheus CRs: " + e.getMessage());
        }
        allResources = KubeResourceManager.get().kubeClient().getClient().load(multiYaml).items();
        LOGGER.info("Loaded {} resources from YAML", allResources.size());
        preparePrometheusCrs();
    }

    /**
     * Prepares Prometheus-related resources before deployment.
     *
     * <p>This method iterates over all loaded Kubernetes resources and applies
     * environment-specific adjustments to ensure they can be safely deployed
     * into the target namespace.</p>
     *
     * <p>The following modifications are performed:</p>
     * <ul>
     *   <li>Sets the target namespace on all namespaced resources, excluding
     *       {@link ClusterRole}, {@link ClusterRoleBinding}, and
     *       {@link CustomResourceDefinition}.</li>
     *   <li>Removes pod-level and container-level {@code securityContext} entries
     *       from {@link Deployment} resources to avoid permission and SCC issues
     *       in restricted environments.</li>
     *   <li>Rewrites subject namespaces in {@link ClusterRoleBinding} resources
     *       from {@code default} to the configured deployment namespace.</li>
     *   <li>Rewrites subject namespaces in {@link RoleBinding} resources
     *       from {@code default} to the configured deployment namespace.</li>
     * </ul>
     *
     * <p>This normalization step ensures that Prometheus CRs and related RBAC
     * resources are compatible with the target cluster and namespace configuration
     * before being applied.</p>
     */
    private void preparePrometheusCrs() {
        allResources.forEach(resource -> {
            String kind = resource.getKind();

            if (!HasMetadata.getKind(ClusterRole.class).equals(kind) &&
                !HasMetadata.getKind(ClusterRoleBinding.class).equals(kind) &&
                !HasMetadata.getKind(CustomResourceDefinition.class).equals(kind)) {
                resource.getMetadata().setNamespace(deploymentNamespace);
            }

            // Remove securityContext from Deployments
            if (HasMetadata.getKind(Deployment.class).equals(resource.getKind())) {
                Deployment deployment = (Deployment) resource;
                LOGGER.info("Removing securityContext from Deployment: {}",
                    deployment.getMetadata().getName());

                // Remove pod-level securityContext
                if (deployment.getSpec().getTemplate().getSpec().getSecurityContext() != null) {
                    deployment.getSpec().getTemplate().getSpec().setSecurityContext(null);
                }

                // Remove container-level securityContext
                deployment.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
                    if (container.getSecurityContext() != null) {
                        container.setSecurityContext(null);
                    }
                });

                // Also remove from initContainers if any
                if (deployment.getSpec().getTemplate().getSpec().getInitContainers() != null) {
                    deployment.getSpec().getTemplate().getSpec().getInitContainers().forEach(container -> {
                        if (container.getSecurityContext() != null) {
                            container.setSecurityContext(null);
                        }
                    });
                }
            }

            // Fix ClusterRoleBinding subjects namespace
            if (HasMetadata.getKind(ClusterRoleBinding.class).equals(resource.getKind())) {
                ClusterRoleBinding crb = (ClusterRoleBinding) resource;
                if (crb.getSubjects() != null) {
                    crb.getSubjects().forEach(subject -> {
                        if (Constants.DEFAULT_NAMESPACE.equals(subject.getNamespace())) {
                            LOGGER.info("Replacing subject namespace 'default' with '{}' in ClusterRoleBinding: {}",
                                deploymentNamespace, crb.getMetadata().getName());
                            subject.setNamespace(deploymentNamespace);
                        }
                    });
                }
            }

            // Fix RoleBinding subjects namespace
            if (HasMetadata.getKind(RoleBinding.class).equals(resource.getKind())) {
                RoleBinding rb = (RoleBinding) resource;
                if (rb.getSubjects() != null) {
                    rb.getSubjects().forEach(subject -> {
                        if (Constants.DEFAULT_NAMESPACE.equals(subject.getNamespace())) {
                            LOGGER.info("Replacing subject namespace 'default' with '{}' in RoleBinding: {}",
                                deploymentNamespace, rb.getMetadata().getName());
                            subject.setNamespace(deploymentNamespace);
                        }
                    });
                }
            }
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
