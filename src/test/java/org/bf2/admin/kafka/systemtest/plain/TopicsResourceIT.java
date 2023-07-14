package org.bf2.admin.kafka.systemtest.plain;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;

import org.bf2.admin.kafka.systemtest.TestPlainProfile;
import org.bf2.admin.kafka.systemtest.utils.TopicUtils2;
import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestProfile(TestPlainProfile.class)
class TopicsResourceIT {

    @Inject
    Config config;

    TopicUtils2 topicUtils;

    @BeforeEach
    void setup() {
        topicUtils = new TopicUtils2(config, null);
        topicUtils.deleteAllTopics();
    }

    @Test
    void testTopicListAfterCreation() {
        List<String> topicNames = IntStream.range(0, 2)
                .mapToObj(i -> UUID.randomUUID().toString())
                .collect(Collectors.toList());

        topicUtils.createTopics(topicNames, 1, Status.CREATED);

        given()
            .log().ifValidationFails()
        .when()
            .get(TopicUtils2.TOPIC_COLLECTION_PATH)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.OK.getStatusCode())
        .assertThat()
            .body("size()", equalTo(topicNames.size()))
            .body("name", containsInAnyOrder(topicNames.toArray(String[]::new)));
    }
}
