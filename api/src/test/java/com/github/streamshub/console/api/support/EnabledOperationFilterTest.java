package com.github.streamshub.console.api.support;

import jakarta.ws.rs.core.Response.Status;

import org.apache.kafka.common.Uuid;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;

/**
 * This test leverages {@linkplain OperationFilterTestProfile} to selectively
 * disable/enable certain REST operations. Do not assume Kafka operations will
 * work when adding tests to this class.
 *
 * @see OperationFilterTestProfile
 */
@QuarkusTest
@TestProfile(OperationFilterTestProfile.class)
class EnabledOperationFilterTest {

    @Test
    void testDescribeTopicNotAllowed() {
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
