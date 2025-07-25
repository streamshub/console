package com.github.streamshub.console.api.model.connect;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.json.JsonObject;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.streamshub.console.api.model.Metrics;
import com.github.streamshub.console.api.model.jsonapi.JsonApiMeta;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRelationshipToMany;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRelationshipToOne;
import com.github.streamshub.console.api.model.jsonapi.JsonApiResource;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRootData;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRootDataList;
import com.github.streamshub.console.api.model.kubernetes.KubeApiResource;
import com.github.streamshub.console.api.model.kubernetes.KubeAttributes;
import com.github.streamshub.console.api.model.kubernetes.PaginatedKubeResource;
import com.github.streamshub.console.api.support.ComparatorBuilder;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.ListRequestContext;

import io.xlate.validation.constraints.Expression;

import static com.github.streamshub.console.api.support.ComparatorBuilder.nullable;
import static java.util.Comparator.comparing;

@Schema(
    name = "KafkaConnector",
    properties = {
        @SchemaProperty(name = "type", enumeration = Connector.API_TYPE),
        @SchemaProperty(name = "meta", implementation = Connector.Meta.class)
    })
@Expression(
    value = "self.id != null",
    message = "resource ID is required",
    node = "id",
    payload = ErrorCategory.InvalidResource.class)
@Expression(
    when = "self.type != null",
    value = "self.type == '" + Connector.API_TYPE + "'",
    message = "resource type conflicts with operation",
    node = "type",
    payload = ErrorCategory.ResourceConflict.class)
public class Connector extends KubeApiResource<Connector.Attributes, Connector.Relationships> implements PaginatedKubeResource {

    public static final String API_TYPE = "connectors";
    public static final String FIELDS_PARAM = "fields[" + API_TYPE + "]";

    @Schema(hidden = true)
    public enum Fields {
        NAME("name"),
        NAMESPACE("namespace"),
        CREATION_TIMESTAMP("creationTimestamp"),
        TYPE("type"),
        CONFIG("config"),
        STATE("state"),
        TRACE("trace"),
        WORKER_ID("workerId"),
        TOPICS("topics"),
        OFFSETS("offsets"),
        CONNECT_CLUSTER("connectCluster"),
        TASKS("tasks");

        public static final String LIST_DEFAULT = "name,namespace,creationTimestamp,type,state,trace,workerId";

        private final String value;

        private Fields(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return value;
        }

        static final Comparator<Connector> ID_COMPARATOR = nullable(Connector::getId);

        static final Map<String, Map<Boolean, Comparator<Connector>>> COMPARATORS =
                ComparatorBuilder.bidirectional(
                        Map.of("id", ID_COMPARATOR,
                                NAME.value, comparing(Connector::name),
                                NAMESPACE.value, nullable(Connector::namespace),
                                CREATION_TIMESTAMP.value, nullable(Connector::creationTimestamp)));

        public static final ComparatorBuilder<Connector> COMPARATOR_BUILDER =
                new ComparatorBuilder<>(Connector.Fields::comparator, Connector.Fields.defaultComparator());

        public static Comparator<Connector> defaultComparator() {
            return ID_COMPARATOR;
        }

        public static Comparator<Connector> comparator(String fieldName, boolean descending) {
            return COMPARATORS.getOrDefault(fieldName, Collections.emptyMap()).get(descending);
        }
    }

    @Schema(name = "KafkaConnectorDataList")
    public static final class DataList extends JsonApiRootDataList<Connector> {
        public DataList(List<Connector> data, ListRequestContext<Connector> listSupport) {
            super(data.stream()
                    .map(entry -> {
                        entry.addMeta("page", listSupport.buildPageMeta(entry::toCursor));
                        return entry;
                    })
                    .toList());
            addMeta("page", listSupport.buildPageMeta());
            listSupport.buildPageLinks(Connector::toCursor).forEach(this::addLink);

            if (listSupport.getFetchParams().includes(Fields.CONNECT_CLUSTER.value)) {
                data.stream()
                    .map(c -> c.relationships.connectClusterResource)
                    .filter(Objects::nonNull)
                    .forEach(this::addIncluded);
            }

            if (listSupport.getFetchParams().includes(Fields.TASKS.value)) {
                data.stream()
                    .map(c -> c.relationships.taskResources)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .forEach(this::addIncluded);
            }
        }
    }

    @Schema(name = "KafkaConnectorData")
    public static final class Data extends JsonApiRootData<Connector> {
        @JsonCreator
        public Data(@JsonProperty("data") Connector data) {
            super(data);
        }
    }

    @Schema(name = "KafkaConnectorMeta", additionalProperties = Object.class)
    @JsonInclude(value = Include.NON_NULL)
    static final class Meta extends JsonApiMeta {
    }

    public static record ConnectorOffset(
            Map<String, Object> offset,
            Map<String, Object> partition
    ) { /* Data container only */ }

    @JsonFilter(FIELDS_PARAM)
    static class Attributes extends KubeAttributes {
        @JsonProperty
        String type;

        @JsonProperty
        Map<String, String> config;

        @JsonProperty
        String state;

        @JsonProperty
        String trace;

        @JsonProperty
        String workerId;

        @JsonProperty
        List<String> topics;

        @JsonProperty
        List<ConnectorOffset> offsets;

        @JsonProperty
        Metrics metrics;
    }

    @JsonFilter(FIELDS_PARAM)
    static class Relationships {
        @JsonProperty
        JsonApiRelationshipToOne connectCluster;

        @JsonIgnore
        ConnectCluster connectClusterResource;

        @JsonProperty
        JsonApiRelationshipToMany tasks;

        @JsonIgnore
        List<ConnectorTask> taskResources = Collections.emptyList();
    }

    public Connector(String id) {
        super(id, API_TYPE, new Attributes(), new Relationships());
    }

    public static Connector fromId(String id) {
        return new Connector(id);
    }

    /**
     * Constructs a "cursor" Topic from the encoded string representation of the subset
     * of Topic fields used to compare entities for pagination/sorting.
     */
    public static Connector fromCursor(JsonObject cursor) {
        return PaginatedKubeResource.fromCursor(cursor, Connector::fromId);
    }

    @Override
    public JsonApiMeta metaFactory() {
        return new Meta();
    }

    public Metrics metrics() {
        return attributes.metrics;
    }

    public void metrics(Metrics metrics) {
        attributes.metrics = metrics;
    }

    public void type(String type) {
        attributes.type = type;
    }

    public void config(Map<String, String> config) {
        attributes.config = config;
    }

    public void state(String state) {
        attributes.state = state;
    }

    public void trace(String trace) {
        attributes.trace = trace;
    }

    public void workerId(String workerId) {
        attributes.workerId = workerId;
    }

    public Connector topics(List<String> topics) {
        attributes.topics = topics;
        return this;
    }

    public Connector offsets(List<ConnectorOffset> offsets) {
        attributes.offsets = offsets;
        return this;
    }

    public Connector connectCluster(ConnectCluster cluster, boolean include) {
        if (include) {
            relationships.connectClusterResource = cluster;
        }
        if (cluster != null) {
            var relationship = new JsonApiRelationshipToOne(cluster.identifier());
            relationships.connectCluster = relationship;
        }
        return this;
    }

    public Connector tasks(List<ConnectorTask> tasks, boolean include) {
        if (include) {
            relationships.taskResources = tasks;
        }
        var relationship = new JsonApiRelationshipToMany(tasks.stream().map(JsonApiResource::identifier).toList());
        relationships.tasks = relationship;
        return this;
    }

    public List<ConnectorTask> taskResources() {
        return relationships.taskResources;
    }
}
