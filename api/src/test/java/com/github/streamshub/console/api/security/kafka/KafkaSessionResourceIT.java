package com.github.streamshub.console.api.security.kafka;

import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;

import org.apache.kafka.clients.CommonClientConfigs;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.test.TestHelper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthentication;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationScramSha512Builder;

import static com.github.streamshub.console.test.TestHelper.authenticate;
import static com.github.streamshub.console.test.TestHelper.mockAdminClient;
import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@TestHTTPEndpoint(KafkaSessionResource.class)
@TestProfile(TestPlainProfile.class)
class KafkaSessionResourceIT {

    private static final String AUTHN_SCRAM_TAG = "AUTHN_SCRAM";

    @Inject
    Config config;

    @Inject
    KubernetesClient client;

    @TestHTTPResource
    String testUri;

    @Inject
    Map<String, KafkaContext> configuredContexts;

    @Inject
    ConsoleConfig consoleConfig;

    TestHelper utils;
    String clusterId;
    URI bootstrapServers;
    Map<String, String> sessionCookies;

    @BeforeEach
    void setup(TestInfo testInfo) {
        bootstrapServers = URI.create(config.getValue(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, String.class));
        utils = new TestHelper(URI.create("/"), config);
        utils.resetSecurity(consoleConfig, false);

        client.resources(Kafka.class).inAnyNamespace().delete();
        clusterId = UUID.randomUUID().toString();

        KafkaListenerAuthentication authn = null;

        if (testInfo.getTags().contains(AUTHN_SCRAM_TAG)) {
            authn = new KafkaListenerAuthenticationScramSha512Builder().build();
        }

        /*
         * Create a Kafka CR that proxies to kafka1.
         * test-kafka3 is predefined in KafkaUnsecuredResourceManager
         */
        Kafka kafka = new KafkaBuilder(utils.buildKafkaResource("test-kafka3", clusterId, bootstrapServers, authn))
                .editSpec()
                    .editKafka()
                    .editMatchingListener(l -> "listener0".equals(l.getName()))
                        .withTls(false)
                    .endListener()
                .endKafka()
                .endSpec()
                .build();

        utils.apply(client, kafka);

        // Wait for the added cluster to be configured in the context map
        await().atMost(10, TimeUnit.SECONDS)
            .until(() -> configuredContexts.values()
                    .stream()
                    .map(KafkaContext::clusterConfig)
                    .map(KafkaClusterConfig::clusterKey)
                    .anyMatch(Cache.metaNamespaceKeyFunc(kafka)::equals));

        if (testInfo.getTags().contains(AUTHN_SCRAM_TAG)) {
            mockAdminClient();
            this.sessionCookies = authenticate("", clusterId, "my-user", "my-password");
        } else {
            this.sessionCookies = Collections.emptyMap();
        }
    }

    @Test
    @Tag(AUTHN_SCRAM_TAG)
    void testGetCurrentKafkaUserSCRAM() {
        whenRequesting(req -> req
                .cookies(sessionCookies)
                .get("user", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("anonymous", is(false))
            .body("username", is("my-user"))
            .body(".", not(hasKey("fullName")));
    }

    @Test
    @Tag(AUTHN_SCRAM_TAG)
    void testLogoutSCRAM() {
        var expectedLocation = UriBuilder.fromUri(testUri)
                .replacePath("/some/path")
                .build()
                .toString();

        whenRequesting(req -> req
                .queryParam("redirect_uri", "/some/path")
                .cookies(sessionCookies)
                .redirects().follow(false)
                .get("logout", clusterId))
            .assertThat()
            .statusCode(is(Status.SEE_OTHER.getStatusCode()))
            .header("Location", is(expectedLocation));
    }

    @Test
    void testGetCurrentUserAnonymous() {
        whenRequesting(req -> req
                .get("user", clusterId))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("anonymous", is(true))
            .body("username", is("ANONYMOUS"))
            .body("fullName", is("Anonymous"));
    }
}
