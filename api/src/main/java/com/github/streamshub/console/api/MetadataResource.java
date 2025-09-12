package com.github.streamshub.console.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import com.github.streamshub.console.config.ConsoleConfig;

import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.base.ResourceDefinitionContext;

@Path("/api/metadata")
@Tag(name = "Application Metadata Resource")
@ApplicationScoped
public class MetadataResource {

    private static final String VERSION = "version";

    static final ResourceDefinitionContext OCP_VERSION_CONTEXT = new ResourceDefinitionContext.Builder()
            .withGroup("config.openshift.io")
            .withKind("ClusterVersion")
            .withNamespaced(false)
            .withVersion("v1")
            .build();

    @Inject
    Logger logger;

    @Inject
    KubernetesClient k8s;

    @Inject
    Config config;

    @Inject
    ConsoleConfig consoleConfig;

    Optional<Map<String, Object>> metadata = Optional.empty();

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", ref = "Application Metadata", content = @Content())
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public Response getMetadata() {
        return Response.ok(metadata.orElseGet(this::initializeMetadata)).build();
    }

    /* test */ void clearMetadata() {
        metadata = Optional.empty();
    }

    private Map<String, Object> initializeMetadata() {
        Map<String, String>  meta = new LinkedHashMap<>();

        meta.put(VERSION, config
                .getValue("console.meta.version", String.class));
        meta.put("platform", config
                .getOptionalValue("console.meta.platform", String.class)
                .orElseGet(this::determinePlatform));

        var replacementMetadata = Optional.<Map<String, Object>>of(Map.of(
            "data", Map.of(
                "id", "console-meta",
                "type", "metadata",
                "attributes", meta
            )
        ));

        if (metadata.isEmpty()) {
            synchronized (this) {
                if (metadata.isEmpty()) {
                    metadata = replacementMetadata;
                }
            }
        }

        return metadata.get();
    }

    private String determinePlatform() {
        if (!consoleConfig.getKubernetes().isEnabled()) {
            return "Unknown";
        }

        var versionInfo = k8s.getKubernetesVersion();
        boolean openshift = false;
        String platform;
        String version;

        try {
            openshift = k8s.getApiGroup("project.openshift.io") != null;
        } catch (KubernetesClientException e) {
            logger.warnf("Unable to determine if cluster is OpenShift - %s", e.getMessage());
        }

        if (openshift) {
            GenericKubernetesResource clusterVersion = null;

            try {
                clusterVersion = k8s.genericKubernetesResources(OCP_VERSION_CONTEXT)
                        .withName(VERSION)
                        .get();
            } catch (KubernetesClientException e) {
                logger.warnf("Unknown OpenShift version; %s", e.getMessage());
            }

            if (clusterVersion != null) {
                version = clusterVersion.<String>get("status", "desired", VERSION);

                if (version == null) {
                    logger.warnf("Unknown OpenShift version; `.status.desired.version` not in ClusterVersion");
                    version = "(Kubernetes %s.%s)".formatted(versionInfo.getMajor(), versionInfo.getMinor());
                }
            } else {
                version = "(Kubernetes %s.%s)".formatted(versionInfo.getMajor(), versionInfo.getMinor());
            }
        } else {
            version = "%s.%s".formatted(versionInfo.getMajor(), versionInfo.getMinor());
        }

        platform = openshift ? "OpenShift " : "Kubernetes ";
        return platform + version;
    }
}
