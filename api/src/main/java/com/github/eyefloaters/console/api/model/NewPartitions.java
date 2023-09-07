package com.github.eyefloaters.console.api.model;

import java.util.List;

public class NewPartitions {

    private int totalCount;

    private List<List<Integer>> newAssignments;

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<List<Integer>> getNewAssignments() {
        return newAssignments;
    }

    public void setNewAssignments(List<List<Integer>> newAssignments) {
        this.newAssignments = newAssignments;
    }

}
