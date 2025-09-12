package com.github.streamshub.console.api;

import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.config.ConfigProvider;
import org.junit.jupiter.api.Test;

import com.github.streamshub.console.kafka.systemtest.TestPlainNoK8sProfile;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestHTTPEndpoint(MetadataResource.class)
@TestProfile(TestPlainNoK8sProfile.class)
class MetadataResourceNoK8sIT {

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

}
