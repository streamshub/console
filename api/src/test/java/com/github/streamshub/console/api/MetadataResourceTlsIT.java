package com.github.streamshub.console.api;

import java.net.ConnectException;
import java.net.URI;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;

import com.github.streamshub.console.kafka.systemtest.TestTlsProfile;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
@TestHTTPEndpoint(MetadataResource.class)
@TestProfile(TestTlsProfile.class)
class MetadataResourceTlsIT {

    @Inject
    @ConfigProperty(name = "quarkus.http.test-ssl-port")
    int testSslPort;

    @Inject
    @ConfigProperty(name = "quarkus.http.test-port")
    int testPort;

    @Test
    void testMetadataAvailableOnHttpsPort() {
        // The lambda receives the RequestSpecification and adds the generated CA trust store
        // so RestAssured accepts the server certificate issued by TlsHelper.
        whenRequesting(req -> req
                .trustStore(TestTlsProfile.TLS.getTrustStore())
                .get(URI.create("https://localhost:%d/api/metadata".formatted(testSslPort))))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("data.id", is("console-meta"))
            .body("data.type", is("metadata"))
            .body("data.attributes.version", is(ConfigProvider.getConfig()
                    .getValue("quarkus.application.version", String.class)));
    }

    @Test
    void testPlainHttpPortNotAvailable() {
        // With insecure-requests=disabled Quarkus does not bind the HTTP port at all.
        assertThrows(ConnectException.class, () ->
            whenRequesting(req -> req.get("http://localhost:%d/api/metadata".formatted(testPort))));
    }
}
