package com.github.streamshub.console.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;

import io.fabric8.kubernetes.api.model.GenericKubernetesResourceBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionStatus;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceSubresourceStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@TestHTTPEndpoint(MetadataResource.class)
@TestProfile(TestPlainProfile.class)
class MetadataResourceIT {

    @Inject
    Logger logger;

    @Inject
    KubernetesClient k8s;

    @Inject
    MetadataResource resource;

    @BeforeEach
    @AfterEach
    void cleanup() {
        resource.clearMetadata();
        removeOpenShiftMockDefinitions();
    }

    @Test
    void testGetMetadataDefaultKubernetes() {
        whenRequesting(req -> req.get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.id", is("console-meta"))
            .body("data.type", is("metadata"))
            .body("data.attributes.version", is(ConfigProvider.getConfig()
                    .getValue("quarkus.application.version", String.class)))
            // Tests run with dev services vanilla Kube API
            .body("data.attributes.platform", matchesPattern("Kubernetes \\d+\\.\\d+"));
    }

    @Test
    void testGetMetadataOpenShiftUnknownVersion() {
        createOpenShiftProjectDefinition();

        whenRequesting(req -> req.get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.id", is("console-meta"))
            .body("data.type", is("metadata"))
            .body("data.attributes.version", is(ConfigProvider.getConfig()
                    .getValue("quarkus.application.version", String.class)))
            .body("data.attributes.platform", matchesPattern("OpenShift \\(Kubernetes \\d+\\.\\d+\\)"));
    }

    @Test
    void testGetMetadataOpenShiftNullVersion() {
        createOpenShiftProjectDefinition();
        createOpenShiftVersion(null);

        whenRequesting(req -> req.get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.id", is("console-meta"))
            .body("data.type", is("metadata"))
            .body("data.attributes.version", is(ConfigProvider.getConfig()
                    .getValue("quarkus.application.version", String.class)))
            .body("data.attributes.platform", matchesPattern("OpenShift \\(Kubernetes \\d+\\.\\d+\\)"));
    }

    @Test
    void testGetMetadataOpenShiftAvailableVersion() {
        createOpenShiftProjectDefinition();
        createOpenShiftVersion("4.20.0");

        whenRequesting(req -> req.get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.id", is("console-meta"))
            .body("data.type", is("metadata"))
            .body("data.attributes.version", is(ConfigProvider.getConfig()
                    .getValue("quarkus.application.version", String.class)))
            .body("data.attributes.platform", is("OpenShift 4.20.0"));
    }

    // Utility methods

    void removeOpenShiftMockDefinitions() {
        delete(k8s.genericKubernetesResources(MetadataResource.OCP_VERSION_CONTEXT).withName("version"));

        for (var mocked : List.of("clusterversions.config.openshift.io", "projects.project.openshift.io")) {
            try {
                delete(k8s.resources(CustomResourceDefinition.class).withName(mocked));
            } catch (Exception e) {
                logger.warnf(e, "Exception deleting mock OCP resource definition");
            }
        }
    }

    void delete(Resource<?> operation) {
        try {
            operation.withTimeout(10, TimeUnit.SECONDS).delete();
        } catch (KubernetesClientException e) {
            if (e.getCode() != 404) {
                throw e;
            }
        }
    }

    void createOpenShiftProjectDefinition() {
        k8s.resource(new CustomResourceDefinitionBuilder()
                .withNewMetadata()
                    .withName("projects.project.openshift.io")
                .endMetadata()
                .withNewSpec()
                    .withScope("Cluster")
                    .withGroup("project.openshift.io")
                    .withNewNames()
                        .withSingular("project")
                        .withPlural("projects")
                        .withKind("Project")
                        .withListKind("ProjectList")
                    .endNames()
                    .addNewVersion()
                        .withName("v1")
                        .withNewSubresources()
                            .withStatus(new CustomResourceSubresourceStatus())
                        .endSubresources()
                        .withNewSchema()
                            .withNewOpenAPIV3Schema()
                                .withType("object")
                                .withXKubernetesPreserveUnknownFields(true)
                            .endOpenAPIV3Schema()
                        .endSchema()
                        .withStorage(true)
                        .withServed(true)
                    .endVersion()
                .endSpec()
                .build())
            .serverSideApply();

        waitUntilEstablished("projects.project.openshift.io");
    }

    void createOpenShiftVersion(String version) {
        k8s.resource(new CustomResourceDefinitionBuilder()
                .withNewMetadata()
                    .withName("clusterversions.config.openshift.io")
                .endMetadata()
                .withNewSpec()
                    .withScope("Cluster")
                    .withGroup("config.openshift.io")
                    .withNewNames()
                        .withSingular("clusterversion")
                        .withPlural("clusterversions")
                        .withKind("ClusterVersion")
                        .withListKind("ClusterVersionList")
                    .endNames()
                    .addNewVersion()
                        .withName("v1")
                        .withNewSchema()
                            .withNewOpenAPIV3Schema()
                                .withType("object")
                                .withXKubernetesPreserveUnknownFields(true)
                            .endOpenAPIV3Schema()
                        .endSchema()
                        .withStorage(true)
                        .withServed(true)
                    .endVersion()
                .endSpec()
                .build())
            .serverSideApply();

        waitUntilEstablished("clusterversions.config.openshift.io");

        var clusterVersion = k8s
                .genericKubernetesResources(MetadataResource.OCP_VERSION_CONTEXT)
                .resource(new GenericKubernetesResourceBuilder()
                        .withKind("ClusterVersion")
                        .withApiVersion("config.openshift.io/v1")
                        .withNewMetadata()
                            .withName("version")
                        .endMetadata()
                        .addToAdditionalProperties("status", Map.of("desired", Collections.singletonMap("version", version)))
                        .build())
                .serverSideApply();

        assertEquals(version, clusterVersion.<String>get("status", "desired", "version"));
    }

    void waitUntilEstablished(String name) {
        k8s.apiextensions().v1().customResourceDefinitions()
            .withName(name)
            .waitUntilCondition(crd -> {
                return Optional.ofNullable(crd.getStatus())
                    .map(CustomResourceDefinitionStatus::getConditions)
                    .map(conditions -> conditions.stream()
                            .filter(c -> "Established".equals(c.getType()))
                            .anyMatch(c -> "True".equals(c.getStatus())))
                    .orElse(false);
            }, 10, TimeUnit.SECONDS);
    }
}
