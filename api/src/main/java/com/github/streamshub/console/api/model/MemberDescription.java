package com.github.streamshub.console.api.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Description of a member of a group
 */
@JsonInclude(value = Include.NON_NULL)
public class MemberDescription {

    private final String memberId;
    private final String groupInstanceId;
    private final String clientId;
    private final String host;
    private List<PartitionId> assignments;

    /**
     * Construct an instance from a Kafka member description and the map of topic
     * Ids, used to set the topic ID of the assignments field elements.
     *
     * @param description Kafka member description model for any type of group
     * @param topicIds    map of topic names to Ids
     * @return a new {@linkplain MemberDescription}
     */
    public static MemberDescription fromKafkaModel(Object description, Map<String, String> topicIds) {
        switch (description) {
            case org.apache.kafka.clients.admin.MemberDescription consumerGroupMember:
                return fromKafkaModel(consumerGroupMember, topicIds);
            case org.apache.kafka.clients.admin.ShareMemberDescription shareGroupMember:
                return fromKafkaModel(shareGroupMember, topicIds);
            case org.apache.kafka.clients.admin.StreamsGroupMemberDescription streamsGroupMember:
                return fromKafkaModel(streamsGroupMember);
            default:
                throw new IllegalArgumentException("Unknown member type: " + description.getClass());
        }
    }

    private static MemberDescription fromKafkaModel(
            org.apache.kafka.clients.admin.MemberDescription description,
            Map<String, String> topicIds) {

        MemberDescription result = new MemberDescription(
                description.consumerId(),
                description.groupInstanceId().orElse(null),
                description.clientId(),
                description.host());

        /*
         * assignments remains mutable to allow replacement with PartitionKeys once
         * topic IDs are known.
         */
        result.assignments = description.assignment()
                .topicPartitions()
                .stream()
                /*
                 * Filter out assigned partitions not visible to the client
                 * configured to connect to Kafka cluster.
                 */
                .filter(partition -> topicIds.containsKey(partition.topic()))
                .map(partition -> new PartitionId(
                        topicIds.get(partition.topic()),
                        partition.topic(),
                        partition.partition()))
                .toList();

        return result;
    }

    private static MemberDescription fromKafkaModel(
            org.apache.kafka.clients.admin.ShareMemberDescription description,
            Map<String, String> topicIds) {

        MemberDescription result = new MemberDescription(
                description.consumerId(),
                null,
                description.clientId(),
                description.host());

        /*
         * assignments remains mutable to allow replacement with PartitionKeys once
         * topic IDs are known.
         */
        result.assignments = description.assignment()
                .topicPartitions()
                .stream()
                /*
                 * Filter out assigned partitions not visible to the client
                 * configured to connect to Kafka cluster.
                 */
                .filter(partition -> topicIds.containsKey(partition.topic()))
                .map(partition -> new PartitionId(
                        topicIds.get(partition.topic()),
                        partition.topic(),
                        partition.partition()))
                .toList();

        return result;
    }

    private static MemberDescription fromKafkaModel(
            org.apache.kafka.clients.admin.StreamsGroupMemberDescription description) {
        MemberDescription result = new MemberDescription(
                description.memberId(),
                description.instanceId().orElse(null),
                description.clientId(),
                description.clientHost());

        /*
         * XXX: extract assignments from streams group topology (if possible?)
         */
        result.assignments = Collections.emptyList();

        return result;
    }

    public MemberDescription(String memberId, String groupInstanceId, String clientId, String host) {
        super();
        this.memberId = memberId;
        this.groupInstanceId = groupInstanceId;
        this.clientId = clientId;
        this.host = host;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getGroupInstanceId() {
        return groupInstanceId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getHost() {
        return host;
    }

    public List<PartitionId> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<PartitionId> assignments) {
        this.assignments = assignments;
    }
}
