package com.github.eyefloaters.console.test;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Named;

import org.apache.kafka.clients.admin.AbortTransactionOptions;
import org.apache.kafka.clients.admin.AbortTransactionResult;
import org.apache.kafka.clients.admin.AbortTransactionSpec;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterClientQuotasOptions;
import org.apache.kafka.clients.admin.AlterClientQuotasResult;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigsOptions;
import org.apache.kafka.clients.admin.AlterConfigsResult;
import org.apache.kafka.clients.admin.AlterConsumerGroupOffsetsOptions;
import org.apache.kafka.clients.admin.AlterConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.AlterPartitionReassignmentsOptions;
import org.apache.kafka.clients.admin.AlterPartitionReassignmentsResult;
import org.apache.kafka.clients.admin.AlterReplicaLogDirsOptions;
import org.apache.kafka.clients.admin.AlterReplicaLogDirsResult;
import org.apache.kafka.clients.admin.AlterUserScramCredentialsOptions;
import org.apache.kafka.clients.admin.AlterUserScramCredentialsResult;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.CreateAclsOptions;
import org.apache.kafka.clients.admin.CreateAclsResult;
import org.apache.kafka.clients.admin.CreateDelegationTokenOptions;
import org.apache.kafka.clients.admin.CreateDelegationTokenResult;
import org.apache.kafka.clients.admin.CreatePartitionsOptions;
import org.apache.kafka.clients.admin.CreatePartitionsResult;
import org.apache.kafka.clients.admin.CreateTopicsOptions;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.DeleteAclsOptions;
import org.apache.kafka.clients.admin.DeleteAclsResult;
import org.apache.kafka.clients.admin.DeleteConsumerGroupOffsetsOptions;
import org.apache.kafka.clients.admin.DeleteConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.DeleteConsumerGroupsOptions;
import org.apache.kafka.clients.admin.DeleteConsumerGroupsResult;
import org.apache.kafka.clients.admin.DeleteRecordsOptions;
import org.apache.kafka.clients.admin.DeleteRecordsResult;
import org.apache.kafka.clients.admin.DeleteTopicsOptions;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.DescribeAclsOptions;
import org.apache.kafka.clients.admin.DescribeAclsResult;
import org.apache.kafka.clients.admin.DescribeClientQuotasOptions;
import org.apache.kafka.clients.admin.DescribeClientQuotasResult;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.DescribeConfigsOptions;
import org.apache.kafka.clients.admin.DescribeConfigsResult;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsOptions;
import org.apache.kafka.clients.admin.DescribeConsumerGroupsResult;
import org.apache.kafka.clients.admin.DescribeDelegationTokenOptions;
import org.apache.kafka.clients.admin.DescribeDelegationTokenResult;
import org.apache.kafka.clients.admin.DescribeFeaturesOptions;
import org.apache.kafka.clients.admin.DescribeFeaturesResult;
import org.apache.kafka.clients.admin.DescribeLogDirsOptions;
import org.apache.kafka.clients.admin.DescribeLogDirsResult;
import org.apache.kafka.clients.admin.DescribeMetadataQuorumOptions;
import org.apache.kafka.clients.admin.DescribeMetadataQuorumResult;
import org.apache.kafka.clients.admin.DescribeProducersOptions;
import org.apache.kafka.clients.admin.DescribeProducersResult;
import org.apache.kafka.clients.admin.DescribeReplicaLogDirsOptions;
import org.apache.kafka.clients.admin.DescribeReplicaLogDirsResult;
import org.apache.kafka.clients.admin.DescribeTopicsOptions;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.DescribeTransactionsOptions;
import org.apache.kafka.clients.admin.DescribeTransactionsResult;
import org.apache.kafka.clients.admin.DescribeUserScramCredentialsOptions;
import org.apache.kafka.clients.admin.DescribeUserScramCredentialsResult;
import org.apache.kafka.clients.admin.ElectLeadersOptions;
import org.apache.kafka.clients.admin.ElectLeadersResult;
import org.apache.kafka.clients.admin.ExpireDelegationTokenOptions;
import org.apache.kafka.clients.admin.ExpireDelegationTokenResult;
import org.apache.kafka.clients.admin.FeatureUpdate;
import org.apache.kafka.clients.admin.FenceProducersOptions;
import org.apache.kafka.clients.admin.FenceProducersResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsOptions;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsResult;
import org.apache.kafka.clients.admin.ListConsumerGroupOffsetsSpec;
import org.apache.kafka.clients.admin.ListConsumerGroupsOptions;
import org.apache.kafka.clients.admin.ListConsumerGroupsResult;
import org.apache.kafka.clients.admin.ListOffsetsOptions;
import org.apache.kafka.clients.admin.ListOffsetsResult;
import org.apache.kafka.clients.admin.ListPartitionReassignmentsOptions;
import org.apache.kafka.clients.admin.ListPartitionReassignmentsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.admin.ListTransactionsOptions;
import org.apache.kafka.clients.admin.ListTransactionsResult;
import org.apache.kafka.clients.admin.NewPartitionReassignment;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.admin.RecordsToDelete;
import org.apache.kafka.clients.admin.RemoveMembersFromConsumerGroupOptions;
import org.apache.kafka.clients.admin.RemoveMembersFromConsumerGroupResult;
import org.apache.kafka.clients.admin.RenewDelegationTokenOptions;
import org.apache.kafka.clients.admin.RenewDelegationTokenResult;
import org.apache.kafka.clients.admin.UnregisterBrokerOptions;
import org.apache.kafka.clients.admin.UnregisterBrokerResult;
import org.apache.kafka.clients.admin.UpdateFeaturesOptions;
import org.apache.kafka.clients.admin.UpdateFeaturesResult;
import org.apache.kafka.clients.admin.UserScramCredentialAlteration;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ElectionType;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.TopicCollection;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.TopicPartitionReplica;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.acl.AclBindingFilter;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.quota.ClientQuotaAlteration;
import org.apache.kafka.common.quota.ClientQuotaFilter;
import org.mockito.Mockito;

import com.github.eyefloaters.console.api.ClientFactory;

import io.quarkus.test.junit.QuarkusMock;

/**
 * Provides a mechanism for integration tests to spy and override
 * particular Admin methods to, for example, force an error condition.
 *
 * See {@link #install(Consumer)} for usage.
 */
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class AdminClientSpy implements Admin {

    static final TypeLiteral<Function<Map<String, Object>, Admin>> CLIENT_BUILDER_TYPE_LITERAL =
            new TypeLiteral<Function<Map<String, Object>, Admin>>() {
                private static final long serialVersionUID = 1L;
            };

    static final AnnotationLiteral<Named> NAMED_ANNOTATION = new NamedKafkaAdminBuilder();

    @SuppressWarnings("all")
    static class NamedKafkaAdminBuilder extends AnnotationLiteral<Named> implements Named {
        private static final long serialVersionUID = 1L;

        @Override
        public String value() {
            return "kafkaAdminBuilder";
        }
    }

    /**
     * Create and install a spy Admin instance to be used for a request instead of
     * {@link ClientFactory#kafkaAdminBuilder}.
     *
     * @param adminSetup a consumer that accepts the Admin client for spying
     */
    public static void install(Consumer<Admin> adminSetup) {
        Function<Map<String, Object>, Admin> builder = config -> {
            Admin client = Mockito.spy(new AdminClientSpy(config));
            adminSetup.accept(client);
            return client;
        };

        QuarkusMock.installMockForType(builder, CLIENT_BUILDER_TYPE_LITERAL, NAMED_ANNOTATION);
    }

    final Admin delegate;

    private AdminClientSpy(Map<String, Object> config) {
        delegate = Admin.create(config);
    }

    public void close(Duration timeout) {
        delegate.close(timeout);
    }

    public CreateTopicsResult createTopics(Collection<NewTopic> newTopics, CreateTopicsOptions options) {
        return delegate.createTopics(newTopics, options);
    }

    public DeleteTopicsResult deleteTopics(TopicCollection topics, DeleteTopicsOptions options) {
        return delegate.deleteTopics(topics, options);
    }

    public ListTopicsResult listTopics(ListTopicsOptions options) {
        return delegate.listTopics(options);
    }

    public DescribeTopicsResult describeTopics(TopicCollection topics, DescribeTopicsOptions options) {
        return delegate.describeTopics(topics, options);
    }

    public DescribeClusterResult describeCluster(DescribeClusterOptions options) {
        return delegate.describeCluster(options);
    }

    public DescribeAclsResult describeAcls(AclBindingFilter filter, DescribeAclsOptions options) {
        return delegate.describeAcls(filter, options);
    }

    public CreateAclsResult createAcls(Collection<AclBinding> acls, CreateAclsOptions options) {
        return delegate.createAcls(acls, options);
    }

    public DeleteAclsResult deleteAcls(Collection<AclBindingFilter> filters, DeleteAclsOptions options) {
        return delegate.deleteAcls(filters, options);
    }

    public DescribeConfigsResult describeConfigs(Collection<ConfigResource> resources, DescribeConfigsOptions options) {
        return delegate.describeConfigs(resources, options);
    }

    @Override
    @Deprecated
    public AlterConfigsResult alterConfigs(Map<ConfigResource, Config> configs, AlterConfigsOptions options) {
        return delegate.alterConfigs(configs, options);
    }

    public AlterConfigsResult incrementalAlterConfigs(Map<ConfigResource, Collection<AlterConfigOp>> configs,
            AlterConfigsOptions options) {
        return delegate.incrementalAlterConfigs(configs, options);
    }

    public AlterReplicaLogDirsResult alterReplicaLogDirs(Map<TopicPartitionReplica, String> replicaAssignment,
            AlterReplicaLogDirsOptions options) {
        return delegate.alterReplicaLogDirs(replicaAssignment, options);
    }

    public DescribeLogDirsResult describeLogDirs(Collection<Integer> brokers, DescribeLogDirsOptions options) {
        return delegate.describeLogDirs(brokers, options);
    }

    public DescribeReplicaLogDirsResult describeReplicaLogDirs(Collection<TopicPartitionReplica> replicas,
            DescribeReplicaLogDirsOptions options) {
        return delegate.describeReplicaLogDirs(replicas, options);
    }

    public CreatePartitionsResult createPartitions(Map<String, NewPartitions> newPartitions,
            CreatePartitionsOptions options) {
        return delegate.createPartitions(newPartitions, options);
    }

    public DeleteRecordsResult deleteRecords(Map<TopicPartition, RecordsToDelete> recordsToDelete,
            DeleteRecordsOptions options) {
        return delegate.deleteRecords(recordsToDelete, options);
    }

    public CreateDelegationTokenResult createDelegationToken(CreateDelegationTokenOptions options) {
        return delegate.createDelegationToken(options);
    }

    public RenewDelegationTokenResult renewDelegationToken(byte[] hmac, RenewDelegationTokenOptions options) {
        return delegate.renewDelegationToken(hmac, options);
    }

    public ExpireDelegationTokenResult expireDelegationToken(byte[] hmac, ExpireDelegationTokenOptions options) {
        return delegate.expireDelegationToken(hmac, options);
    }

    public DescribeDelegationTokenResult describeDelegationToken(DescribeDelegationTokenOptions options) {
        return delegate.describeDelegationToken(options);
    }

    public DescribeConsumerGroupsResult describeConsumerGroups(Collection<String> groupIds,
            DescribeConsumerGroupsOptions options) {
        return delegate.describeConsumerGroups(groupIds, options);
    }

    public ListConsumerGroupsResult listConsumerGroups(ListConsumerGroupsOptions options) {
        return delegate.listConsumerGroups(options);
    }

    public ListConsumerGroupOffsetsResult listConsumerGroupOffsets(Map<String, ListConsumerGroupOffsetsSpec> groupSpecs,
            ListConsumerGroupOffsetsOptions options) {
        return delegate.listConsumerGroupOffsets(groupSpecs, options);
    }

    public DeleteConsumerGroupsResult deleteConsumerGroups(Collection<String> groupIds,
            DeleteConsumerGroupsOptions options) {
        return delegate.deleteConsumerGroups(groupIds, options);
    }

    public DeleteConsumerGroupOffsetsResult deleteConsumerGroupOffsets(String groupId,
            Set<TopicPartition> partitions,
            DeleteConsumerGroupOffsetsOptions options) {
        return delegate.deleteConsumerGroupOffsets(groupId, partitions, options);
    }

    public ElectLeadersResult electLeaders(ElectionType electionType,
            Set<TopicPartition> partitions,
            ElectLeadersOptions options) {
        return delegate.electLeaders(electionType, partitions, options);
    }

    public AlterPartitionReassignmentsResult alterPartitionReassignments(
            Map<TopicPartition, Optional<NewPartitionReassignment>> reassignments,
            AlterPartitionReassignmentsOptions options) {
        return delegate.alterPartitionReassignments(reassignments, options);
    }

    public ListPartitionReassignmentsResult listPartitionReassignments(Optional<Set<TopicPartition>> partitions,
            ListPartitionReassignmentsOptions options) {
        return delegate.listPartitionReassignments(partitions, options);
    }

    public RemoveMembersFromConsumerGroupResult removeMembersFromConsumerGroup(String groupId,
            RemoveMembersFromConsumerGroupOptions options) {
        return delegate.removeMembersFromConsumerGroup(groupId, options);
    }

    public AlterConsumerGroupOffsetsResult alterConsumerGroupOffsets(String groupId,
            Map<TopicPartition, OffsetAndMetadata> offsets,
            AlterConsumerGroupOffsetsOptions options) {
        return delegate.alterConsumerGroupOffsets(groupId, offsets, options);
    }

    public ListOffsetsResult listOffsets(Map<TopicPartition, OffsetSpec> topicPartitionOffsets,
            ListOffsetsOptions options) {
        return delegate.listOffsets(topicPartitionOffsets, options);
    }

    public DescribeClientQuotasResult describeClientQuotas(ClientQuotaFilter filter,
            DescribeClientQuotasOptions options) {
        return delegate.describeClientQuotas(filter, options);
    }

    public AlterClientQuotasResult alterClientQuotas(Collection<ClientQuotaAlteration> entries,
            AlterClientQuotasOptions options) {
        return delegate.alterClientQuotas(entries, options);
    }

    public DescribeUserScramCredentialsResult describeUserScramCredentials(List<String> users,
            DescribeUserScramCredentialsOptions options) {
        return delegate.describeUserScramCredentials(users, options);
    }

    public AlterUserScramCredentialsResult alterUserScramCredentials(List<UserScramCredentialAlteration> alterations,
            AlterUserScramCredentialsOptions options) {
        return delegate.alterUserScramCredentials(alterations, options);
    }

    public DescribeFeaturesResult describeFeatures(DescribeFeaturesOptions options) {
        return delegate.describeFeatures(options);
    }

    public UpdateFeaturesResult updateFeatures(Map<String, FeatureUpdate> featureUpdates,
            UpdateFeaturesOptions options) {
        return delegate.updateFeatures(featureUpdates, options);
    }

    public DescribeMetadataQuorumResult describeMetadataQuorum(DescribeMetadataQuorumOptions options) {
        return delegate.describeMetadataQuorum(options);
    }

    public UnregisterBrokerResult unregisterBroker(int brokerId, UnregisterBrokerOptions options) {
        return delegate.unregisterBroker(brokerId, options);
    }

    public DescribeProducersResult describeProducers(Collection<TopicPartition> partitions,
            DescribeProducersOptions options) {
        return delegate.describeProducers(partitions, options);
    }

    public DescribeTransactionsResult describeTransactions(Collection<String> transactionalIds,
            DescribeTransactionsOptions options) {
        return delegate.describeTransactions(transactionalIds, options);
    }

    public AbortTransactionResult abortTransaction(AbortTransactionSpec spec, AbortTransactionOptions options) {
        return delegate.abortTransaction(spec, options);
    }

    public ListTransactionsResult listTransactions(ListTransactionsOptions options) {
        return delegate.listTransactions(options);
    }

    public FenceProducersResult fenceProducers(Collection<String> transactionalIds, FenceProducersOptions options) {
        return delegate.fenceProducers(transactionalIds, options);
    }

    public Map<MetricName, ? extends Metric> metrics() {
        return delegate.metrics();
    }


}
