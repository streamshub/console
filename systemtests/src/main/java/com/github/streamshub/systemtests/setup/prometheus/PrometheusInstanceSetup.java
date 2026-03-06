package com.github.streamshub.systemtests.setup.prometheus;

import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.SetupUtils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceOrder;
import io.fabric8.kubernetes.api.model.HasMetadata;
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

        try {
            Path directory = Paths.get(PROMETHEUS_INSTANCE_EXAMPLES);
            try (Stream<Path> paths = Files.list(directory)) {
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

        allResources = ResourceOrder.sort(allResources);
        LOGGER.info("Loaded {} resources from Prometheus operator YAML", allResources.size());
        preparePrometheusCrs();
    }

    private void preparePrometheusCrs() {
        allResources.forEach(resource -> {
            SetupUtils.setNamespaceOnNamespacedResources(resource, deploymentNamespace);
            SetupUtils.fixClusterRoleBindingNamespace(resource, deploymentNamespace);
        });
    }

    public void setup() {
        allResources.forEach(resource -> KubeResourceManager.get().createOrUpdateResourceWithoutWait(resource));
        // Additional check that Prometheus pod is running
        WaitUtils.waitForPodsReadyAndStable(deploymentNamespace, Labels.getPrometheusInstanceLabel(deploymentName), 1, true);
    }

    public String getName() {
        return this.deploymentName;
    }

    public String getPrometheusServerUrl() {
        return "http://" + this.deploymentName + "." + this.deploymentNamespace + ".svc.cluster.local:9090";
    }
}
