package io.streamshub.console.api.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;

import jakarta.ws.rs.ForbiddenException;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ClassicGroupDescription;
import org.apache.kafka.clients.admin.DescribeClassicGroupsOptions;
import org.apache.kafka.clients.admin.DescribeClassicGroupsResult;
import org.apache.kafka.clients.admin.GroupListing;
import org.apache.kafka.common.ClassicGroupState;
import org.apache.kafka.common.GroupState;
import org.apache.kafka.common.GroupType;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.config.ConfigResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.streamshub.console.api.model.ConfigEntry;
import io.streamshub.console.api.model.Either;
import io.streamshub.console.api.model.Group;
import io.streamshub.console.api.security.PermissionService;
import io.streamshub.console.api.support.ContextualExecutorProvider;
import io.streamshub.console.api.support.KafkaContext;
import io.streamshub.console.api.support.ListRequestContext;
import io.streamshub.console.config.security.Privilege;
import io.streamshub.console.config.security.ResourceTypes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GroupServiceTest {

    Admin admin;
    ConfigService configService;
    PermissionService permissionService;
    KafkaContext kafkaContext;
    ContextualExecutorProvider threadContext;
    TopicDescribeService topicService;
    GroupService groupService;
    Map<String, Group> groups;

    @BeforeEach
    void setup() {
        admin = mock(Admin.class);
        configService = mock(ConfigService.class);
        permissionService = mock(PermissionService.class);
        kafkaContext = mock(KafkaContext.class);
        threadContext = mock(ContextualExecutorProvider.class);
        topicService = mock(TopicDescribeService.class);
        groupService = new GroupService();
        groupService.configService = configService;
        groupService.permissionService = permissionService;
        groupService.kafkaContext = kafkaContext;
        groupService.threadContext = threadContext;
        groupService.topicService = topicService;
        groups = Map.of("test-group", group("test-group"));

        when(kafkaContext.admin()).thenReturn(admin);
        when(threadContext.currentContextExecutor()).thenReturn(Runnable::run);
        when(permissionService.permitted(any(), any(), anyString())).thenReturn(true);
        when(configService.describeConfigs(eq(admin), anyList(), anyBoolean()))
            .thenReturn(CompletableFuture.completedFuture(Map.of(
                    "test-group", Either.of(configs()))));
    }

    @Test
    void testMaybeDescribeConfigsWithExplicitFalseDoesNotRequestDocumentation() throws Exception {
        groupService.maybeDescribeConfigs(admin, groups, List.of(Group.Fields.CONFIGS), false)
            .get(10, TimeUnit.SECONDS);

        verify(configService).describeConfigs(admin, groupKeys(), false);
    }

    @Test
    void testMaybeDescribeConfigsForwardsDocumentationRequest() throws Exception {
        groupService.maybeDescribeConfigs(admin, groups, List.of(Group.Fields.CONFIGS), true)
            .get(10, TimeUnit.SECONDS);

        verify(configService).describeConfigs(admin, groupKeys(), true);
    }

    @Test
    void testConfigsAreNotFetchedWhenGroupFieldsExcludeConfigs() throws Exception {
        groupService.maybeDescribeConfigs(admin, groups, List.of(Group.Fields.GROUP_ID), true)
            .get(10, TimeUnit.SECONDS);

        verify(configService, never()).describeConfigs(any(), anyList(), anyBoolean());
    }

    @Test
    void testAugmentListFetchesConfigsWithoutDocumentation() throws Exception {
        Group group = groups.get("test-group");
        List<Group> result = groupService.augmentList(
                admin,
                List.of(group),
                List.of(Group.Fields.GROUP_ID, Group.Fields.CONFIGS),
                false)
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);

        assertSame(group, result.get(0));
        assertNotNull(group.configs());
        verify(configService).describeConfigs(admin, groupKeys(), false);
    }

    @Test
    void testAugmentListFetchesConfigsWithDocumentation() throws Exception {
        Group group = groups.get("test-group");
        List<Group> result = groupService.augmentList(
                admin,
                List.of(group),
                List.of(Group.Fields.GROUP_ID, Group.Fields.CONFIGS),
                true)
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);

        assertSame(group, result.get(0));
        verify(configService).describeConfigs(admin, groupKeys(), true);
    }

    @Test
    void testAugmentListDoesNotFetchConfigsWhenExcluded() throws Exception {
        groupService.augmentList(
                admin,
                List.of(groups.get("test-group")),
                List.of(Group.Fields.GROUP_ID),
                true)
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);

        verify(configService, never()).describeConfigs(any(), anyList(), anyBoolean());
    }

    @Test
    void testAugmentListWithDescribeFlowFetchesConfigs() throws Exception {
        stubDescribeFlow();

        Group group = groups.get("test-group");
        List<Group> result = groupService.augmentList(
                admin,
                List.of(group),
                List.of(Group.Fields.GROUP_ID, Group.Fields.MEMBERS, Group.Fields.CONFIGS),
                true)
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);

        assertSame(group, result.get(0));
        assertNotNull(group.configs());
        assertEquals("45000", group.configs().getPrimary().get("session.timeout.ms").getValue());
        verify(configService).describeConfigs(admin, groupKeys(), true);
    }

    @Test
    void testAugmentListWithDescribeFlowDoesNotFetchConfigsTwice() throws Exception {
        stubDescribeFlow();

        groupService.augmentList(
                admin,
                List.of(groups.get("test-group")),
                List.of(Group.Fields.GROUP_ID, Group.Fields.MEMBERS, Group.Fields.CONFIGS),
                true)
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);

        verify(configService, times(1)).describeConfigs(admin, groupKeys(), true);
    }

    @Test
    void testMaybeDescribeConfigsSkipsConfigServiceWhenNoGroupsPermitted() throws Exception {
        Group group = groups.get("test-group");

        when(permissionService.permitted(
                eq(ResourceTypes.Kafka.GROUP_CONFIGS), eq(Privilege.GET), eq("test-group")))
            .thenReturn(false);
        when(permissionService.forbidden(
                eq(ResourceTypes.Kafka.GROUP_CONFIGS), eq(Privilege.GET), eq("test-group")))
            .thenReturn(new ForbiddenException("denied"));

        groupService.maybeDescribeConfigs(admin, Map.of("test-group", group), List.of(Group.Fields.CONFIGS), true)
            .get(10, TimeUnit.SECONDS);

        verify(configService, never()).describeConfigs(any(), anyList(), anyBoolean());
        assertNotNull(group.configs());
    }

    @Test
    void testDescribeGroupForwardsDocumentationRequest() throws Exception {
        GroupService service = spy(new GroupService());
        service.kafkaContext = kafkaContext;
        service.configService = configService;
        service.permissionService = permissionService;
        service.threadContext = threadContext;
        service.topicService = topicService;

        stubDescribeFlow();
        doAnswer(invocation -> (UnaryOperator<Object>) item -> item)
            .when(permissionService).addPrivileges(any(), any());

        Group group = groups.get("test-group");
        doReturn(CompletableFuture.completedFuture(group))
            .when(service).findGroup(admin, "test-group");

        Group result = service.describeGroup(
                Group.encodeGroupId("test-group"),
                List.of(Group.Fields.GROUP_ID, Group.Fields.CONFIGS),
                true)
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);

        assertNotNull(result);
        assertEquals("45000", result.configs().getPrimary().get("session.timeout.ms").getValue());
        verify(configService).describeConfigs(admin, groupKeys(), true);
    }

    @SuppressWarnings("unchecked")
    @Test
    void testListGroupsWithExplicitFalse() throws Exception {
        GroupService service = spy(new GroupService());
        ListRequestContext<Group> listSupport = mock(ListRequestContext.class);

        doReturn(CompletableFuture.completedFuture(List.of()))
            .when(service)
            .listGroups(anyList(), same(listSupport), eq(false));

        service.listGroups(List.of(Group.Fields.GROUP_ID), listSupport, false)
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);

        verify(service).listGroups(anyList(), same(listSupport), eq(false));
    }

    @Test
    void testDescribeGroupWithExplicitFalse() throws Exception {
        GroupService service = spy(new GroupService());
        Group group = group("test-group");

        doReturn(CompletableFuture.completedFuture(group))
            .when(service)
            .describeGroup(anyString(), anyList(), eq(false));

        service.describeGroup(Group.encodeGroupId("test-group"), List.of(Group.Fields.GROUP_ID), false)
            .toCompletableFuture()
            .get(10, TimeUnit.SECONDS);

        verify(service).describeGroup(anyString(), anyList(), eq(false));
    }

    void stubDescribeFlow() {
        when(topicService.listTopics(true, true))
            .thenReturn(CompletableFuture.completedFuture(List.of()));
        when(admin.describeClassicGroups(anyCollection(), any(DescribeClassicGroupsOptions.class)))
            .thenReturn(new DescribeClassicGroupsResult(Map.of(
                    "test-group", KafkaFuture.completedFuture(classicGroupDescription()))));
    }

    ClassicGroupDescription classicGroupDescription() {
        return new ClassicGroupDescription(
                "test-group",
                "consumer",
                null,
                List.of(),
                ClassicGroupState.STABLE,
                new Node(0, "localhost", 9092));
    }

    List<ConfigResource> groupKeys() {
        return List.of(new ConfigResource(ConfigResource.Type.GROUP, "test-group"));
    }

    Map<String, ConfigEntry> configs() {
        ConfigEntry entry = new ConfigEntry();
        entry.setValue("45000");
        entry.setType("STRING");
        return Map.of("session.timeout.ms", entry);
    }

    Group group(String groupId) {
        GroupListing listing = mock(GroupListing.class);
        when(listing.groupId()).thenReturn(groupId);
        when(listing.isSimpleConsumerGroup()).thenReturn(false);
        when(listing.groupState()).thenReturn(Optional.of(GroupState.STABLE));
        when(listing.type()).thenReturn(Optional.of(GroupType.CLASSIC));
        when(listing.protocol()).thenReturn("consumer");
        return Group.fromKafkaModel(listing);
    }
}
