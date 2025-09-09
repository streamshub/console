package com.github.streamshub.console.api;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import com.github.streamshub.console.config.ConsoleConfig;

import io.fabric8.kubernetes.api.model.APIGroup;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;

@Path("/api/metadata")
@Tag(name = "Application Metadata Resource")
@ApplicationScoped
public class MetadataResource {

    private static final String VERSION = "version";

    @Inject
    Logger logger;

    @Inject
    KubernetesClient k8s;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    @ConfigProperty(name = "console.meta.version")
    String consoleVersion;

    @Inject
    @ConfigProperty(name = "console.meta.platform")
    Optional<String> consolePlatform;

    Map<String, Object> metadata;

    @PostConstruct
    void initialize() {
        Map<String, String>  meta = new LinkedHashMap<>();
        meta.put(VERSION, consoleVersion);
        meta.put("platform", consolePlatform.orElseGet(this::determinePlatform));
        this.metadata = Map.of(
                "data", Map.of(
                    "id", "meta",
                    "type", "metadata",
                    "attributes", meta
                )
        );
    }

    private String determinePlatform() {
        if (consoleConfig.getKubernetes().isEnabled()) {
            var versionInfo = k8s.getKubernetesVersion();
            boolean openshift = false;
            String platform;
            String version;

            try {
                openshift = k8s.getApiGroups()
                        .getGroups()
                        .stream()
                        .map(APIGroup::getName)
                        .anyMatch("project.openshift.io"::equals);

                if (openshift) {
                    var clusterVersion = k8s.genericKubernetesResources("config.openshift.io/v1", "ClusterVersion")
                            .withName(VERSION)
                            .get();
                    version = clusterVersion.<String>get("status", "desired", VERSION);
                } else {
                    version = "%s.%s".formatted(versionInfo.getMajor(), versionInfo.getMinor());
                }
            } catch (KubernetesClientException e) {
                if (openshift) {
                    logger.warnf("Unable to determine OpenShift version - %s", e.getMessage());
                    version = "(Kubernetes %s.%s)".formatted(versionInfo.getMajor(), versionInfo.getMinor());
                } else {
                    logger.warnf("Unable to determine if cluster is OpenShift - %s", e.getMessage());
                    version = "%s.%s".formatted(versionInfo.getMajor(), versionInfo.getMinor());
                }
            }

            platform = openshift ? "OpenShift " : "Kubernetes ";
            return platform + version;
        }

        return "Unknown";
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", ref = "Application Metadata", content = @Content())
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public Response getMetadata() {
        return Response.ok(metadata).build();
    }
}
