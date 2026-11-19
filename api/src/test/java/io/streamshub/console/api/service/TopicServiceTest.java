package io.streamshub.console.api.service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.common.Uuid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.streamshub.console.api.model.Topic;
import io.streamshub.console.api.support.ListRequestContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TopicServiceTest {

    TopicDescribeService topicDescribe;
    TopicService topicService;

    @BeforeEach
    void setup() {
        topicDescribe = mock(TopicDescribeService.class);
        topicService = new TopicService();
        topicService.topicDescribe = topicDescribe;
    }

    @Test
    void testLegacyListTopicsDoesNotRequestDocumentation() {
        when(topicDescribe.listTopics(anyList(), anyString(), any(), eq(false)))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        topicService.listTopics(List.of(), "latest", listSupport()).toCompletableFuture().join();

        verify(topicDescribe).listTopics(anyList(), anyString(), any(), eq(false));
    }

    @Test
    void testListTopicsForwardsDocumentationRequest() {
        when(topicDescribe.listTopics(anyList(), anyString(), any(), eq(true)))
                .thenReturn(CompletableFuture.completedFuture(List.of()));

        topicService.listTopics(List.of(), "latest", listSupport(), true).toCompletableFuture().join();

        verify(topicDescribe).listTopics(anyList(), anyString(), any(), eq(true));
    }

    @Test
    void testLegacyDescribeTopicDoesNotRequestDocumentation() {
        Topic topic = topic();
        when(topicDescribe.describeTopic(anyString(), anyList(), anyString(), eq(false)))
                .thenReturn(CompletableFuture.completedFuture(topic));

        topicService.describeTopic(topic.getId(), List.of(), "latest").toCompletableFuture().join();

        verify(topicDescribe).describeTopic(anyString(), anyList(), anyString(), eq(false));
    }

    @Test
    void testDescribeTopicForwardsDocumentationRequest() {
        Topic topic = topic();
        when(topicDescribe.describeTopic(anyString(), anyList(), anyString(), eq(true)))
                .thenReturn(CompletableFuture.completedFuture(topic));

        topicService.describeTopic(topic.getId(), List.of(), "latest", true).toCompletableFuture().join();

        verify(topicDescribe).describeTopic(anyString(), anyList(), anyString(), eq(true));
    }

    Topic topic() {
        return new Topic("test-topic", false, Uuid.randomUuid().toString());
    }

    @SuppressWarnings("unchecked")
    ListRequestContext<Topic> listSupport() {
        return mock(ListRequestContext.class);
    }
}
