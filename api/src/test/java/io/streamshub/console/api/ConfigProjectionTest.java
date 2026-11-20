package io.streamshub.console.api;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import jakarta.json.Json;

import org.apache.kafka.clients.admin.GroupListing;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.GroupType;
import org.apache.kafka.common.Uuid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.streamshub.console.api.model.ConfigEntry;
import io.streamshub.console.api.model.Either;
import io.streamshub.console.api.model.Group;
import io.streamshub.console.api.model.NewTopic;
import io.streamshub.console.api.model.Topic;
import io.streamshub.console.api.service.GroupService;
import io.streamshub.console.api.service.TopicDescribeService;
import io.streamshub.console.api.service.TopicService;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

import static io.streamshub.console.test.TestHelper.whenRequesting;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@QuarkusTest
@TestProfile(ConfigProjectionTest.TestProfile.class)
class ConfigProjectionTest {

    @InjectMock
    TopicService topicService;

    @InjectMock
    GroupService groupService;

    @InjectMock
    TopicDescribeService topicDescribeService;

    @BeforeEach
    void setup() {
        Topic topic = topic();
        Group group = group();

        lenient().when(topicDescribeService.topicNameForId(anyString()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of("test-topic")));
        lenient().when(topicService.listTopics(anyList(), anyString(), any(), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture(List.of(topic)));
        lenient().when(topicService.describeTopic(anyString(), anyList(), anyString(), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture(topic));
        lenient().when(topicService.createTopic(any(NewTopic.class), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture(newTopic()));
        lenient().when(groupService.listGroups(anyList(), any(), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture(List.of(group)));
        lenient().when(groupService.describeGroup(anyString(), anyList(), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture(group));
        lenient().when(groupService.patchGroup(anyString(), any(), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture(Optional.of(group)));
    }

    @Test
    void testListTopicsUsesDefaultConfigFields() {
        whenRequesting(req -> req
                .param(Topic.FIELDS_PARAM, Topic.Fields.NAME + "," + Topic.Fields.CONFIGS)
                .get("/api/kafkas/{clusterId}/topics", "k1-id"))
            .assertThat()
            .statusCode(is(200))
            .body("data[0].attributes.configs.'cleanup.policy'", allOf(
                    hasKey("value"),
                    hasKey("source"),
                    hasKey("sensitive"),
                    hasKey("readOnly"),
                    hasKey("type"),
                    not(hasKey("documentation"))));
    }

    @Test
    void testDescribeTopicUsesDefaultConfigFields() {
        whenRequesting(req -> req
                .param(Topic.FIELDS_PARAM, Topic.Fields.NAME + "," + Topic.Fields.CONFIGS)
                .get("/api/kafkas/{clusterId}/topics/{topicId}", "k1-id", Uuid.randomUuid().toString()))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes.configs.'cleanup.policy'", allOf(
                    hasKey("value"),
                    hasKey("source"),
                    hasKey("sensitive"),
                    hasKey("readOnly"),
                    hasKey("type"),
                    not(hasKey("documentation"))));
    }

    @Test
    void testListTopicsEmptyConfigFieldsUseDefaults() {
        whenRequesting(req -> req
                .param(Topic.FIELDS_PARAM, Topic.Fields.NAME + "," + Topic.Fields.CONFIGS)
                .param(ConfigEntry.FIELDS_PARAM, "")
                .get("/api/kafkas/{clusterId}/topics", "k1-id"))
            .assertThat()
            .statusCode(is(200))
            .body("data[0].attributes.configs.'cleanup.policy'", allOf(
                    hasKey("value"),
                    hasKey("source"),
                    hasKey("sensitive"),
                    hasKey("readOnly"),
                    hasKey("type"),
                    not(hasKey("documentation"))));

        verify(topicService).listTopics(anyList(), anyString(), any(), eq(false));
    }

    @Test
    void testDescribeTopicEmptyConfigFieldsUseDefaults() {
        whenRequesting(req -> req
                .param(Topic.FIELDS_PARAM, Topic.Fields.NAME + "," + Topic.Fields.CONFIGS)
                .param(ConfigEntry.FIELDS_PARAM, "")
                .get("/api/kafkas/{clusterId}/topics/{topicId}", "k1-id", Uuid.randomUuid().toString()))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes.configs.'cleanup.policy'", allOf(
                    hasKey("value"),
                    hasKey("source"),
                    hasKey("sensitive"),
                    hasKey("readOnly"),
                    hasKey("type"),
                    not(hasKey("documentation"))));

        verify(topicService).describeTopic(anyString(), anyList(), anyString(), eq(false));
    }

    @Test
    void testListTopicsValueOnlyProjectionDoesNotRequestDocumentation() {
        whenRequesting(req -> req
                .param(Topic.FIELDS_PARAM, Topic.Fields.NAME + "," + Topic.Fields.CONFIGS)
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.VALUE)
                .get("/api/kafkas/{clusterId}/topics", "k1-id"))
            .assertThat()
            .statusCode(is(200))
            .body("data[0].attributes.configs.'cleanup.policy'.value", is("delete"))
            .body("data[0].attributes.configs.'cleanup.policy'", allOf(
                    not(hasKey("source")),
                    not(hasKey("sensitive")),
                    not(hasKey("readOnly")),
                    not(hasKey("type")),
                    not(hasKey("documentation"))));

        verify(topicService).listTopics(anyList(), anyString(), any(), eq(false));
    }

    @Test
    void testListTopicsDocumentationOnlyProjectionRequestsDocumentation() {
        whenRequesting(req -> req
                .param(Topic.FIELDS_PARAM, Topic.Fields.NAME + "," + Topic.Fields.CONFIGS)
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.DOCUMENTATION)
                .get("/api/kafkas/{clusterId}/topics", "k1-id"))
            .assertThat()
            .statusCode(is(200))
            .body("data[0].attributes.configs.'cleanup.policy'.documentation", is("docs"))
            .body("data[0].attributes.configs.'cleanup.policy'", allOf(
                    not(hasKey("value")),
                    not(hasKey("source")),
                    not(hasKey("sensitive")),
                    not(hasKey("readOnly")),
                    not(hasKey("type"))));

        verify(topicService).listTopics(anyList(), anyString(), any(), eq(true));
    }

    @Test
    void testListTopicsValueAndDocumentationProjection() {
        whenRequesting(req -> req
                .param(Topic.FIELDS_PARAM, Topic.Fields.NAME + "," + Topic.Fields.CONFIGS)
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.VALUE + "," + ConfigEntry.Fields.DOCUMENTATION)
                .get("/api/kafkas/{clusterId}/topics", "k1-id"))
            .assertThat()
            .statusCode(is(200))
            .body("data[0].attributes.configs.'cleanup.policy'.value", is("delete"))
            .body("data[0].attributes.configs.'cleanup.policy'.documentation", is("docs"))
            .body("data[0].attributes.configs.'cleanup.policy'", allOf(
                    not(hasKey("source")),
                    not(hasKey("sensitive")),
                    not(hasKey("readOnly")),
                    not(hasKey("type"))));

        verify(topicService).listTopics(anyList(), anyString(), any(), eq(true));
    }

    @Test
    void testDescribeTopicValueOnlyProjectionDoesNotRequestDocumentation() {
        whenRequesting(req -> req
                .param(Topic.FIELDS_PARAM, Topic.Fields.NAME + "," + Topic.Fields.CONFIGS)
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.VALUE)
                .get("/api/kafkas/{clusterId}/topics/{topicId}", "k1-id", Uuid.randomUuid().toString()))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes.configs.'cleanup.policy'.value", is("delete"))
            .body("data.attributes.configs.'cleanup.policy'", allOf(
                    not(hasKey("source")),
                    not(hasKey("sensitive")),
                    not(hasKey("readOnly")),
                    not(hasKey("type")),
                    not(hasKey("documentation"))));

        verify(topicService).describeTopic(anyString(), anyList(), anyString(), eq(false));
    }

    @Test
    void testDescribeTopicDocumentationOnlyProjectionRequestsDocumentation() {
        whenRequesting(req -> req
                .param(Topic.FIELDS_PARAM, Topic.Fields.NAME + "," + Topic.Fields.CONFIGS)
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.DOCUMENTATION)
                .get("/api/kafkas/{clusterId}/topics/{topicId}", "k1-id", Uuid.randomUuid().toString()))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes.configs.'cleanup.policy'.documentation", is("docs"))
            .body("data.attributes.configs.'cleanup.policy'", allOf(
                    not(hasKey("value")),
                    not(hasKey("source")),
                    not(hasKey("sensitive")),
                    not(hasKey("readOnly")),
                    not(hasKey("type"))));

        verify(topicService).describeTopic(anyString(), anyList(), anyString(), eq(true));
    }

    @Test
    void testListTopicsDocumentationNotRequestedWhenConfigsExcluded() {
        whenRequesting(req -> req
                .param(Topic.FIELDS_PARAM, Topic.Fields.NAME)
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.DOCUMENTATION)
                .get("/api/kafkas/{clusterId}/topics", "k1-id"))
            .assertThat()
            .statusCode(is(200))
            .body("data[0].attributes", not(hasKey("configs")));

        verify(topicService).listTopics(anyList(), anyString(), any(), eq(false));
    }

    @Test
    void testDescribeTopicDocumentationNotRequestedWhenConfigsExcluded() {
        whenRequesting(req -> req
                .param(Topic.FIELDS_PARAM, Topic.Fields.NAME)
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.DOCUMENTATION)
                .get("/api/kafkas/{clusterId}/topics/{topicId}", "k1-id", Uuid.randomUuid().toString()))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes", not(hasKey("configs")));

        verify(topicService).describeTopic(anyString(), anyList(), anyString(), eq(false));
    }

    @Test
    void testListTopicsInvalidConfigFieldReturnsBadRequest() {
        whenRequesting(req -> req
                .param(ConfigEntry.FIELDS_PARAM, "invalid")
                .get("/api/kafkas/{clusterId}/topics", "k1-id"))
            .assertThat()
            .statusCode(is(400))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4001"))
            .body("errors.title", contains("Invalid query parameter"))
            .body("errors.source.parameter", contains(ConfigEntry.FIELDS_PARAM));
    }

    @Test
    void testDescribeTopicInvalidConfigFieldReturnsBadRequest() {
        whenRequesting(req -> req
                .param(ConfigEntry.FIELDS_PARAM, "invalid")
                .get("/api/kafkas/{clusterId}/topics/{topicId}", "k1-id", Uuid.randomUuid().toString()))
            .assertThat()
            .statusCode(is(400))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4001"))
            .body("errors.title", contains("Invalid query parameter"))
            .body("errors.source.parameter", contains(ConfigEntry.FIELDS_PARAM));
    }

    @Test
    void testListGroupsUsesDefaultConfigFields() {
        whenRequesting(req -> req
                .param(Group.FIELDS_PARAM, Group.Fields.GROUP_ID + "," + Group.Fields.CONFIGS)
                .get("/api/kafkas/{clusterId}/groups", "k1-id"))
            .assertThat()
            .statusCode(is(200))
            .body("data[0].attributes.configs.'session.timeout.ms'", allOf(
                    hasKey("value"),
                    hasKey("source"),
                    hasKey("sensitive"),
                    hasKey("readOnly"),
                    hasKey("type"),
                    not(hasKey("documentation"))));

        verify(groupService).listGroups(anyList(), any(), eq(false));
    }

    @Test
    void testDescribeGroupUsesDefaultConfigFields() {
        whenRequesting(req -> req
                .param(Group.FIELDS_PARAM, Group.Fields.GROUP_ID + "," + Group.Fields.CONFIGS)
                .get("/api/kafkas/{clusterId}/groups/{groupId}", "k1-id", Group.encodeGroupId("test-group")))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes.configs.'session.timeout.ms'", allOf(
                    hasKey("value"),
                    hasKey("source"),
                    hasKey("sensitive"),
                    hasKey("readOnly"),
                    hasKey("type"),
                    not(hasKey("documentation"))));

        verify(groupService).describeGroup(anyString(), anyList(), eq(false));
    }

    @Test
    void testListGroupsEmptyConfigFieldsUseDefaults() {
        whenRequesting(req -> req
                .param(Group.FIELDS_PARAM, Group.Fields.GROUP_ID + "," + Group.Fields.CONFIGS)
                .param(ConfigEntry.FIELDS_PARAM, "")
                .get("/api/kafkas/{clusterId}/groups", "k1-id"))
            .assertThat()
            .statusCode(is(200))
            .body("data[0].attributes.configs.'session.timeout.ms'", allOf(
                    hasKey("value"),
                    hasKey("source"),
                    hasKey("sensitive"),
                    hasKey("readOnly"),
                    hasKey("type"),
                    not(hasKey("documentation"))));

        verify(groupService).listGroups(anyList(), any(), eq(false));
    }

    @Test
    void testDescribeGroupEmptyConfigFieldsUseDefaults() {
        whenRequesting(req -> req
                .param(Group.FIELDS_PARAM, Group.Fields.GROUP_ID + "," + Group.Fields.CONFIGS)
                .param(ConfigEntry.FIELDS_PARAM, "")
                .get("/api/kafkas/{clusterId}/groups/{groupId}", "k1-id", Group.encodeGroupId("test-group")))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes.configs.'session.timeout.ms'", allOf(
                    hasKey("value"),
                    hasKey("source"),
                    hasKey("sensitive"),
                    hasKey("readOnly"),
                    hasKey("type"),
                    not(hasKey("documentation"))));

        verify(groupService).describeGroup(anyString(), anyList(), eq(false));
    }

    @Test
    void testListGroupsValueOnlyProjectionDoesNotRequestDocumentation() {
        whenRequesting(req -> req
                .param(Group.FIELDS_PARAM, Group.Fields.GROUP_ID + "," + Group.Fields.CONFIGS)
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.VALUE)
                .get("/api/kafkas/{clusterId}/groups", "k1-id"))
            .assertThat()
            .statusCode(is(200))
            .body("data[0].attributes.configs.'session.timeout.ms'.value", is("45000"))
            .body("data[0].attributes.configs.'session.timeout.ms'", allOf(
                    not(hasKey("source")),
                    not(hasKey("sensitive")),
                    not(hasKey("readOnly")),
                    not(hasKey("type")),
                    not(hasKey("documentation"))));

        verify(groupService).listGroups(anyList(), any(), eq(false));
    }

    @Test
    void testListGroupsDocumentationOnlyProjectionRequestsDocumentation() {
        whenRequesting(req -> req
                .param(Group.FIELDS_PARAM, Group.Fields.GROUP_ID + "," + Group.Fields.CONFIGS)
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.DOCUMENTATION)
                .get("/api/kafkas/{clusterId}/groups", "k1-id"))
            .assertThat()
            .statusCode(is(200))
            .body("data[0].attributes.configs.'session.timeout.ms'.documentation", is("docs"))
            .body("data[0].attributes.configs.'session.timeout.ms'", allOf(
                    not(hasKey("value")),
                    not(hasKey("source")),
                    not(hasKey("sensitive")),
                    not(hasKey("readOnly")),
                    not(hasKey("type"))));

        verify(groupService).listGroups(anyList(), any(), eq(true));
    }

    @Test
    void testListGroupsValueAndDocumentationProjection() {
        whenRequesting(req -> req
                .param(Group.FIELDS_PARAM, Group.Fields.GROUP_ID + "," + Group.Fields.CONFIGS)
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.VALUE + "," + ConfigEntry.Fields.DOCUMENTATION)
                .get("/api/kafkas/{clusterId}/groups", "k1-id"))
            .assertThat()
            .statusCode(is(200))
            .body("data[0].attributes.configs.'session.timeout.ms'.value", is("45000"))
            .body("data[0].attributes.configs.'session.timeout.ms'.documentation", is("docs"))
            .body("data[0].attributes.configs.'session.timeout.ms'", allOf(
                    not(hasKey("source")),
                    not(hasKey("sensitive")),
                    not(hasKey("readOnly")),
                    not(hasKey("type"))));

        verify(groupService).listGroups(anyList(), any(), eq(true));
    }

    @Test
    void testDescribeGroupValueOnlyProjectionDoesNotRequestDocumentation() {
        whenRequesting(req -> req
                .param(Group.FIELDS_PARAM, Group.Fields.GROUP_ID + "," + Group.Fields.CONFIGS)
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.VALUE)
                .get("/api/kafkas/{clusterId}/groups/{groupId}", "k1-id", Group.encodeGroupId("test-group")))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes.configs.'session.timeout.ms'.value", is("45000"))
            .body("data.attributes.configs.'session.timeout.ms'", allOf(
                    not(hasKey("source")),
                    not(hasKey("sensitive")),
                    not(hasKey("readOnly")),
                    not(hasKey("type")),
                    not(hasKey("documentation"))));

        verify(groupService).describeGroup(anyString(), anyList(), eq(false));
    }

    @Test
    void testDescribeGroupDocumentationOnlyProjectionRequestsDocumentation() {
        whenRequesting(req -> req
                .param(Group.FIELDS_PARAM, Group.Fields.GROUP_ID + "," + Group.Fields.CONFIGS)
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.DOCUMENTATION)
                .get("/api/kafkas/{clusterId}/groups/{groupId}", "k1-id", Group.encodeGroupId("test-group")))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes.configs.'session.timeout.ms'.documentation", is("docs"))
            .body("data.attributes.configs.'session.timeout.ms'", allOf(
                    not(hasKey("value")),
                    not(hasKey("source")),
                    not(hasKey("sensitive")),
                    not(hasKey("readOnly")),
                    not(hasKey("type"))));

        verify(groupService).describeGroup(anyString(), anyList(), eq(true));
    }

    @Test
    void testDescribeGroupValueAndDocumentationProjection() {
        whenRequesting(req -> req
                .param(Group.FIELDS_PARAM, Group.Fields.GROUP_ID + "," + Group.Fields.CONFIGS)
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.VALUE + "," + ConfigEntry.Fields.DOCUMENTATION)
                .get("/api/kafkas/{clusterId}/groups/{groupId}", "k1-id", Group.encodeGroupId("test-group")))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes.configs.'session.timeout.ms'.value", is("45000"))
            .body("data.attributes.configs.'session.timeout.ms'.documentation", is("docs"))
            .body("data.attributes.configs.'session.timeout.ms'", allOf(
                    not(hasKey("source")),
                    not(hasKey("sensitive")),
                    not(hasKey("readOnly")),
                    not(hasKey("type"))));

        verify(groupService).describeGroup(anyString(), anyList(), eq(true));
    }

    @Test
    void testListGroupsDocumentationNotRequestedWhenConfigsExcluded() {
        whenRequesting(req -> req
                .param(Group.FIELDS_PARAM, Group.Fields.GROUP_ID)
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.DOCUMENTATION)
                .get("/api/kafkas/{clusterId}/groups", "k1-id"))
            .assertThat()
            .statusCode(is(200))
            .body("data[0].attributes", not(hasKey("configs")));

        verify(groupService).listGroups(anyList(), any(), eq(false));
    }

    @Test
    void testDescribeGroupDocumentationNotRequestedWhenConfigsExcluded() {
        whenRequesting(req -> req
                .param(Group.FIELDS_PARAM, Group.Fields.GROUP_ID)
                .param(ConfigEntry.FIELDS_PARAM, ConfigEntry.Fields.DOCUMENTATION)
                .get("/api/kafkas/{clusterId}/groups/{groupId}", "k1-id", Group.encodeGroupId("test-group")))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes", not(hasKey("configs")));

        verify(groupService).describeGroup(anyString(), anyList(), eq(false));
    }

    @Test
    void testListGroupsInvalidConfigFieldReturnsBadRequest() {
        whenRequesting(req -> req
                .param(ConfigEntry.FIELDS_PARAM, "invalid")
                .get("/api/kafkas/{clusterId}/groups", "k1-id"))
            .assertThat()
            .statusCode(is(400))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4001"))
            .body("errors.title", contains("Invalid query parameter"))
            .body("errors.source.parameter", contains(ConfigEntry.FIELDS_PARAM));
    }

    @Test
    void testDescribeGroupInvalidConfigFieldReturnsBadRequest() {
        whenRequesting(req -> req
                .param(ConfigEntry.FIELDS_PARAM, "invalid")
                .get("/api/kafkas/{clusterId}/groups/{groupId}", "k1-id", Group.encodeGroupId("test-group")))
            .assertThat()
            .statusCode(is(400))
            .body("errors.size()", is(1))
            .body("errors.status", contains("400"))
            .body("errors.code", contains("4001"))
            .body("errors.title", contains("Invalid query parameter"))
            .body("errors.source.parameter", contains(ConfigEntry.FIELDS_PARAM));
    }

    @Test
    void testPatchGroupDryRunResponseOmitsConfigs() {
        String groupId = Group.encodeGroupId("test-group");

        whenRequesting(req -> req
                .contentType("application/json")
                .body(Json.createObjectBuilder()
                        .add("meta", Json.createObjectBuilder().add("dryRun", true))
                        .add("data", Json.createObjectBuilder()
                                .add("id", groupId)
                                .add("type", Group.API_TYPE)
                                .add("attributes", Json.createObjectBuilder()))
                        .build()
                        .toString())
                .patch("/api/kafkas/{clusterId}/groups/{groupId}", "k1-id", groupId))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes.state", is("STABLE"))
            .body("data.attributes", not(hasKey("configs")));

        verify(groupService).patchGroup(anyString(), any(), eq(true));
    }

    @Test
    void testCreateTopicUsesDefaultConfigFields() {
        whenRequesting(req -> req
                .contentType("application/json")
                .body(newTopicRequest(false))
                .post("/api/kafkas/{clusterId}/topics", "k1-id"))
            .assertThat()
            .statusCode(is(201))
            .body("data.attributes.configs.'retention.ms'", allOf(
                    hasKey("value"),
                    hasKey("source"),
                    hasKey("sensitive"),
                    hasKey("readOnly"),
                    hasKey("type"),
                    not(hasKey("documentation"))));
    }

    @Test
    void testValidateTopicUsesDefaultConfigFields() {
        whenRequesting(req -> req
                .contentType("application/json")
                .body(newTopicRequest(true))
                .post("/api/kafkas/{clusterId}/topics", "k1-id"))
            .assertThat()
            .statusCode(is(200))
            .body("data.attributes.configs.'retention.ms'", allOf(
                    hasKey("value"),
                    hasKey("source"),
                    hasKey("sensitive"),
                    hasKey("readOnly"),
                    hasKey("type"),
                    not(hasKey("documentation"))));
    }

    String newTopicRequest(boolean validateOnly) {
        var builder = Json.createObjectBuilder()
                .add("data", Json.createObjectBuilder()
                        .add("type", Topic.API_TYPE)
                        .add("attributes", Json.createObjectBuilder()
                                .add("name", "new-topic")
                                .add("numPartitions", 1)
                                .add("replicationFactor", 1)));

        if (validateOnly) {
            builder.add("meta", Json.createObjectBuilder().add("validateOnly", true));
        }

        return builder.build().toString();
    }

    Topic topic() {
        Topic topic = new Topic("test-topic", false, Uuid.randomUuid().toString());
        topic.addConfigs(Either.of(Map.of("cleanup.policy", configEntry("delete"))));
        return topic;
    }

    Group group() {
        GroupListing listing = mock(GroupListing.class);
        when(listing.groupId()).thenReturn("test-group");
        when(listing.isSimpleConsumerGroup()).thenReturn(false);
        when(listing.groupState()).thenReturn(Optional.of(GroupState.STABLE));
        when(listing.type()).thenReturn(Optional.of(GroupType.CLASSIC));
        when(listing.protocol()).thenReturn("consumer");

        Group group = Group.fromKafkaModel(listing);
        group.addConfigs(Either.of(Map.of("session.timeout.ms", configEntry("45000"))));
        return group;
    }

    NewTopic newTopic() {
        return new NewTopic(
                "new-topic",
                Uuid.randomUuid().toString(),
                1,
                (short) 1,
                null,
                Map.of("retention.ms", configEntry("604800000")));
    }

    ConfigEntry configEntry(String value) {
        ConfigEntry entry = new ConfigEntry();
        entry.setValue(value);
        entry.setSource("DEFAULT_CONFIG");
        entry.setType("STRING");
        entry.setDocumentation("docs");
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
