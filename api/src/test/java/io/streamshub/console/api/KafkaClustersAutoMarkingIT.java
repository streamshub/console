package io.streamshub.console.api;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.streamshub.console.api.support.Holder;
import io.streamshub.console.api.support.KafkaContext;
import io.streamshub.console.kafka.systemtest.KroxyliciousMarkingTestProfile;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.kroxylicious.kubernetes.api.v1alpha1.VirtualKafkaCluster;
import io.kroxylicious.kubernetes.api.v1alpha1.VirtualKafkaClusterBuilder;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static io.streamshub.console.test.TestHelper.whenRequesting;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@TestHTTPEndpoint(KafkaClustersResource.class)
@TestProfile(KroxyliciousMarkingTestProfile.class)
class KafkaClustersAutoMarkingIT {

    static final String EXPLICIT_KIND_CLUSTER_ID = "marking-explicit-kind-id";
    static final String EXPLICIT_KIND_BOOTSTRAP = "localhost:59991";
    static final String NO_KIND_CLUSTER_ID = "marking-no-kind-id";
    static final String NO_KIND_BOOTSTRAP = "localhost:59992";

    @Inject
    KubernetesClient client;

    @Inject
    Map<String, KafkaContext> configuredContexts;

    @Inject
    @Named("VirtualKafkaClusterInformer")
    Holder<SharedIndexInformer<VirtualKafkaCluster>> vkcInformer;

    @BeforeEach
    void setup() {
        client.resources(VirtualKafkaCluster.class).inAnyNamespace().delete();
        await().atMost(Duration.ofSeconds(10)).until(() -> vkcInformer.get().getStore().list().isEmpty());
        await().atMost(Duration.ofSeconds(10)).until(() -> configuredContexts.size() == 2);
    }

    @Test
    void configuredClusterIsMarkedVirtualWhenBootstrapMatchesAnAcceptedVkc() {
        createAcceptedVkc(NO_KIND_BOOTSTRAP);

        whenRequesting(req -> req.queryParam("fields[kafkas]", "name").get())
            .assertThat()
            .statusCode(200)
            .body("data.find { it.id == '" + NO_KIND_CLUSTER_ID + "' }.meta.kind", is("virtualkafkaclusters.kroxylicious.io"));
    }

    @Test
    void configuredClusterIsNotMarkedWhenNoVkcMatches() {
        whenRequesting(req -> req.queryParam("fields[kafkas]", "name").get())
            .assertThat()
            .statusCode(200)
            .body("data.find { it.id == '" + NO_KIND_CLUSTER_ID + "' }.meta", not(hasKey("kind")));
    }

    @Test
    void explicitlyConfiguredKindIsNeverOverridden() {
        createAcceptedVkc(EXPLICIT_KIND_BOOTSTRAP);

        whenRequesting(req -> req.queryParam("fields[kafkas]", "name").get())
            .assertThat()
            .statusCode(200)
            .body("data.find { it.id == '" + EXPLICIT_KIND_CLUSTER_ID + "' }.meta.kind", is("kafkas.kafka.strimzi.io"));
    }

    private void createAcceptedVkc(String bootstrapServer) {
        var vkc = new VirtualKafkaClusterBuilder()
                .withNewMetadata().withName("vkc-" + UUID.randomUUID().toString().substring(0, 8)).withNamespace("default").endMetadata()
                .withNewSpec()
                    .withNewProxyRef().withName("test-proxy").endProxyRef()
                    .withNewTargetKafkaServiceRef().withName("test-service").endTargetKafkaServiceRef()
                    .addNewIngress().withNewIngressRef().withName("ing").endIngressRef().endIngress()
                .endSpec()
                .withNewStatus()
                    .addNewIngress().withName("ing").withBootstrapServer(bootstrapServer).endIngress()
                .endStatus()
                .build();

        var created = client.resource(vkc).create();
        created.setStatus(vkc.getStatus());
        created = client.resource(created).updateStatus();
        var createdWithStatus = created;

        await().atMost(Duration.ofSeconds(10)).until(() ->
                vkcInformer.get().getStore().list().stream()
                        .filter(v -> Cache.metaNamespaceKeyFunc(v).equals(Cache.metaNamespaceKeyFunc(createdWithStatus)))
                        .anyMatch(v -> v.getStatus() != null && v.getStatus().getIngresses() != null));
    }
}
