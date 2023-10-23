package com.github.eyefloaters.console.api.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
public class MemberDescription {

    private final String memberId;
    private final String groupInstanceId;
    private final String clientId;
    private final String host;
    private List<PartitionKey> assignments;

    public static MemberDescription fromKafkaModel(org.apache.kafka.clients.admin.MemberDescription description) {
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
                .map(partition -> new PartitionKey(null, partition.topic(), partition.partition()))
                .toList();

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

    public List<PartitionKey> getAssignments() {
        return assignments;
    }

    public void setAssignments(List<PartitionKey> assignments) {
        this.assignments = assignments;
    }
}
