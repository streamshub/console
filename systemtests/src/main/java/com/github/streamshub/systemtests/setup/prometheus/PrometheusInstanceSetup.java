package com.github.streamshub.systemtests.setup.prometheus;

import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.WaitUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.skodjob.testframe.TestFrameEnv;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class PrometheusInstanceSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(PrometheusInstanceSetup.class);
    private static final String PROMETHEUS_INSTANCE_NAME = "console-prometheus";
    private static final String PROMETHEUS_INSTANCE_EXAMPLES = TestFrameEnv.USER_PATH + "/../examples/prometheus";
    private List<HasMetadata> allResources = new ArrayList<>();

    private final String deploymentNamespace;
    private final String deploymentName;

    public PrometheusInstanceSetup(String namespace) {
        this.deploymentNamespace = namespace;
        this.deploymentName = PROMETHEUS_INSTANCE_NAME;
        preparePrometheusCrs();
    }

    /**
     * Loads and prepares Prometheus example Custom Resources for deployment.
     *
     * <p>This method reads all YAML manifests from the configured Prometheus
     * examples directory and loads them into Kubernetes resource objects using
     * the Fabric8 client.</p>
     *
     * <p>Once loaded, the following adjustments are applied to ensure the resources
     * can be deployed correctly into the target namespace:</p>
     * <ul>
     *   <li>All namespaced resources are assigned the configured deployment
     *       namespace.</li>
     *   <li>{@link ClusterRole} and {@link ClusterRoleBinding} resources are left
     *       cluster-scoped and are not assigned a namespace.</li>
     *   <li>All {@link ClusterRoleBinding} subject namespaces are rewritten from
     *       {@code default} to the configured deployment namespace.</li>
     * </ul>
     *
     * <p>If any YAML file cannot be read or parsed, a {@link SetupException} is
     * thrown to fail the setup early and surface configuration issues.</p>
     *
     * <p>This preparation step ensures Prometheus resources are namespace-aware
     * and compatible with the target test environment before being applied.</p>
     */
    private void preparePrometheusCrs() {
        try {
            Path directory = Paths.get(PROMETHEUS_INSTANCE_EXAMPLES);
            try (Stream<Path> paths = Files.walk(directory)) {
                paths.filter(Files::isRegularFile)
                     .filter(path -> path.toString().endsWith(".yaml") || path.toString().endsWith(".yml"))
                     .forEach(path -> {
                         try (InputStream stream = Files.newInputStream(path)) {
                             List<HasMetadata> resources = KubeResourceManager.get()
                                 .kubeClient()
                                 .getClient()
                                 .load(stream)
                                 .items();
                             allResources.addAll(resources);
                         } catch (IOException e) {
                             throw new UncheckedIOException(e);
                         }
                     });
            }
        } catch (IOException e) {
            throw new SetupException("Unable to parse prometheus examples: " + e.getMessage());
        }

        allResources.forEach(resource -> {
            String kind = resource.getKind();
            // Set namespace for all resources except cluster-scoped ones
            if (!HasMetadata.getKind(ClusterRole.class).equals(kind) &&
                !HasMetadata.getKind(ClusterRoleBinding.class).equals(kind)) {
                resource.getMetadata().setNamespace(deploymentNamespace);
            }

            // Set ClusterRoleBinding subjects namespace
            if (HasMetadata.getKind(ClusterRoleBinding.class).equals(kind)) {
                ClusterRoleBinding crb = (ClusterRoleBinding) resource;
                if (crb.getSubjects() != null) {
                    crb.getSubjects().forEach(subject -> {
                        LOGGER.info("Replacing subject namespace 'default' with '{}' in ClusterRoleBinding: {}",
                                deploymentNamespace, crb.getMetadata().getName());
                        subject.setNamespace(deploymentNamespace);
                    });
                }
            }
        });
    }

    public void setup() {
        allResources.forEach(resource -> KubeResourceManager.get().createOrUpdateResourceWithoutWait(resource));
        // Additional check that Prometheus pod is running
        WaitUtils.waitForPodsReadyAndStable(deploymentNamespace,
            new LabelSelectorBuilder().withMatchLabels(Map.of(Constants.PROMETHEUS_NAME, deploymentName)).build(),
            1, true);
    }

    public String getName() {
        return this.deploymentName;
    }

    public String getPrometheusServerUrl() {
        return "http://" + this.deploymentName + "." + this.deploymentNamespace + ".svc.cluster.local:9090";
    }
}
