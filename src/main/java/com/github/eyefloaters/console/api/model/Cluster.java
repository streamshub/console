package com.github.eyefloaters.console.api.model;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class Cluster {

    @Schema(name = "ClusterListResponse")
    public static final class ListResponse extends DataListResponse<Cluster> {
        public ListResponse(List<Cluster> data) {
            super(data);
        }
    }

    @Schema(name = "ClusterResponse")
    public static final class SingleResponse extends DataResponse<Cluster> {
        public SingleResponse(Cluster data) {
            super(data);
        }
    }

    String kind = "Cluster";
    String name; // Strimzi Kafka CR only
    String clusterId;
    List<Node> nodes;
    Node controller;
    List<String> authorizedOperations;
    String bootstrapServers; // Strimzi Kafka CR only
    String authType; // Strimzi Kafka CR only

    public Cluster() {
    }

    public Cluster(String clusterId, List<Node> nodes, Node controller, List<String> authorizedOperations) {
        super();
        this.clusterId = clusterId;
        this.nodes = nodes;
        this.controller = controller;
        this.authorizedOperations = authorizedOperations;
    }

    public String getKind() {
        return kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClusterId() {
        return clusterId;
    }

    public void setClusterId(String clusterId) {
        this.clusterId = clusterId;
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

    public Node getController() {
        return controller;
    }

    public void setController(Node controller) {
        this.controller = controller;
    }

    public List<String> getAuthorizedOperations() {
        return authorizedOperations;
    }

    public void setAuthorizedOperations(List<String> authorizedOperations) {
        this.authorizedOperations = authorizedOperations;
    }

    public String getBootstrapServers() {
        return bootstrapServers;
    }

    public void setBootstrapServers(String bootstrapServers) {
        this.bootstrapServers = bootstrapServers;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }
}
