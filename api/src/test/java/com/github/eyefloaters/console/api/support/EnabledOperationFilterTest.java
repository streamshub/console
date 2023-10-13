package com.github.eyefloaters.console.api.support;

import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.common.Uuid;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.eyefloaters.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

@QuarkusTest
@TestProfile(OperationFilterTestProfile.class)
class EnabledOperationFilterTest {

    @Test
    void testDescribeTopicNotAllowed() throws Exception {
        whenRequesting(req -> req.get("/api/kafkas/{clusterId}/topics/{topicId}",
                Uuid.randomUuid().toString(),
                Uuid.randomUuid().toString()))
            .assertThat()
            .statusCode(is(Status.METHOD_NOT_ALLOWED.getStatusCode()))
            .body("errors.size()", is(1))
            .body("errors.status", contains("405"))
            .body("errors.code", contains("4051"));
    }

}
