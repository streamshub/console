package com.github.streamshub.systemtests.utils;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.resourcetypes.apicurio.ApicurioRegistry3Type;
import com.github.streamshub.systemtests.resourcetypes.console.ConsoleType;
import com.github.streamshub.systemtests.resourcetypes.kafka.KafkaConnectType;
import com.github.streamshub.systemtests.resourcetypes.kafka.KafkaTopicType;
import com.github.streamshub.systemtests.resourcetypes.kafka.KafkaType;
import com.github.streamshub.systemtests.resourcetypes.kafka.KafkaUserType;
import com.github.streamshub.systemtests.resourcetypes.kroxy.KroxyResourceType;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProtocolFilter;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxy;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaProxyIngress;
import io.kroxylicious.kubernetes.api.v1alpha1.KafkaService;
import io.kroxylicious.kubernetes.api.v1alpha1.VirtualKafkaCluster;
import io.skodjob.testframe.resources.ClusterRoleBindingType;
import io.skodjob.testframe.resources.ClusterRoleType;
import io.skodjob.testframe.resources.ConfigMapType;
import io.skodjob.testframe.resources.CustomResourceDefinitionType;
import io.skodjob.testframe.resources.DeploymentType;
import io.skodjob.testframe.resources.InstallPlanType;
import io.skodjob.testframe.resources.JobType;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.NamespaceType;
import io.skodjob.testframe.resources.OperatorGroupType;
import io.skodjob.testframe.resources.RoleBindingType;
import io.skodjob.testframe.resources.SecretType;
import io.skodjob.testframe.resources.ServiceAccountType;
import io.skodjob.testframe.resources.ServiceType;
import io.skodjob.testframe.resources.SubscriptionType;
import io.skodjob.testframe.utils.KubeUtils;
import org.apache.logging.log4j.Logger;
import org.slf4j.event.Level;

import java.io.IOException;

@SuppressWarnings("ClassDataAbstractionCoupling")
public class SetupUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(SetupUtils.class);
    private SetupUtils() {}

    /**
     * Initializes the testing tools and Kubernetes resource manager for system tests.
     *
     * <p>This method ensures that resource types, callbacks, and logging
     * configurations are set up only once, at the start of test execution.
     * Calling it multiple times has no effect after the first invocation.</p>
     *
     * <p>Unlike placing this logic in a {@code static {}} block, this method
     * avoids executing prematurely when the test listener scans class names
     * and methods, preventing unwanted logging or side effects before actual
     * test cases run.</p>
     *
     * <p>Specifically, it:</p>
     * <ul>
     *   <li>Registers all custom Kubernetes resource types used in the tests.</li>
     *   <li>Adds a callback to label newly created namespaces for log collection.</li>
     *   <li>Configures the path for storing YAML representations of resources.</li>
     *   <li>Logs the environment configuration and saves it to a file.</li>
     * </ul>
     */
    public static void initializeSystemTests() {
        KubeResourceManager.get().setResourceTypes(
            new CustomResourceDefinitionType(),
            new ClusterRoleBindingType(),
            new ClusterRoleType(),
            new ConfigMapType(),
            new ConsoleType(),
            new DeploymentType(),
            new InstallPlanType(),
            new JobType(),
            new KafkaType(),
            new KafkaTopicType(),
            new KafkaUserType(),
            new KafkaConnectType(),
            new NamespaceType(),
            new OperatorGroupType(),
            new RoleBindingType(),
            new SecretType(),
            new ServiceAccountType(),
            new ServiceType(),
            new SubscriptionType(),
            new KroxyResourceType<>(KafkaProxy.class),
            new KroxyResourceType<>(KafkaService.class),
            new KroxyResourceType<>(KafkaProxyIngress.class),
            new KroxyResourceType<>(VirtualKafkaCluster.class),
            new KroxyResourceType<>(KafkaProtocolFilter.class),
            new ApicurioRegistry3Type());

        KubeResourceManager.get().addCreateCallback(resource -> {
            // Set collect label for every namespace created with TF
            if (resource instanceof Namespace) {
                KubeUtils.labelNamespace(resource.getMetadata().getName(), Labels.COLLECT_ST_LOGS, "true");
            }
        });

        // Allow storing YAML files
        KubeResourceManager.get().setStoreYamlPath(Environment.TEST_LOG_DIR);

        try {
            Environment.logConfigAndSaveToFile();
        } catch (IOException e) {
            LOGGER.error("Saving of config env file failed");
        }
    }

    public static void cleanupIfNeeded() {
        if (Environment.CLEANUP_ENVIRONMENT) {
            KubeResourceManager.get().printCurrentResources(Level.DEBUG);
            KubeResourceManager.get().deleteResources(false);
        }
    }

    /**
     * Removes pod and container {@code securityContext} settings from a Deployment
     * when running on OpenShift.
     *
     * <p>If the provided resource is a {@link Deployment} and the cluster is
     * detected as OpenShift, this method clears:</p>
     * <ul>
     *   <li>The pod-level {@code securityContext}</li>
     *   <li>The {@code securityContext} of all containers</li>
     *   <li>The {@code securityContext} of all init containers (if present)</li>
     * </ul>
     *
     * <p>This helps avoid permission or SCC-related conflicts in restricted
     * OpenShift environments.</p>
     *
     * @param resource the Kubernetes resource to inspect and normalize
     */
    public static void removeSecurityContexts(HasMetadata resource) {
        if (resource instanceof Deployment deployment && ClusterUtils.isOcp()) {
            PodSpec podSpec = deployment.getSpec().getTemplate().getSpec();

            if (podSpec.getSecurityContext() != null) {
                LOGGER.info("Removing pod securityContext from Deployment: {}", deployment.getMetadata().getName());
                podSpec.setSecurityContext(null);
            }

            podSpec.getContainers().forEach(container -> {
                if (container.getSecurityContext() != null) {
                    LOGGER.info("Removing container securityContext from Deployment: {}", deployment.getMetadata().getName());
                    container.setSecurityContext(null);
                }
            });

            if (podSpec.getInitContainers() != null) {
                podSpec.getInitContainers().forEach(container -> {
                    if (container.getSecurityContext() != null) {
                        LOGGER.info("Removing init container securityContext from Deployment: {}", deployment.getMetadata().getName());

                        container.setSecurityContext(null);
                    }
                });
            }
        }
    }

    /**
     * Rewrites subject namespaces in a {@link ClusterRoleBinding}.
     *
     * <p>If the provided resource is a {@link ClusterRoleBinding}, this method
     * updates the namespace of all defined subjects to the given target namespace.
     * This ensures RBAC bindings reference the correct namespace in
     * namespace-scoped deployments.</p>
     *
     * @param resource         the Kubernetes resource to inspect
     * @param targetNamespace  the namespace to set on all subjects
     */
    public static void fixClusterRoleBindingNamespace(HasMetadata resource, String targetNamespace) {
        if (resource instanceof ClusterRoleBinding crb) {
            if (crb.getSubjects() != null) {
                crb.getSubjects().forEach(subject -> {
                    LOGGER.info("Setting subject namespace to '{}' in ClusterRoleBinding: {}",
                        targetNamespace, crb.getMetadata().getName());
                    subject.setNamespace(targetNamespace);
                });
            }
        }
    }

    /**
     * Rewrites subject namespaces in a {@link RoleBinding}.
     *
     * <p>If the provided resource is a {@link RoleBinding}, this method
     * updates the namespace of all defined subjects to the given target namespace.
     * This ensures RBAC bindings reference the correct namespace in
     * namespace-scoped deployments.</p>
     *
     * @param resource         the Kubernetes resource to inspect
     * @param targetNamespace  the namespace to set on all subjects
     */
    public static void fixRoleBindingNamespace(HasMetadata resource, String targetNamespace) {
        if (resource instanceof RoleBinding rb) {
            if (rb.getSubjects() != null) {
                rb.getSubjects().forEach(subject -> {
                    LOGGER.info("Setting subject namespace to '{}' in RoleBinding: {}",
                        targetNamespace, rb.getMetadata().getName());
                    subject.setNamespace(targetNamespace);
                });
            }
        }
    }

    /**
     * Sets the namespace on supported namespaced Kubernetes resources.
     *
     * <p> If the provided resource is instance of Namespace, this method updates its metadata namespace
     * to the specified target namespace.</p>
     *
     * @param resource        the Kubernetes resource to update
     * @param targetNamespace the namespace to assign to the resource
     */
    public static void setNamespaceOnNamespacedResources(HasMetadata resource, String targetNamespace) {
        if (resource instanceof Namespaced) {
            LOGGER.info("Setting resource {} to namespace {}", resource.getMetadata().getName(), targetNamespace);
            resource.getMetadata().setNamespace(targetNamespace);
        }
    }
}
