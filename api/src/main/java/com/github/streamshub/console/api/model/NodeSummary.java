package com.github.streamshub.console.api.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Schema(name = "NodeSummary")
@JsonInclude(value = Include.NON_NULL)
public class NodeSummary {

    private final Map<String, NodePool> nodePools = new HashMap<>();
    private final Statuses statuses = new Statuses();
    private String leaderId;

    @Schema(name = "NodeSummaryPools")
    public static record NodePool(
            Set<Node.Role> roles,
            @Schema(implementation = int.class)
            AtomicInteger count
    ) {
    }

    @Schema(name = "NodeSummaryStatuses")
    public static record Statuses(
            Map<String, Integer> controllers,
            Map<String, Integer> brokers,
            Map<String, Integer> combined
    ) {
        Statuses() {
            this(new HashMap<>(), new HashMap<>(), new HashMap<>());
        }
    }

    public NodePool nodePool(String name) {
        return nodePools.computeIfAbsent(name, k -> new NodePool(new TreeSet<>(), new AtomicInteger()));
    }

    public Map<String, NodePool> getNodePools() {
        return nodePools;
    }

    public Statuses getStatuses() {
        return statuses;
    }

    public void setLeaderId(String leaderId) {
        this.leaderId = leaderId;
    }

    public String getLeaderId() {
        return leaderId;
    }
}
