package com.github.streamshub.console.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.json.JsonObject;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.streamshub.console.api.model.jsonapi.JsonApiMeta;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRootData;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRootDataList;
import com.github.streamshub.console.api.model.jsonapi.None;
import com.github.streamshub.console.api.model.kubernetes.KubeApiResource;
import com.github.streamshub.console.api.model.kubernetes.KubeAttributes;
import com.github.streamshub.console.api.model.kubernetes.PaginatedKubeResource;
import com.github.streamshub.console.api.support.ComparatorBuilder;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.api.support.StringEnumeration;

import io.xlate.validation.constraints.Expression;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;

@Schema(
    name = "KafkaRebalance",
    properties = {
        @SchemaProperty(name = "type", enumeration = KafkaRebalance.API_TYPE),
        @SchemaProperty(name = "meta", implementation = KafkaRebalance.Meta.class)
    })
@Expression(
    value = "self.id != null",
    message = "resource ID is required",
    node = "id",
    payload = ErrorCategory.InvalidResource.class)
@Expression(
    when = "self.type != null",
    value = "self.type == '" + KafkaRebalance.API_TYPE + "'",
    message = "resource type conflicts with operation",
    node = "type",
    payload = ErrorCategory.ResourceConflict.class)
public class KafkaRebalance extends KubeApiResource<KafkaRebalance.Attributes, None> implements PaginatedKubeResource {

    public static final String API_TYPE = "kafkaRebalances";
    public static final String FIELDS_PARAM = "fields[" + API_TYPE + "]";

    public static class Fields {
        public static final String NAME = "name";
        public static final String NAMESPACE = "namespace";
        public static final String CREATION_TIMESTAMP = "creationTimestamp";
        public static final String STATUS = "status";
        public static final String MODE = "mode";
        public static final String BROKERS = "brokers";
        public static final String GOALS = "goals";
        public static final String SKIP_HARD_GOAL_CHECK = "skipHardGoalCheck";
        public static final String REBALANCE_DISK = "rebalanceDisk";
        public static final String EXCLUDED_TOPICS = "excludedTopics";
        public static final String CONCURRENT_PARTITION_MOVEMENTS_PER_BROKER = "concurrentPartitionMovementsPerBroker";
        public static final String CONCURRENT_INTRABROKER_PARTITION_MOVEMENTS = "concurrentIntraBrokerPartitionMovements";
        public static final String CONCURRENT_LEADER_MOVEMENTS = "concurrentLeaderMovements";
        public static final String REPLICATION_THROTTLE = "replicationThrottle";
        public static final String REPLICA_MOVEMENT_STRATEGIES = "replicaMovementStrategies";
        public static final String SESSION_ID = "sessionId";
        public static final String OPTIMIZATION_RESULT = "optimizationResult";
        public static final String CONDITIONS = "conditions";

        static final Comparator<KafkaRebalance> ID_COMPARATOR =
                comparing(KafkaRebalance::getId, nullsLast(String::compareTo));

        static final Map<String, Map<Boolean, Comparator<KafkaRebalance>>> COMPARATORS =
                ComparatorBuilder.bidirectional(
                        Map.of("id", ID_COMPARATOR,
                                NAME, comparing(KafkaRebalance::name),
                                NAMESPACE, comparing(KafkaRebalance::namespace),
                                CREATION_TIMESTAMP, comparing(KafkaRebalance::creationTimestamp),
                                MODE, comparing(KafkaRebalance::mode, nullsLast(String::compareTo)),
                                STATUS, comparing(KafkaRebalance::status, nullsLast(String::compareTo))));

        public static final ComparatorBuilder<KafkaRebalance> COMPARATOR_BUILDER =
                new ComparatorBuilder<>(KafkaRebalance.Fields::comparator, KafkaRebalance.Fields.defaultComparator());

        public static final String LIST_DEFAULT =
                NAME + ", "
                + NAMESPACE + ", "
                + CREATION_TIMESTAMP + ", "
                + STATUS;

        public static final String DESCRIBE_DEFAULT =
                NAME + ", "
                + NAMESPACE + ", "
                + CREATION_TIMESTAMP + ", "
                + STATUS + ", "
                + MODE + ", "
                + BROKERS + ", "
                + GOALS + ", "
                + SKIP_HARD_GOAL_CHECK + ", "
                + REBALANCE_DISK + ", "
                + EXCLUDED_TOPICS + ", "
                + CONCURRENT_PARTITION_MOVEMENTS_PER_BROKER + ", "
                + CONCURRENT_INTRABROKER_PARTITION_MOVEMENTS + ", "
                + CONCURRENT_LEADER_MOVEMENTS + ", "
                + REPLICATION_THROTTLE + ", "
                + REPLICA_MOVEMENT_STRATEGIES + ", "
                + SESSION_ID + ", "
                + OPTIMIZATION_RESULT + ", "
                + CONDITIONS;

        private Fields() {
            // Prevent instances
        }

        public static Comparator<KafkaRebalance> defaultComparator() {
            return ID_COMPARATOR;
        }

        public static Comparator<KafkaRebalance> comparator(String fieldName, boolean descending) {
            return COMPARATORS.getOrDefault(fieldName, Collections.emptyMap()).get(descending);
        }
    }

    @Schema(name = "KafkaRebalanceDataList")
    public static final class RebalanceDataList extends JsonApiRootDataList<KafkaRebalance> {
        public RebalanceDataList(List<KafkaRebalance> data, ListRequestContext<KafkaRebalance> listSupport) {
            super(data.stream()
                    .map(entry -> {
                        entry.addMeta("page", listSupport.buildPageMeta(entry::toCursor));
                        return entry;
                    })
                    .toList());
            addMeta("page", listSupport.buildPageMeta());
            listSupport.meta().forEach(this::addMeta);
            listSupport.buildPageLinks(KafkaRebalance::toCursor).forEach(this::addLink);
        }
    }

    @Schema(name = "KafkaRebalanceData")
    public static final class RebalanceData extends JsonApiRootData<KafkaRebalance> {
        @JsonCreator
        public RebalanceData(@JsonProperty("data") KafkaRebalance data) {
            super(data);
        }
    }

    @Schema(name = "KafkaRebalanceMeta", additionalProperties = Object.class)
    @JsonInclude(value = Include.NON_NULL)
    static final class Meta extends JsonApiMeta {

        @JsonProperty
        @Schema(
                description = """
                    Valid values allowed for the meta.action property for this resource, \
                    given its current status.
                    """,
                readOnly = true)
        List<String> allowedActions = new ArrayList<>(3);

        @JsonProperty
        @Schema(
                description = """
                    Optimization proposal will be auto-approved without the \
                    need for manual approval.
                    """,
                readOnly = true)
        boolean autoApproval;

        @JsonProperty
        @Schema(
            description = """
                Action to be taken against the Kafka Rebalance resource. \
                Depends on the current resource state.
                """,
            enumeration = { "approve", "refresh", "stop" },
            writeOnly = true,
            nullable = true)
        @StringEnumeration(
            allowedValues = { "approve", "refresh", "stop" },
            payload = ErrorCategory.InvalidResource.class,
            message = "invalid rebalance action"
        )
        /**
         * @see io.strimzi.api.kafka.model.rebalance.KafkaRebalanceAnnotation
         */
        String action;
    }

    @JsonFilter("fieldFilter")
    @Schema(name = "KafkaRebalanceAttributes")
    static class Attributes extends KubeAttributes {
        @JsonProperty
        @Schema(readOnly = true, nullable = true)
        String status;

        @JsonProperty
        @Schema(readOnly = true, nullable = true)
        String mode;

        @JsonProperty
        @Schema(readOnly = true, nullable = true)
        List<Integer> brokers;

        @JsonProperty
        @Schema(readOnly = true, nullable = true)
        List<String> goals;

        @JsonProperty
        @Schema(readOnly = true)
        boolean skipHardGoalCheck;

        @JsonProperty
        @Schema(readOnly = true)
        boolean rebalanceDisk;

        @JsonProperty
        @Schema(readOnly = true, nullable = true)
        String excludedTopics;

        @JsonProperty
        @Schema(readOnly = true)
        int concurrentPartitionMovementsPerBroker;

        @JsonProperty
        @Schema(readOnly = true)
        int concurrentIntraBrokerPartitionMovements;

        @JsonProperty
        @Schema(readOnly = true)
        int concurrentLeaderMovements;

        @JsonProperty
        @Schema(readOnly = true)
        long replicationThrottle;

        @JsonProperty
        @Schema(readOnly = true, nullable = true)
        List<String> replicaMovementStrategies;

        @JsonProperty
        @Schema(readOnly = true, nullable = true)
        String sessionId;

        @JsonProperty
        @Schema(readOnly = true)
        Map<String, Object> optimizationResult = new HashMap<>(0);

        @JsonProperty
        @Schema(readOnly = true)
        List<Condition> conditions;
    }

    public KafkaRebalance(String id) {
        super(id, API_TYPE, new Attributes());
    }

    @JsonCreator
    public KafkaRebalance(String id, String type, KafkaRebalance.Attributes attributes, Meta meta) {
        super(id, type, meta, attributes);
    }

    /**
     * Constructs a "cursor" KafkaRebalance from the encoded string representation of the subset
     * of KafkaRebalance fields used to compare entities for pagination/sorting.
     */
    public static KafkaRebalance fromCursor(JsonObject cursor) {
        return PaginatedKubeResource.fromCursor(cursor, KafkaRebalance::new);
    }

    @Override
    public JsonApiMeta metaFactory() {
        return new Meta();
    }

    public List<String> allowedActions() {
        return ((Meta) getOrCreateMeta()).allowedActions;
    }

    public void autoApproval(boolean autoApproval) {
        ((Meta) getOrCreateMeta()).autoApproval = autoApproval;
    }

    public String action() {
        return Optional.ofNullable(meta())
                .map(Meta.class::cast)
                .map(meta -> meta.action)
                .orElse(null);
    }

    public String status() {
        return attributes.status;
    }

    public void status(String status) {
        attributes.status = status;
    }

    public String mode() {
        return attributes.mode;
    }

    public void mode(String mode) {
        attributes.mode = mode;
    }

    public void brokers(List<Integer> brokers) {
        attributes.brokers = brokers;
    }

    public void goals(List<String> goals) {
        attributes.goals = goals;
    }

    public void skipHardGoalCheck(boolean skipHardGoalCheck) {
        attributes.skipHardGoalCheck = skipHardGoalCheck;
    }

    public void rebalanceDisk(boolean rebalanceDisk) {
        attributes.rebalanceDisk = rebalanceDisk;
    }

    public void excludedTopics(String excludedTopics) {
        attributes.excludedTopics = excludedTopics;
    }

    public void concurrentPartitionMovementsPerBroker(int concurrentPartitionMovementsPerBroker) {
        attributes.concurrentPartitionMovementsPerBroker = concurrentPartitionMovementsPerBroker;
    }

    public void concurrentIntraBrokerPartitionMovements(int concurrentIntraBrokerPartitionMovements) {
        attributes.concurrentIntraBrokerPartitionMovements = concurrentIntraBrokerPartitionMovements;
    }

    public void concurrentLeaderMovements(int concurrentLeaderMovements) {
        attributes.concurrentLeaderMovements = concurrentLeaderMovements;
    }

    public void replicationThrottle(long replicationThrottle) {
        attributes.replicationThrottle = replicationThrottle;
    }

    public void replicaMovementStrategies(List<String> replicaMovementStrategies) {
        attributes.replicaMovementStrategies = replicaMovementStrategies;
    }

    public void sessionId(String sessionId) {
        attributes.sessionId = sessionId;
    }

    public Map<String, Object> optimizationResult() {
        return attributes.optimizationResult;
    }

    public void conditions(List<Condition> conditions) {
        attributes.conditions = conditions;
    }
}
