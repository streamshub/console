package com.github.eyefloaters.console.api.model;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Schema(name = "KafkaClusterAttributes")
@JsonFilter("fieldFilter")
public class KafkaCluster {

    public class Fields {
        public static final String NAME = "name";
        public static final String NODES = "nodes";
        public static final String CONTROLLER = "controller";
        public static final String AUTHORIZED_OPERATIONS = "authorizedOperations";
        public static final String BOOTSTRAP_SERVERS = "bootstrapServers";
        public static final String AUTH_TYPE = "authType";

        public static final String LIST_DEFAULT =
                NAME + ", "
                + BOOTSTRAP_SERVERS + ", "
                + AUTH_TYPE;

        public static final String DESCRIBE_DEFAULT =
                NAME + ", "
                + NODES + ", "
                + CONTROLLER + ", "
                + AUTHORIZED_OPERATIONS + ", "
                + BOOTSTRAP_SERVERS + ", "
                + AUTH_TYPE;

        private Fields() {
        }
    }


    @Schema(name = "KafkaClusterListResponse")
    public static final class ListResponse extends DataListResponse<KafkaClusterResource> {
        public ListResponse(List<KafkaCluster> data) {
            super(data.stream().map(KafkaClusterResource::new).toList());
        }
    }

    @Schema(name = "KafkaClusterResponse")
    public static final class SingleResponse extends DataResponse<KafkaClusterResource> {
        public SingleResponse(KafkaCluster data) {
            super(new KafkaClusterResource(data));
        }
    }

    @Schema(name = "KafkaCluster")
    public static final class KafkaClusterResource extends Resource<KafkaCluster> {
        public KafkaClusterResource(KafkaCluster data) {
            super(data.clusterId, "kafkas", data);
        }
    }

    String name; // Strimzi Kafka CR only
    @JsonIgnore
    final String clusterId;
    final List<Node> nodes;
    final Node controller;
    final List<String> authorizedOperations;
    String bootstrapServers; // Strimzi Kafka CR only
    String authType; // Strimzi Kafka CR only

    public KafkaCluster(String clusterId, List<Node> nodes, Node controller, List<String> authorizedOperations) {
        super();
        this.clusterId = clusterId;
        this.nodes = nodes;
        this.controller = controller;
        this.authorizedOperations = authorizedOperations;
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

    public List<Node> getNodes() {
        return nodes;
    }

    public Node getController() {
        return controller;
    }

    public List<String> getAuthorizedOperations() {
        return authorizedOperations;
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
