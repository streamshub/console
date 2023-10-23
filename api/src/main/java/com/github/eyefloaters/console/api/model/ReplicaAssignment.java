package com.github.eyefloaters.console.api.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonIgnore;

public interface ReplicaAssignment {

    Map<String, List<Integer>> replicasAssignments();

    @JsonIgnore
    public default boolean hasReplicaAssignment(int partitionId) {
        var assignments = replicasAssignments();
        return assignments != null && assignments.containsKey(String.valueOf(partitionId));
    }

    /**
     * Retrieve the replica assignments for the given partitionId or an empty list
     * if no replica assignments are present or the partitionId does not have any
     * assignments defined.
     *
     * @param partitionId partitionId to retrieve the replica assignments
     * @return list of replica assignments for the partition, or empty list - never
     *         null
     */
    @JsonIgnore
    public default List<Integer> replicaAssignment(int partitionId) {
        return Optional.ofNullable(replicasAssignments())
            .map(assignments -> assignments.get(String.valueOf(partitionId)))
            .orElseGet(Collections::emptyList);
    }
}
