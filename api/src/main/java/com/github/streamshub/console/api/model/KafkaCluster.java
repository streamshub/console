package com.github.streamshub.console.api.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.streamshub.console.api.support.ComparatorBuilder;
import com.github.streamshub.console.api.support.ListRequestContext;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;

@Schema(name = "KafkaCluster")
public class KafkaCluster extends Resource<KafkaCluster.Attributes> {

    public static class Fields {
        public static final String NAME = "name";
        public static final String NAMESPACE = "namespace";
        public static final String CREATION_TIMESTAMP = "creationTimestamp";
        public static final String NODES = "nodes";
        public static final String CONTROLLER = "controller";
        public static final String AUTHORIZED_OPERATIONS = "authorizedOperations";
        public static final String LISTENERS = "listeners";
        public static final String METRICS = "metrics";
        public static final String KAFKA_VERSION = "kafkaVersion";
        public static final String STATUS = "status";
        public static final String CONDITIONS = "conditions";
        public static final String NODE_POOLS = "nodePools";
        public static final String CRUISE_CONTROL_ENABLED = "cruiseControlEnabled";

        static final Comparator<KafkaCluster> ID_COMPARATOR =
                comparing(KafkaCluster::getId, nullsLast(String::compareTo));

        static final Map<String, Map<Boolean, Comparator<KafkaCluster>>> COMPARATORS =
                ComparatorBuilder.bidirectional(
                        Map.of("id", ID_COMPARATOR,
                                NAME, comparing(KafkaCluster::name),
                                NAMESPACE, comparing(KafkaCluster::namespace),
                                CREATION_TIMESTAMP, comparing(KafkaCluster::creationTimestamp)));

        public static final ComparatorBuilder<KafkaCluster> COMPARATOR_BUILDER =
                new ComparatorBuilder<>(KafkaCluster.Fields::comparator, KafkaCluster.Fields.defaultComparator());

        public static final String LIST_DEFAULT =
                NAME + ", "
                + NAMESPACE;

        public static final String DESCRIBE_DEFAULT =
                NAME + ", "
                + NAMESPACE + ", "
                + CREATION_TIMESTAMP + ", "
                + NODES + ", "
                + CONTROLLER + ", "
                + AUTHORIZED_OPERATIONS + ", "
                + LISTENERS + ", "
                + KAFKA_VERSION + ", "
                + STATUS + ", "
                + CONDITIONS + ", "
                + NODE_POOLS;

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
    public static final class ListResponse extends DataList<KafkaCluster> {
        public ListResponse(List<KafkaCluster> data, ListRequestContext<KafkaCluster> listSupport) {
            super(data.stream()
                    .map(entry -> {
                        entry.addMeta("page", listSupport.buildPageMeta(entry::toCursor));
                        return entry;
                    })
                    .toList());
            addMeta("page", listSupport.buildPageMeta());
            listSupport.buildPageLinks(KafkaCluster::toCursor).forEach(this::addLink);
        }
    }

    @Schema(name = "KafkaClusterResponse")
    public static final class SingleResponse extends DataSingleton<KafkaCluster> {
        public SingleResponse(KafkaCluster data) {
            super(data);
        }
    }

    @JsonFilter("fieldFilter")
    static class Attributes {
        @JsonProperty
        String name; // Strimzi Kafka CR only

        @JsonProperty
        String namespace; // Strimzi Kafka CR only

        @JsonProperty
        String creationTimestamp; // Strimzi Kafka CR only

        @JsonProperty
        final List<Node> nodes;

        @JsonProperty
        final Node controller;

        @JsonProperty
        final List<String> authorizedOperations;

        @JsonProperty
        List<KafkaListener> listeners; // Strimzi Kafka CR only

        @JsonProperty
        String kafkaVersion;

        @JsonProperty
        String status;

        @JsonProperty
        List<Condition> conditions;

        @JsonProperty
        List<String> nodePools;

        @JsonProperty
        boolean cruiseControlEnabled;

        Attributes(List<Node> nodes, Node controller, List<String> authorizedOperations) {
            this.nodes = nodes;
            this.controller = controller;
            this.authorizedOperations = authorizedOperations;
        }
    }

    public KafkaCluster(String id, List<Node> nodes, Node controller, List<String> authorizedOperations) {
        super(id, "kafkas", new Attributes(nodes, controller, authorizedOperations));
    }

    /**
     * Constructs a "cursor" Topic from the encoded string representation of the subset
     * of Topic fields used to compare entities for pagination/sorting.
     */
    public static KafkaCluster fromCursor(JsonObject cursor) {
        if (cursor == null) {
            return null;
        }

        KafkaCluster cluster = new KafkaCluster(cursor.getString("id"), null, null, null);
        JsonObject attr = cursor.getJsonObject("attributes");
        cluster.name(attr.getString(Fields.NAME, null));
        cluster.namespace(attr.getString(Fields.NAMESPACE, null));
        cluster.creationTimestamp(attr.getString(Fields.CREATION_TIMESTAMP, null));

        return cluster;
    }

    public String toCursor(List<String> sortFields) {
        JsonObjectBuilder cursor = Json.createObjectBuilder()
                .add("id", id == null ? Json.createValue("") : Json.createValue(id));

        JsonObjectBuilder attrBuilder = Json.createObjectBuilder();
        maybeAddAttribute(attrBuilder, sortFields, Fields.NAME, attributes.name);
        maybeAddAttribute(attrBuilder, sortFields, Fields.NAMESPACE, attributes.namespace);
        maybeAddAttribute(attrBuilder, sortFields, Fields.CREATION_TIMESTAMP, attributes.creationTimestamp);
        cursor.add("attributes", attrBuilder.build());

        return Base64.getUrlEncoder().encodeToString(cursor.build().toString().getBytes(StandardCharsets.UTF_8));
    }

    static void maybeAddAttribute(JsonObjectBuilder attrBuilder, List<String> sortFields, String key, String value) {
        if (sortFields.contains(key)) {
            attrBuilder.add(key, value != null ? Json.createValue(value) : JsonValue.NULL);
        }
    }

    public String name() {
        return attributes.name;
    }

    public void name(String name) {
        attributes.name = name;
    }

    public String namespace() {
        return attributes.namespace;
    }

    public void namespace(String namespace) {
        attributes.namespace = namespace;
    }

    public String creationTimestamp() {
        return attributes.creationTimestamp;
    }

    public void creationTimestamp(String creationTimestamp) {
        attributes.creationTimestamp = creationTimestamp;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Node> nodes() {
        return attributes.nodes;
    }

    public Node controller() {
        return attributes.controller;
    }

    public List<String> authorizedOperations() {
        return attributes.authorizedOperations;
    }

    public List<KafkaListener> listeners() {
        return attributes.listeners;
    }

    public void listeners(List<KafkaListener> listeners) {
        attributes.listeners = listeners;
    }

    public String kafkaVersion() {
        return attributes.kafkaVersion;
    }

    public void kafkaVersion(String kafkaVersion) {
        attributes.kafkaVersion = kafkaVersion;
    }

    public String status() {
        return attributes.status;
    }

    public void status(String status) {
        attributes.status = status;
    }

    public List<Condition> conditions() {
        return attributes.conditions;
    }

    public void conditions(List<Condition> conditions) {
        attributes.conditions = conditions;
    }

    @JsonIgnore
    public boolean isConfigured() {
        return Boolean.TRUE.equals(getMeta("configured"));
    }

    @JsonIgnore
    public void setConfigured(boolean configured) {
        addMeta("configured", configured);
    }

    public List<String> nodePools() {
        return attributes.nodePools;
    }

    public void nodePools(List<String> nodePools) {
        attributes.nodePools = nodePools;
    }

    public boolean cruiseControlEnabled() {
        return attributes.cruiseControlEnabled;
    }

    public void cruiseControlEnabled(boolean cruiseControlEnabled) {
        attributes.cruiseControlEnabled = cruiseControlEnabled;
    }
}
