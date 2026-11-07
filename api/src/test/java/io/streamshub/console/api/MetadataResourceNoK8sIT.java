package io.streamshub.console.api;

import java.net.ConnectException;
import java.net.URI;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import io.streamshub.console.kafka.systemtest.TestPlainNoK8sProfile;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static io.streamshub.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
@TestHTTPEndpoint(MetadataResource.class)
@TestProfile(TestPlainNoK8sProfile.class)
class MetadataResourceNoK8sIT {

    @Inject
    @ConfigProperty(name = "quarkus.http.test-ssl-port")
    int testSslPort;

    @Test
    void testGetMetadataDefaultUnknown() {
        whenRequesting(req -> req.get())
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.id", is("console-meta"))
            .body("data.type", is("metadata"))
            .body("data.attributes.version", is(ConfigProvider.getConfig()
                    .getValue("quarkus.application.version", String.class)))
            .body("data.attributes.platform", is("Unknown"));
    }

    @Test
    void testHttpsPortNotAvailableWithoutTls() {
        // When no TLS is configured, Quarkus does not bind the SSL port at all.
        assertThrows(ConnectException.class, () ->
            whenRequesting(req -> req
                    .relaxedHTTPSValidation()
                    .get(URI.create("https://localhost:%d/api/metadata".formatted(testSslPort)))));
    }
}
