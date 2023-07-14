package io.strimzi.kafka.instance.model;

import static io.strimzi.kafka.instance.BlockingSupplier.get;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.CreateTopicsResult;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class NewTopic {

    private String name;
    private String topicId;
    private Integer numPartitions;
    private Short replicationFactor;
    private Map<Integer, List<Integer>> replicasAssignments;
    private Map<String, String> configs = null;

    public static NewTopic fromKafkaModel(String topicName, CreateTopicsResult result) {
        NewTopic response = new NewTopic();

        response.setName(topicName);
        response.setTopicId(get(() -> result.topicId(topicName)).toString());
        response.setNumPartitions(get(() -> result.numPartitions(topicName)));
        response.setReplicationFactor(get(() -> result.replicationFactor(topicName)).shortValue());
        response.setConfigs(get(() -> result.config(topicName))
                .entries()
                .stream()
                .collect(Collectors.toMap(
                        org.apache.kafka.clients.admin.ConfigEntry::name,
                        org.apache.kafka.clients.admin.ConfigEntry::value)));

        return response;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTopicId() {
        return topicId;
    }

    public void setTopicId(String topicId) {
        this.topicId = topicId;
    }

    public Integer getNumPartitions() {
        return numPartitions;
    }

    public void setNumPartitions(Integer numPartitions) {
        this.numPartitions = numPartitions;
    }

    public Short getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(Short replicationFactor) {
        this.replicationFactor = replicationFactor;
    }

    public Map<Integer, List<Integer>> getReplicasAssignments() {
        return replicasAssignments;
    }

    public void setReplicasAssignments(Map<Integer, List<Integer>> replicasAssignments) {
        this.replicasAssignments = replicasAssignments;
    }

    public Map<String, String> getConfigs() {
        return configs;
    }

    public void setConfigs(Map<String, String> configs) {
        this.configs = configs;
    }

}
