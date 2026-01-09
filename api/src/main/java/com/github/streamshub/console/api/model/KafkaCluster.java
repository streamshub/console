package com.github.streamshub.console.api.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.json.JsonObject;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.streamshub.console.api.model.jsonapi.JsonApiMeta;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRelationshipToMany;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRootData;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRootDataList;
import com.github.streamshub.console.api.model.kubernetes.KubeApiResource;
import com.github.streamshub.console.api.model.kubernetes.KubeAttributes;
import com.github.streamshub.console.api.model.kubernetes.PaginatedKubeResource;
import com.github.streamshub.console.api.support.ComparatorBuilder;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.config.ClusterKind;

import io.xlate.validation.constraints.Expression;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;

@Schema(
    name = "KafkaCluster",
    properties = {
        @SchemaProperty(name = "type", enumeration = KafkaCluster.API_TYPE),
        @SchemaProperty(name = "meta", implementation = KafkaCluster.Meta.class)
    })
@Expression(
    value = "self.id != null",
    message = "resource ID is required",
    node = "id",
    payload = ErrorCategory.InvalidResource.class)
@Expression(
    when = "self.type != null",
    value = "self.type == '" + KafkaCluster.API_TYPE + "'",
    message = "resource type conflicts with operation",
    node = "type",
    payload = ErrorCategory.ResourceConflict.class)
public class KafkaCluster extends KubeApiResource<KafkaCluster.Attributes, KafkaCluster.Relationships> implements PaginatedKubeResource {

    public static final String API_TYPE = "kafkas";
    public static final String FIELDS_PARAM = "fields[" + API_TYPE + "]";

    public static class Fields {
        public static final String NAME = "name";
        public static final String NAMESPACE = "namespace";
        public static final String CREATION_TIMESTAMP = "creationTimestamp";
        public static final String NODES = "nodes";
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
                + NAMESPACE + ", "
                + CREATION_TIMESTAMP + ", "
                + KAFKA_VERSION + ", "
                + STATUS + ", "
                + CONDITIONS + ", "
                + NODE_POOLS + ", "
                + CRUISE_CONTROL_ENABLED;

        public static final String DESCRIBE_DEFAULT =
                NAME + ", "
                + NAMESPACE + ", "
                + CREATION_TIMESTAMP + ", "
                + NODES + ", "
                + AUTHORIZED_OPERATIONS + ", "
                + LISTENERS + ", "
                + KAFKA_VERSION + ", "
                + STATUS + ", "
                + CONDITIONS + ", "
                + NODE_POOLS + ", "
                + CRUISE_CONTROL_ENABLED;

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

    @Schema(name = "KafkaClusterDataList")
    public static final class KafkaClusterDataList extends JsonApiRootDataList<KafkaCluster> {
        public KafkaClusterDataList(List<KafkaCluster> data, ListRequestContext<KafkaCluster> listSupport) {
            super(data.stream()
                    .map(entry -> {
                        entry.addMeta("page", listSupport.buildPageMeta(entry::toCursor));
                        return entry;
                    })
                    .toList());
            addMeta("page", listSupport.buildPageMeta());
            listSupport.meta().forEach(this::addMeta);
            listSupport.buildPageLinks(KafkaCluster::toCursor).forEach(this::addLink);
        }
    }

    @Schema(name = "KafkaClusterData")
    public static final class KafkaClusterData extends JsonApiRootData<KafkaCluster> {
        @JsonCreator
        public KafkaClusterData(@JsonProperty("data") KafkaCluster data) {
            super(data);
        }
    }

    @Schema(name = "KafkaClusterMeta", additionalProperties = Object.class)
    @JsonInclude(value = Include.NON_NULL)
    static final class Meta extends JsonApiMeta {
        @JsonProperty
        @Schema(description = """
                    Indicates whether reconciliation is paused for the associated Kafka \
                    custom resource. This value is optional and will not be present when \
                    the Kafka cluster is not (known) to be managed by Strimzi.
                    """)
        Boolean reconciliationPaused;

        @JsonProperty
        @Schema(description = "Kubernetes resource kind backing this Kafka cluster.",
                implementation = ClusterKind.class)
        ClusterKind kind;
    }

    @JsonFilter("fieldFilter")
    static class Attributes extends KubeAttributes {
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

        @JsonProperty
        Metrics metrics = new Metrics();

        @JsonCreator
        Attributes(List<String> authorizedOperations) {
            this.authorizedOperations = authorizedOperations;
        }
    }

    @JsonFilter("fieldFilter")
    static class Relationships {
        @JsonProperty
        JsonApiRelationshipToMany nodes = new JsonApiRelationshipToMany();
    }

    public KafkaCluster(String id, List<String> authorizedOperations) {
        super(id, API_TYPE, new Attributes(authorizedOperations), new Relationships());
    }

    @JsonCreator
    public KafkaCluster(String id, String type, Meta meta, Attributes attributes, Relationships relationships) {
        super(id, type, meta, attributes, relationships);
    }

    public static KafkaCluster fromId(String id) {
        return new KafkaCluster(id, null);
    }

    /**
     * Constructs a "cursor" Topic from the encoded string representation of the subset
     * of Topic fields used to compare entities for pagination/sorting.
     */
    public static KafkaCluster fromCursor(JsonObject cursor) {
        return PaginatedKubeResource.fromCursor(cursor, KafkaCluster::fromId);
    }

    @Override
    public JsonApiMeta metaFactory() {
        return new Meta();
    }

    public void reconciliationPaused(Boolean reconciliationPaused) {
        ((Meta) getOrCreateMeta()).reconciliationPaused = reconciliationPaused;
    }

    public Boolean reconciliationPaused() {
        return Optional.ofNullable(meta())
                .map(Meta.class::cast)
                .map(meta -> meta.reconciliationPaused)
                .orElse(null);
    }

    public void kind(ClusterKind kind) {
        ((Meta) getOrCreateMeta()).kind = kind;
    }

    public JsonApiRelationshipToMany nodes() {
        return relationships.nodes;
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
        return Boolean.TRUE.equals(meta("configured"));
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

    public void cruiseControlEnabled(boolean cruiseControlEnabled) {
        attributes.cruiseControlEnabled = cruiseControlEnabled;
    }

    public Metrics metrics() {
        return attributes.metrics;
    }

    public void metrics(Metrics metrics) {
        attributes.metrics = metrics;
    }
}
