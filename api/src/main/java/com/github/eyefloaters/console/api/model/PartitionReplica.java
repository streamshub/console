package com.github.eyefloaters.console.api.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = Include.NON_NULL)
public class PartitionReplica {

    private final int nodeId;
    private final String nodeRack;
    private final boolean inSync;
    private Either<ReplicaLocalStorage, Error> localStorage;

    public PartitionReplica(int nodeId, String nodeRack, boolean inSync) {
        this.nodeId = nodeId;
        this.nodeRack = nodeRack;
        this.inSync = inSync;
    }

    public static PartitionReplica fromKafkaModel(org.apache.kafka.common.Node node, List<Integer> isr) {
        return new PartitionReplica(node.id(), node.rack(), isr.contains(node.id()));
    }

    @JsonProperty
    public int nodeId() {
        return nodeId;
    }

    @JsonProperty
    public String nodeRack() {
        return nodeRack;
    }

    @JsonProperty
    public boolean inSync() {
        return inSync;
    }

    @JsonProperty
    public Either<ReplicaLocalStorage, Error> localStorage() {
        return localStorage;
    }

    public void localStorage(Either<ReplicaLocalStorage, Error> localStorage) {
        this.localStorage = localStorage;
    }
}
