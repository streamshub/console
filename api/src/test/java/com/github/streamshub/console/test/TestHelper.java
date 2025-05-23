package com.github.streamshub.console.test;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.Annotations;
import com.github.streamshub.console.api.support.TrustStoreSupport;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.TrustStoreConfig;
import com.github.streamshub.console.config.security.GlobalSecurityConfigBuilder;
import com.github.streamshub.console.config.security.SecurityConfig;
import com.github.streamshub.console.kafka.systemtest.utils.ClientsConfig;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthentication;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.fail;

public class TestHelper {

    static final Logger log = Logger.getLogger(TestHelper.class);
    final Config config;
    final Properties adminConfig;

    public TestHelper(URI bootstrapServers, Config config) {
        this.config = config;
        this.adminConfig = ClientsConfig.getAdminConfig(config);

        adminConfig.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers.toString());
    }

    public Kafka buildKafkaResource(String name, URI bootstrapServers) {
        return buildKafkaResource(name, UUID.randomUUID().toString(), bootstrapServers);
    }

    public Kafka buildKafkaResource(String name, String id, URI bootstrapServers) {
        return buildKafkaResource(name, id, bootstrapServers, null);
    }

    public Kafka buildKafkaResource(String name, String id, URI bootstrapServers, KafkaListenerAuthentication auth) {
        return new KafkaBuilder()
            .withNewMetadata()
                .withNamespace("default")
                .withName(name)
                .addToAnnotations("x-custom-annotation", "value-1")
            .endMetadata()
            .withNewSpec()
                .withNewKafka()
                    .withReplicas(1)
                    .addNewListener()
                        .withName("listener0")
                        .withType(KafkaListenerType.NODEPORT)
                        .withAuth(auth)
                        .withNewConfiguration()
                            .withNewBootstrap()
                                .addToAnnotations(Annotations.CONSOLE_LISTENER.value(), "true")
                            .endBootstrap()
                        .endConfiguration()
                    .endListener()
                    .addNewListener()
                        .withName("listener1")
                        .withType(KafkaListenerType.NODEPORT)
                        .withAuth(auth)
                        .withNewConfiguration()
                            .withNewBootstrap()
                                .addToAnnotations(Annotations.CONSOLE_LISTENER.value(), "false")
                            .endBootstrap()
                        .endConfiguration()
                    .endListener()
                    .addNewListener()
                        .withName("listener2")
                        .withType(KafkaListenerType.INTERNAL)
                    .endListener()
                .endKafka()
            .endSpec()
            .withNewStatus()
                .withClusterId(id)
                .addNewListener()
                    .withName("listener0")
                    .addNewAddress()
                        .withHost(bootstrapServers.getHost())
                        .withPort(bootstrapServers.getPort())
                    .endAddress()
                .endListener()
                .addNewListener()
                    .withName("listener1")
                    .addNewAddress()
                        .withHost(bootstrapServers.getHost())
                        .withPort(bootstrapServers.getPort())
                    .endAddress()
                .endListener()
                .addNewListener()
                    .withName("listener2")
                    .addNewAddress()
                        .withHost("local.cluster")
                        .withPort(9091)
                    .endAddress()
                .endListener()
            .endStatus()
            .build();
    }

    public <T extends HasMetadata> T apply(KubernetesClient client, T resource) {
        var resourceClient = client.resource(resource);
        resource = resourceClient.serverSideApply();

        if (resource instanceof CustomResource<?, ?> || hasStatus(resource)) {
            resource = resourceClient.patchStatus();
        }

        return resource;
    }

    private static boolean hasStatus(Object resource) {
        try {
            Method getStatus = resource.getClass().getMethod("getStatus");
            return Objects.nonNull(getStatus.invoke(resource));
        } catch (Exception e) {
            return false;
        }
    }

    public String getClusterId() {
        try (Admin admin = Admin.create(adminConfig)) {
            return admin.describeCluster().clusterId().get(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Process interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail(e);
        }

        return null;
    }

    public Optional<String> getCursor(JsonObject dataList, Integer idx) {
        return Optional.ofNullable(idx)
            .map("/data/%d/meta/page/cursor"::formatted)
            .map(pointer -> getValue(dataList, JsonString.class, pointer))
            .map(JsonString::getString);
    }

    public <J extends JsonValue> J getValue(JsonStructure source, Class<J> type, String pointer) {
        return type.cast(source.getValue(pointer));
    }

    public static ValidatableResponse whenRequesting(Function<RequestSpecification, Response> requestFn) {
        RequestSpecification requestSpec = given().log().ifValidationFails();
        Response response = requestFn.apply(requestSpec);
        return response.then().log().ifValidationFails();
    }

    public void resetSecurity(ConsoleConfig consoleConfig, boolean oidc) {
        consoleConfig.clearSecurity();

        if (oidc) {
            consoleConfig.setSecurity(oidcSecurity().build());
            CDI.current().select(TrustStoreSupport.class).get().registerTrustStores(consoleConfig);
        }
    }

    public void updateSecurity(SecurityConfig config, SecurityConfig updates) {
        config.setAudit(updates.getAudit());
        config.setRoles(updates.getRoles());
        config.setSubjects(updates.getSubjects());
    }

    private GlobalSecurityConfigBuilder oidcSecurity() {
        return new GlobalSecurityConfigBuilder()
            .withNewOidc()
                .withClientId("console-client")
                .withClientSecret("console-client-secret")
                .withAuthServerUrl(config.getValue("console.test.oidc-url", String.class))
                .withIssuer(config.getValue("console.test.oidc-issuer", String.class))
                .withNewTrustStore()
                    .withType(TrustStoreConfig.Type.valueOf(config.getValue("console.test.oidc-trust-store.type", String.class)))
                    .withNewPassword()
                        .withValue(config.getValue("console.test.oidc-trust-store.password", String.class))
                    .endPassword()
                    .withNewContent()
                        .withValueFrom(config.getValue("console.test.oidc-trust-store.path", String.class))
                    .endContent()
                .endTrustStore()
            .endOidc();
    }
}
