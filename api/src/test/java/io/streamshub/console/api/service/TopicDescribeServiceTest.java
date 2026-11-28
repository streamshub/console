package io.streamshub.console.api.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.common.Uuid;
import org.apache.kafka.common.config.ConfigResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.streamshub.console.api.model.Topic;
import io.streamshub.console.api.security.PermissionService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TopicDescribeServiceTest {

    Admin admin;
    ConfigService configService;
    PermissionService permissionService;
    TopicDescribeService topicDescribeService;
    Uuid topicId;
    Topic topic;
    Map<Uuid, Topic> topics;

    @BeforeEach
    void setup() {
        admin = mock(Admin.class);
        configService = mock(ConfigService.class);
        permissionService = mock(PermissionService.class);
        topicDescribeService = new TopicDescribeService();
        topicDescribeService.configService = configService;
        topicDescribeService.permissionService = permissionService;

        topicId = Uuid.randomUuid();
        topic = new Topic("test-topic", false, topicId.toString());
        topics = Map.of(topicId, topic);

        when(permissionService.permitted(any(), any(), anyString())).thenReturn(true);
        when(configService.describeConfigs(eq(admin), anyList(), anyBoolean()))
                .thenReturn(CompletableFuture.completedFuture(Map.of()));
    }

    @Test
    void testMaybeDescribeConfigsWithExplicitFalseDoesNotRequestDocumentation() throws Exception {
        topicDescribeService.maybeDescribeConfigs(admin, topics, List.of(Topic.Fields.CONFIGS), false)
                .get(10, TimeUnit.SECONDS);

        verify(configService).describeConfigs(admin, topicKeys(), false);
    }

    @Test
    void testMaybeDescribeConfigsForwardsDocumentationRequest() throws Exception {
        topicDescribeService.maybeDescribeConfigs(admin, topics, List.of(Topic.Fields.CONFIGS), true)
                .get(10, TimeUnit.SECONDS);

        verify(configService).describeConfigs(admin, topicKeys(), true);
    }

    @Test
    void testConfigsAreNotFetchedWhenTopicFieldsExcludeConfigs() throws Exception {
        topicDescribeService.maybeDescribeConfigs(admin, topics, List.of(Topic.Fields.NAME), true)
                .get(10, TimeUnit.SECONDS);

        verifyNoInteractions(configService);
    }

    List<ConfigResource> topicKeys() {
        return List.of(new ConfigResource(ConfigResource.Type.TOPIC, topic.name()));
    }
}
