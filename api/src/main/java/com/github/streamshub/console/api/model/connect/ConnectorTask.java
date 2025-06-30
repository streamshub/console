package com.github.streamshub.console.api.model.connect;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

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
import com.github.streamshub.console.api.model.jsonapi.JsonApiRelationshipToOne;
import com.github.streamshub.console.api.model.jsonapi.JsonApiResource;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRootData;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRootDataList;
import com.github.streamshub.console.api.support.ComparatorBuilder;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.ListRequestContext;

import io.xlate.validation.constraints.Expression;

import static com.github.streamshub.console.api.support.ComparatorBuilder.nullable;

@Schema(
    name = "KafkaConnectorTask",
    properties = {
        @SchemaProperty(name = "type", enumeration = ConnectorTask.API_TYPE),
        @SchemaProperty(name = "meta", implementation = ConnectorTask.Meta.class)
    })
@Expression(
    value = "self.id != null",
    message = "resource ID is required",
    node = "id",
    payload = ErrorCategory.InvalidResource.class)
@Expression(
    when = "self.type != null",
    value = "self.type == '" + ConnectorTask.API_TYPE + "'",
    message = "resource type conflicts with operation",
    node = "type",
    payload = ErrorCategory.ResourceConflict.class)
public class ConnectorTask extends JsonApiResource<ConnectorTask.Attributes, ConnectorTask.Relationships> {

    public static final String API_TYPE = "connectorTasks";
    public static final String FIELDS_PARAM = "fields[" + API_TYPE + "]";

    @Schema(hidden = true)
    public enum Fields {
        TASK_ID("taskId"),
        CONFIG("config"),
        STATE("state"),
        TRACE("trace"),
        WORKER_ID("workerId"),
        CONNECTOR("connector");

        public static final String LIST_DEFAULT = "taskId,state,trace,workerId,connector";

        private final String value;

        private Fields(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return value;
        }

        static final Comparator<ConnectorTask> ID_COMPARATOR = nullable(ConnectorTask::getId);

        static final Map<String, Map<Boolean, Comparator<ConnectorTask>>> COMPARATORS =
                ComparatorBuilder.bidirectional(Map.of("id", ID_COMPARATOR));

        public static final ComparatorBuilder<ConnectorTask> COMPARATOR_BUILDER =
                new ComparatorBuilder<>(ConnectorTask.Fields::comparator, ConnectorTask.Fields.defaultComparator());

        public static Comparator<ConnectorTask> defaultComparator() {
            return ID_COMPARATOR;
        }

        public static Comparator<ConnectorTask> comparator(String fieldName, boolean descending) {
            return COMPARATORS.getOrDefault(fieldName, Collections.emptyMap()).get(descending);
        }
    }

    @Schema(name = "KafkaConnectorTaskDataList")
    public static final class DataList extends JsonApiRootDataList<ConnectorTask> {
        public DataList(List<ConnectorTask> data, ListRequestContext<ConnectorTask> listSupport) {
            super(data.stream()
                    .map(entry -> {
                        entry.addMeta("page", listSupport.buildPageMeta(entry::toCursor));
                        return entry;
                    })
                    .toList());
            addMeta("page", listSupport.buildPageMeta());
            listSupport.buildPageLinks(ConnectorTask::toCursor).forEach(this::addLink);

            if (listSupport.getFetchParams().includes(Fields.CONNECTOR.value)) {
                data.stream()
                    .map(c -> c.relationships.connectorResource)
                    .filter(Objects::nonNull)
                    .forEach(this::addIncluded);
            }
        }
    }

    @Schema(name = "KafkaConnectorTaskData")
    public static final class Data extends JsonApiRootData<ConnectorTask> {
        @JsonCreator
        public Data(@JsonProperty("data") ConnectorTask data) {
            super(data);
        }
    }

    @Schema(name = "KafkaConnectorTaskMeta", additionalProperties = Object.class)
    @JsonInclude(value = Include.NON_NULL)
    static final class Meta extends JsonApiMeta {
    }

    @JsonFilter(FIELDS_PARAM)
    static class Attributes {
        @JsonProperty
        Integer taskId;

        @JsonProperty
        Map<String, String> config;

        @JsonProperty
        String state;

        @JsonProperty
        String trace;

        @JsonProperty
        String workerId;

        @JsonProperty
        Metrics metrics;
    }

    @JsonFilter(FIELDS_PARAM)
    static class Relationships {
        @JsonProperty
        JsonApiRelationshipToOne connector;

        @JsonIgnore
        Connector connectorResource;
    }

    public ConnectorTask(String id) {
        super(id, API_TYPE, new Attributes(), new Relationships());
    }

    @JsonCreator
    public ConnectorTask(String id, String type, Meta meta, Attributes attributes, Relationships relationships) {
        super(id, type, meta, attributes, relationships);
    }

    public static ConnectorTask fromId(String id) {
        return new ConnectorTask(id);
    }

    /**
     * Constructs a "cursor" ConnectorTask from the encoded string representation of the subset
     * of fields used to compare entities for pagination/sorting.
     */
    public static ConnectorTask fromCursor(JsonObject cursor) {
        if (cursor == null) {
            return null;
        }

        return new ConnectorTask(cursor.getString("id"));
    }

    public String toCursor(List<String> sortFields) {
        JsonObjectBuilder cursor = Json.createObjectBuilder()
                .add("id", id);
        JsonObjectBuilder attrBuilder = Json.createObjectBuilder();
        cursor.add("attributes", attrBuilder.build());

        return Base64.getUrlEncoder().encodeToString(cursor.build().toString().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public JsonApiMeta metaFactory() {
        return new Meta();
    }

    public void setId(String id) {
        this.id = id;
    }

    public Metrics metrics() {
        return attributes.metrics;
    }

    public void metrics(Metrics metrics) {
        attributes.metrics = metrics;
    }

    public Integer taskId() {
        return attributes.taskId;
    }

    public void taskId(Integer taskId) {
        attributes.taskId = taskId;
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

    public ConnectorTask connector(Connector connector, boolean include) {
        if (include) {
            relationships.connectorResource = connector;
        }
        var relationship = new JsonApiRelationshipToOne(connector.identifier());
        relationships.connector = relationship;
        return this;
    }
}
