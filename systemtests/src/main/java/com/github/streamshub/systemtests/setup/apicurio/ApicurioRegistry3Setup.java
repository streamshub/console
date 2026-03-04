package com.github.streamshub.systemtests.setup.apicurio;

import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.apicurio.registry.operator.api.v1.ApicurioRegistry3;
import io.apicurio.registry.operator.api.v1.ApicurioRegistry3Spec;
import io.apicurio.registry.operator.api.v1.spec.AppSpec;
import io.apicurio.registry.operator.api.v1.spec.IngressSpec;
import io.apicurio.registry.operator.api.v1.spec.UiSpec;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class ApicurioRegistry3Setup {
    private static final Logger LOGGER = LogWrapper.getLogger(ApicurioRegistry3Setup.class);
    private ApicurioRegistry3Setup() {}

    // -------------------------------------------------------------------------
    // Setup entrypoints
    // -------------------------------------------------------------------------

    /**
     * Deploys an Apicurio Registry 3 instance with in-memory storage (dev/testing only).
     * Data is lost on pod restart.
     */
    public static void setupMemIfNeeded(String namespace, String registryName) {
        LOGGER.info("----------- Deploy Apicurio Registry Instance (mem) -----------");
        if (ResourceUtils.getKubeResource(ApicurioRegistry3.class, namespace, registryName) != null) {
            LOGGER.warn("Skipping Apicurio Registry deployment, already present");
            return;
        }
        KubeResourceManager.get().createResourceWithWait(getDefaultApicurioRegistryMem(namespace, registryName));
        LOGGER.info("Apicurio Registry (mem) '{}' deployed in namespace '{}'", registryName, namespace);
    }

    public static ApicurioRegistry3 getDefaultApicurioRegistryMem(String namespace, String registryName) {
        // ApicurioRegistry3 has lombok generated builder
        AppSpec appSpec = AppSpec.builder()
            .ingress(IngressSpec.builder()
                .host(registryName + "-app-service-" + namespace + "." + ClusterUtils.getClusterDomain())
                .build())
            .env(List.of(
                new EnvVarBuilder()
                    .withName("APICURIO_REST_DELETION_ARTIFACT_ENABLED")
                    .withValue("true")
                    .build(),
                new EnvVarBuilder()
                    .withName("APICURIO_REST_DELETION_ARTIFACT_VERSION_ENABLED")
                    .withValue("true")
                    .build(),
                new EnvVarBuilder()
                    .withName("APICURIO_REST_DELETION_GROUP_ENABLED")
                    .withValue("true")
                    .build()
            ))
            .build();

        UiSpec uiSpec = UiSpec.builder()
            .ingress(IngressSpec.builder()
                .host(registryName + "-ui-service-" + namespace + "." + ClusterUtils.getClusterDomain())
                .build())
            .build();

        ApicurioRegistry3Spec spec = ApicurioRegistry3Spec.builder()
            .app(appSpec)
            .ui(uiSpec)
            .build();

        ApicurioRegistry3 registry = new ApicurioRegistry3();
        registry.setMetadata(new ObjectMetaBuilder()
            .withName(registryName)
            .withNamespace(namespace)
            .build());
        registry.setSpec(spec);
        return registry;
    }
}