package com.github.eyefloaters.console.api.model;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

public class KafkaCluster {

    @Schema(name = "KafkaClusterListResponse")
    public static final class ListResponse extends DataListResponse<KafkaCluster> {
        public ListResponse(List<KafkaCluster> data) {
            super(data);
        }
    }

    @Schema(name = "KafkaClusterResponse")
    public static final class SingleResponse extends DataResponse<KafkaCluster> {
        public SingleResponse(KafkaCluster data) {
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

    public KafkaCluster() {
    }

    public KafkaCluster(String clusterId, List<Node> nodes, Node controller, List<String> authorizedOperations) {
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
