package io.streamshub.console.api;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.streamshub.console.api.model.ConfigEntry;
import io.streamshub.console.api.service.NodeService;

import io.quarkus.test.InjectMock;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static io.streamshub.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(NodesConfigsResourceTest.TestProfile.class)
@TestHTTPEndpoint(NodesResource.class)
class NodesConfigsResourceTest {

    @InjectMock
    NodeService nodeService;

    @BeforeEach
    void setup() {
        when(nodeService.describeConfigs(anyString(), anyBoolean())).thenAnswer(invocation -> {
            boolean includeDocumentation = invocation.getArgument(1);
            return CompletableFuture.completedFuture(Map.of("log.dirs", configEntry(includeDocumentation)));
        });
    }

    @Test
    void testDefaultFieldsDoNotRequestDocumentation() {
        whenRequesting(req -> req.get("{nodeId}/configs", "k1-id", "0"))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes.'log.dirs'.value", is("/tmp"))
            .body("data.attributes.'log.dirs'.source", is("DEFAULT_CONFIG"))
            .body("data.attributes.'log.dirs'.sensitive", is(false))
            .body("data.attributes.'log.dirs'.readOnly", is(false))
            .body("data.attributes.'log.dirs'.type", is("STRING"))
            .body("data.attributes.'log.dirs'", not(hasKey("documentation")));

        verify(nodeService).describeConfigs("0", false);
    }

    @Test
    void testEmptyFieldSelectionUsesDefaultFields() {
        whenRequesting(req -> req
                .param(ConfigEntry.FIELDS_PARAM, "")
                .get("{nodeId}/configs", "k1-id", "0"))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes.'log.dirs'.value", is("/tmp"))
            .body("data.attributes.'log.dirs'.source", is("DEFAULT_CONFIG"))
            .body("data.attributes.'log.dirs'.sensitive", is(false))
            .body("data.attributes.'log.dirs'.readOnly", is(false))
            .body("data.attributes.'log.dirs'.type", is("STRING"))
            .body("data.attributes.'log.dirs'", not(hasKey("documentation")));

        verify(nodeService).describeConfigs("0", false);
    }

    @Test
    void testValueOnlyProjectionDoesNotRequestDocumentation() {
        whenRequesting(req -> req
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.VALUE)
                .get("{nodeId}/configs", "k1-id", "0"))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes.'log.dirs'.value", is("/tmp"))
            .body("data.attributes.'log.dirs'", not(hasKey("source")))
            .body("data.attributes.'log.dirs'", not(hasKey("sensitive")))
            .body("data.attributes.'log.dirs'", not(hasKey("readOnly")))
            .body("data.attributes.'log.dirs'", not(hasKey("type")))
            .body("data.attributes.'log.dirs'", not(hasKey("documentation")));

        verify(nodeService).describeConfigs("0", false);
    }

    @Test
    void testDocumentationOnlyProjection() {
        whenRequesting(req -> req
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.DOCUMENTATION)
                .get("{nodeId}/configs", "k1-id", "0"))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes.'log.dirs'.documentation", is("documentation"))
            .body("data.attributes.'log.dirs'", not(hasKey("value")))
            .body("data.attributes.'log.dirs'", not(hasKey("source")))
            .body("data.attributes.'log.dirs'", not(hasKey("sensitive")))
            .body("data.attributes.'log.dirs'", not(hasKey("readOnly")))
            .body("data.attributes.'log.dirs'", not(hasKey("type")));

        verify(nodeService).describeConfigs("0", true);
    }

    @Test
    void testValueAndDocumentationProjection() {
        whenRequesting(req -> req
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.VALUE + "," + ConfigEntry.Fields.DOCUMENTATION)
                .get("{nodeId}/configs", "k1-id", "0"))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes.'log.dirs'.value", is("/tmp"))
            .body("data.attributes.'log.dirs'.documentation", is("documentation"))
            .body("data.attributes.'log.dirs'", not(hasKey("source")))
            .body("data.attributes.'log.dirs'", not(hasKey("sensitive")))
            .body("data.attributes.'log.dirs'", not(hasKey("readOnly")))
            .body("data.attributes.'log.dirs'", not(hasKey("type")));

        verify(nodeService).describeConfigs("0", true);
    }

    @Test
    void testInvalidFieldSelectionReturnsBadRequest() {
        whenRequesting(req -> req
                .param(ConfigEntry.FIELDS_PARAM, "invalid")
                .get("{nodeId}/configs", "k1-id", "0"))
            .assertThat()
            .statusCode(is(400))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4001"))
            .body("errors.title", contains("Invalid query parameter"))
            .body("errors.source.parameter", contains(ConfigEntry.FIELDS_PARAM));

        verifyNoInteractions(nodeService);
    }

    ConfigEntry configEntry(boolean includeDocumentation) {
        ConfigEntry entry = new ConfigEntry();
        entry.setName("log.dirs");
        entry.setValue("/tmp");
        entry.setSource("DEFAULT_CONFIG");
        entry.setType("STRING");
        entry.setDocumentation(includeDocumentation ? "documentation" : null);
        return entry;
    }

    public static class TestProfile implements QuarkusTestProfile {

        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.kubernetes-client.devservices.enabled", "false",
                    "console.config-path", configPath("/config-nodes-test.yaml"));
        }

        static String configPath(String resourceName) {
            try {
                return Path.of(TestProfile.class.getResource(resourceName).toURI())
                        .toAbsolutePath()
                        .toString();
            } catch (URISyntaxException e) {
                throw new IllegalStateException("Invalid URL for test resource " + resourceName, e);
            }
        }
    }
}
