package com.github.streamshub.console.api.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonString;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.streamshub.console.api.support.ComparatorBuilder;
import com.github.streamshub.console.api.support.ListRequestContext;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;

@Schema(
        name = "Node",
        properties = {
            @SchemaProperty(name = "type", enumeration = Node.API_TYPE),
            @SchemaProperty(name = "meta", implementation = Node.Meta.class)
        })
@JsonInclude(value = Include.NON_NULL)
public class Node extends Resource<Node.Attributes> {

    public static final String API_TYPE = "nodes";
    public static final String FIELDS_PARAM = "fields[" + API_TYPE + "]";

    public static class Fields {
        public static final String NODE_POOL = "nodePool";
        public static final String ROLES = "roles";
        public static final String STATUS = "status";
        public static final String KAFKA_VERSION = "kafkaVersion";
        public static final String METADATA_STATE = "metadataState";
        public static final String BROKER = "broker";
        public static final String CONTROLLER = "controller";
        public static final String HOST = "host";
        public static final String PORT = "port";
        public static final String RACK = "rack";
        public static final String STORAGE_USED = "storageUsed";
        public static final String STORAGE_CAPACITY = "storageCapacity";

        static final Comparator<Node> ID_COMPARATOR =
                comparing(Node::numericId, nullsLast(Integer::compareTo));

        static final Map<String, Map<Boolean, Comparator<Node>>> COMPARATORS =
                ComparatorBuilder.bidirectional(
                        Map.of("id", ID_COMPARATOR,
                                NODE_POOL, comparing(Node::nodePool, nullsLast(String::compareTo)),
                                RACK, comparing(Node::rack, nullsLast(String::compareTo)),
                                STATUS, comparing(Node::status, nullsLast(String::compareTo)),
                                ROLES, comparing(Node::roles, nullsLast((r1, r2) -> {
                                    /*
                                     * Ranking
                                     * 1. Controllers
                                     * 2. Dual-role
                                     * 3. Brokers
                                     */
                                    int rank1 = r1.size() == 2 ? 2 : (r1.contains(Role.CONTROLLER) ? 1 : 3);
                                    int rank2 = r2.size() == 2 ? 2 : (r2.contains(Role.CONTROLLER) ? 1 : 3);
                                    return Integer.compare(rank1, rank2);
                                }))
                        )
                );

        public static final ComparatorBuilder<Node> COMPARATOR_BUILDER =
                new ComparatorBuilder<>(Node.Fields::comparator, Node.Fields.defaultComparator());

        public static final String LIST_DEFAULT =
                NODE_POOL + ", "
                        + ROLES + ", "
                        + STATUS + ", "
                        + KAFKA_VERSION + ", "
                        + METADATA_STATE + ", "
                        + BROKER + ", "
                        + CONTROLLER + ", "
                        + HOST + ", "
                        + PORT + ", "
                        + RACK + ", "
                        + STORAGE_USED + ", "
                        + STORAGE_CAPACITY;

        private Fields() {
            // Prevent instances
        }

        public static Comparator<Node> defaultComparator() {
            return ID_COMPARATOR;
        }

        public static Comparator<Node> comparator(String fieldName, boolean descending) {
            return COMPARATORS.getOrDefault(fieldName, Collections.emptyMap()).get(descending);
        }
    }

    @Schema(name = "NodeDataList")
    public static final class NodeDataList extends DataList<Node> {
        public NodeDataList(List<Node> data, ListRequestContext<Node> listSupport) {
            super(data.stream()
                    .map(entry -> {
                        entry.addMeta("page", listSupport.buildPageMeta(entry::toCursor));
                        return entry;
                    })
                    .toList());
            addMeta("page", listSupport.buildPageMeta());
            listSupport.meta().forEach(this::addMeta);
            listSupport.buildPageLinks(Node::toCursor).forEach(this::addLink);
        }
    }

    @Schema(name = "NodeData")
    public static final class NodeData extends DataSingleton<Node> {
        @JsonCreator
        public NodeData(@JsonProperty("data") Node data) {
            super(data);
        }
    }

    @Schema(name = "NodeMeta", additionalProperties = Object.class)
    @JsonInclude(value = Include.NON_NULL)
    public static final class Meta extends JsonApiMeta {
    }

    @JsonFilter("fieldFilter")
    public static class Attributes {
        public static record Broker(BrokerStatus status, int replicaCount, int leaderCount) {
            public Broker status(BrokerStatus status) {
                return new Broker(status, replicaCount, leaderCount);
            }
        }

        public static record Controller(ControllerStatus status) {
        }

        @JsonProperty
        String host;
        @JsonProperty
        Integer port;
        @JsonProperty
        String rack;
        @JsonProperty
        String nodePool;
        @JsonProperty
        String kafkaVersion;
        @JsonProperty
        Set<Role> roles;
        @JsonProperty
        MetadataState metadataState;
        @JsonProperty
        @Schema(nullable = true)
        Broker broker;
        @JsonProperty
        @Schema(nullable = true)
        Controller controller;
        @JsonProperty
        Long storageUsed;
        @JsonProperty
        Long storageCapacity;

        public Attributes(String host, Integer port, String rack, String nodePool, Set<Role> roles,
                MetadataState metadataState) {
            this.host = host;
            this.port = port;
            this.rack = rack;
            this.nodePool = nodePool;
            this.roles = roles != null ? new TreeSet<>(roles) : null;
            this.metadataState = metadataState;
        }

        public Attributes() {
        }

        @JsonProperty
        public String status() {
            String status = "Ready";

            if (broker != null) {
                if (broker.status() != BrokerStatus.RUNNING) {
                    status = "Warning";
                }
            }
            if (controller != null) {
                if (controller.status() == ControllerStatus.FOLLOWER_LAGGED) {
                    status = "Warning";
                }
            }

            return status;
        }
    }

    public enum Role {
        CONTROLLER,
        BROKER;

        @Override
        @JsonValue
        public String toString() {
            return name().toLowerCase(Locale.ENGLISH);
        }

        public static Role fromValue(String value) {
            Objects.requireNonNull(value);
            return valueOf(value.toUpperCase(Locale.ENGLISH));
        }
    }

    public enum BrokerStatus {

        NOT_RUNNING("NotRunning"),
        STARTING("Starting"),
        RECOVERY("Recovery"),
        RUNNING("Running"),
        PENDING_CONTROLLED_SHUTDOWN("PendingControlledShutdown"),
        SHUTTING_DOWN("ShuttingDown"),
        UNKNOWN("Unknown");

        final String value;

        private BrokerStatus(String value) {
            this.value = value;
        }

        public static BrokerStatus fromValue(String brokerState) {
            return switch (brokerState) {
                case "0" -> NOT_RUNNING;
                case "1" -> STARTING;
                case "2" -> RECOVERY;
                case "3" -> RUNNING;
                case "6" -> PENDING_CONTROLLED_SHUTDOWN;
                case "7" -> SHUTTING_DOWN;
                default -> UNKNOWN;
            };
        }

        @Override
        @JsonValue
        public String toString() {
            return value;
        }
    }

    public enum ControllerStatus {

        LEADER("QuorumLeader"),
        FOLLOWER("QuorumFollower"),
        FOLLOWER_LAGGED("QuorumFollowerLagged"),
        UNKNOWN("Unknown");

        final String value;

        private ControllerStatus(String value) {
            this.value = value;
        }

        public static ControllerStatus fromValue(String value) {
            for (ControllerStatus s : values()) {
                if (s.value.equals(value)) {
                    return s;
                }
            }
            return UNKNOWN;
        }

        @Override
        @JsonValue
        public String toString() {
            return value;
        }
    }

    public enum MetadataStatus {
        LEADER,
        FOLLOWER,
        OBSERVER;

        @Override
        @JsonValue
        public String toString() {
            return name().toLowerCase(Locale.ENGLISH);
        }
    }

    public static record MetadataState(
        MetadataStatus status,
        long logEndOffset,
        long lag
    ) {
    }

    public static Node fromKafkaModel(org.apache.kafka.common.Node node, String nodePool, Set<Role> roles, MetadataState metadataState) {
        return new Node(String.valueOf(node.id()), node.host(), node.port(), node.rack(), nodePool, roles, metadataState);
    }

    public static Node fromMetadataState(String id, String nodePool, Set<Role> roles, MetadataState metadataState) {
        return new Node(id, null, null, null, nodePool, roles, metadataState);
    }

    public static Node fromKafkaModel(org.apache.kafka.common.Node node) {
        return fromKafkaModel(node, null, null, null);
    }

    /**
     * Constructs a "cursor" Topic from the encoded string representation of the subset
     * of Topic fields used to compare entities for pagination/sorting.
     */
    public static Node fromCursor(JsonObject cursor) {
        if (cursor == null) {
            return null;
        }

        Node node = new Node(cursor.getString("id"));
        JsonObject attr = cursor.getJsonObject("attributes");
        node.attributes.nodePool = attr.getString(Fields.NODE_POOL, null);
        node.attributes.rack = attr.getString(Fields.RACK, null);
        node.attributes.roles = Optional.ofNullable(attr.getJsonArray(Fields.ROLES))
                .map(roles -> roles.stream()
                        .map(JsonString.class::cast)
                        .map(JsonString::getString)
                        .map(Role::fromValue)
                        .collect(Collectors.toSet()))
                .orElse(null);

        return node;
    }

    public String toCursor(List<String> sortFields) {
        JsonObjectBuilder cursor = Json.createObjectBuilder()
                .add("id", id);
        JsonObjectBuilder attrBuilder = Json.createObjectBuilder();

        if (sortFields.contains(Fields.NODE_POOL)) {
            attrBuilder.add(Fields.NODE_POOL, attributes.nodePool);
        }

        if (sortFields.contains(Fields.RACK)) {
            attrBuilder.add(Fields.RACK, attributes.rack);
        }

        if (sortFields.contains(Fields.ROLES)) {
            var roleBuilder = Json.createArrayBuilder();
            attributes.roles.stream().map(Node.Role::toString).forEach(roleBuilder::add);
            attrBuilder.add(Fields.ROLES, roleBuilder);
        }

        cursor.add("attributes", attrBuilder.build());

        return Base64.getUrlEncoder().encodeToString(cursor.build().toString().getBytes(StandardCharsets.UTF_8));
    }

    public Node(String id, String host, Integer port, String rack, String nodePool, Set<Role> roles,
            MetadataState metadataState) {
        super(id, API_TYPE, new Meta(), new Attributes(host, port, rack, nodePool, roles, metadataState));
    }

    public Node(String id) {
        super(id, API_TYPE, new Meta(), new Attributes());
    }

    @Override
    public JsonApiMeta metaFactory() {
        return new Meta();
    }

    @Override
    public Meta meta() {
        return (Meta) super.meta();
    }

    private Integer numericId() {
        return Integer.valueOf(id);
    }

    public Set<Node.Role> roles() {
        return attributes.roles;
    }

    @JsonIgnore
    public boolean isBroker() {
        return attributes.roles.contains(Role.BROKER);
    }

    @JsonIgnore
    public boolean isController() {
        return attributes.roles.contains(Role.CONTROLLER);
    }

    @JsonIgnore
    public boolean isQuorumLeader() {
        return attributes.metadataState.status == MetadataStatus.LEADER;
    }

    @JsonIgnore
    public boolean hasLag() {
        return attributes.metadataState.lag > 0;
    }

    public String nodePool() {
        return attributes.nodePool;
    }

    public String status() {
        return attributes.status();
    }

    @JsonIgnore
    public boolean isAddressable() {
        return Objects.nonNull(attributes.host);
    }

    public String rack() {
        return attributes.rack;
    }

    public void kafkaVersion(String kafkaVersion) {
        attributes.kafkaVersion = kafkaVersion;
    }

    public Attributes.Broker broker() {
        return attributes.broker;
    }

    public void broker(Attributes.Broker broker) {
        attributes.broker = broker;
    }

    public void controller(Attributes.Controller controller) {
        attributes.controller = controller;
    }

    public void storage(Long used, Long capacity) {
        attributes.storageUsed = used;
        attributes.storageCapacity = capacity;
    }
}
