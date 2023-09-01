package com.github.eyefloaters.console.api.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.eyefloaters.console.api.support.ComparatorBuilder;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;

@Schema(name = "KafkaClusterAttributes")
@JsonFilter("fieldFilter")
public class KafkaCluster {

    public static class Fields {
        public static final String NAME = "name";
        public static final String NAMESPACE = "namespace";
        public static final String CREATION_TIMESTAMP = "creationTimestamp";
        public static final String NODES = "nodes";
        public static final String CONTROLLER = "controller";
        public static final String AUTHORIZED_OPERATIONS = "authorizedOperations";
        public static final String BOOTSTRAP_SERVERS = "bootstrapServers";
        public static final String AUTH_TYPE = "authType";

        static final Comparator<KafkaCluster> ID_COMPARATOR =
                comparing(KafkaCluster::getId);

        static final Map<String, Map<Boolean, Comparator<KafkaCluster>>> COMPARATORS =
                ComparatorBuilder.bidirectional(
                        Map.of("id", ID_COMPARATOR,
                                NAME, comparing(KafkaCluster::getName),
                                NAMESPACE, comparing(KafkaCluster::getNamespace),
                                CREATION_TIMESTAMP, comparing(KafkaCluster::getCreationTimestamp),
                                BOOTSTRAP_SERVERS, comparing(KafkaCluster::getBootstrapServers, nullsLast(String::compareTo)),
                                AUTH_TYPE, comparing(KafkaCluster::getAuthType, nullsLast(String::compareTo))));

        public static final String LIST_DEFAULT =
                NAME + ", "
                + NAMESPACE + ", "
                + CREATION_TIMESTAMP + ", "
                + BOOTSTRAP_SERVERS + ", "
                + AUTH_TYPE;

        public static final String DESCRIBE_DEFAULT =
                NAME + ", "
                + NAMESPACE + ", "
                + CREATION_TIMESTAMP + ", "
                + NODES + ", "
                + CONTROLLER + ", "
                + AUTHORIZED_OPERATIONS + ", "
                + BOOTSTRAP_SERVERS + ", "
                + AUTH_TYPE;

        private Fields() {
            // Prevent instances
        }

        public static Comparator<KafkaCluster> defaultComparator() {
            return ID_COMPARATOR;
        }

        public static Comparator<KafkaCluster> comparator(String fieldName, boolean descending) {
            return COMPARATORS.getOrDefault(fieldName, Collections.emptyMap()).get(descending);
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
            super(data.id, "kafkas", data);
        }
    }

    String name; // Strimzi Kafka CR only
    String namespace; // Strimzi Kafka CR only
    String creationTimestamp; // Strimzi Kafka CR only
    @JsonIgnore
    final String id;
    final List<Node> nodes;
    final Node controller;
    final List<String> authorizedOperations;
    String bootstrapServers; // Strimzi Kafka CR only
    String authType; // Strimzi Kafka CR only

    public KafkaCluster(String id, List<Node> nodes, Node controller, List<String> authorizedOperations) {
        super();
        this.id = id;
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

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getCreationTimestamp() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(String creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public String getId() {
        return id;
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
