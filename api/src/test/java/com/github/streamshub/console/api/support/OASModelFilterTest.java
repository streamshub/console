package com.github.streamshub.console.api.support;

import jakarta.ws.rs.core.Response.Status;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

@QuarkusTest
@TestProfile(OperationFilterTestProfile.class)
class OASModelFilterTest {

    @Test
    void testOpenApiOperationsMatchConfiguration() {
        whenRequesting(req -> req.get("/openapi.json"))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("paths.'/api/kafkas/{clusterId}/topics'", hasKey("post"))
            .body("paths.'/api/kafkas/{clusterId}/topics/{topicId}'", allOf(not(hasKey("get")), not(hasKey("delete"))));
    }

}
