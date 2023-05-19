package io.strimzi.kafka.instance.model;

import java.util.List;

public class Cluster {

    String kind = "Cluster";
    String clusterId;
    List<Node> nodes;
    Node controller;
    List<String> authorizedOperations;

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


}
