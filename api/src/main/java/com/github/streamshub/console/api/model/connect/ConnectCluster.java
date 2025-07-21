package com.github.streamshub.console.api.model.connect;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

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
import com.github.streamshub.console.api.model.KafkaCluster;
import com.github.streamshub.console.api.model.Metrics;
import com.github.streamshub.console.api.model.jsonapi.Identifier;
import com.github.streamshub.console.api.model.jsonapi.JsonApiMeta;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRelationshipToMany;
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
    name = "KafkaConnectCluster",
    properties = {
        @SchemaProperty(name = "type", enumeration = ConnectCluster.API_TYPE),
        @SchemaProperty(name = "meta", implementation = ConnectCluster.Meta.class)
    })
@Expression(
    value = "self.id != null",
    message = "resource ID is required",
    node = "id",
    payload = ErrorCategory.InvalidResource.class)
@Expression(
    when = "self.type != null",
    value = "self.type == '" + ConnectCluster.API_TYPE + "'",
    message = "resource type conflicts with operation",
    node = "type",
    payload = ErrorCategory.ResourceConflict.class)
public class ConnectCluster extends KubeApiResource<ConnectCluster.Attributes, ConnectCluster.Relationships> implements PaginatedKubeResource {

    public static final String API_TYPE = "connects";
    public static final String FIELDS_PARAM = "fields[" + API_TYPE + "]";

    @Schema(hidden = true)
    public enum Fields {
        NAME("name"),
        NAMESPACE("namespace"),
        CREATION_TIMESTAMP("creationTimestamp"),
        COMMIT("commit"),
        KAFKA_CLUSTER_ID("kafkaClusterId"),
        VERSION("version"),
        PLUGINS("plugins"),
        KAFKA_CLUSTERS("kafkaClusters"),
        CONNECTORS("connectors");

        public static final String LIST_DEFAULT = "name,namespace,creationTimestamp,commit,kafkaClusterId,version,kafkaClusters";

        private final String value;

        private Fields(String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return value;
        }

        static final Comparator<ConnectCluster> ID_COMPARATOR = nullable(ConnectCluster::getId);

        static final Map<String, Map<Boolean, Comparator<ConnectCluster>>> COMPARATORS =
                ComparatorBuilder.bidirectional(
                        Map.of("id", ID_COMPARATOR,
                                NAME.value, comparing(ConnectCluster::name),
                                NAMESPACE.value, nullable(ConnectCluster::namespace),
                                CREATION_TIMESTAMP.value, nullable(ConnectCluster::creationTimestamp),
                                VERSION.value, nullable(ConnectCluster::version)));

        public static final ComparatorBuilder<ConnectCluster> COMPARATOR_BUILDER =
                new ComparatorBuilder<>(ConnectCluster.Fields::comparator, ConnectCluster.Fields.defaultComparator());

        public static Comparator<ConnectCluster> defaultComparator() {
            return ID_COMPARATOR;
        }

        public static Comparator<ConnectCluster> comparator(String fieldName, boolean descending) {
            return COMPARATORS.getOrDefault(fieldName, Collections.emptyMap()).get(descending);
        }
    }

    @Schema(name = "KafkaConnectClusterDataList")
    public static final class DataList extends JsonApiRootDataList<ConnectCluster> {
        public DataList(List<ConnectCluster> data, ListRequestContext<ConnectCluster> listSupport) {
            super(data.stream()
                    .map(entry -> {
                        entry.addMeta("page", listSupport.buildPageMeta(entry::toCursor));
                        return entry;
                    })
                    .toList());

            addMeta("page", listSupport.buildPageMeta());
            listSupport.buildPageLinks(ConnectCluster::toCursor).forEach(this::addLink);

            if (listSupport.getFetchParams().includes(Fields.CONNECTORS.value)) {
                data.stream()
                    .map(c -> c.relationships.connectorResources)
                    .filter(Objects::nonNull)
                    .flatMap(Collection::stream)
                    .forEach(connector -> {
                        addIncluded(connector);

                        if (listSupport.getFetchParams().includes(Connector.Fields.TASKS.toString())) {
                            Optional.ofNullable(connector.getRelationships().taskResources)
                                .map(Collection::stream)
                                .orElseGet(Stream::empty)
                                .forEach(this::addIncluded);
                        }
                    });
            }
        }
    }

    @Schema(name = "KafkaConnectClusterData")
    public static final class Data extends JsonApiRootData<ConnectCluster> {
        @JsonCreator
        public Data(@JsonProperty("data") ConnectCluster data) {
            super(data);
        }
    }

    @Schema(name = "KafkaConnectClusterMeta", additionalProperties = Object.class)
    @JsonInclude(value = Include.NON_NULL)
    static final class Meta extends JsonApiMeta {
    }

    @JsonFilter(FIELDS_PARAM)
    static class Attributes extends KubeAttributes {
        @JsonProperty
        String commit;

        @JsonProperty
        String kafkaClusterId;

        @JsonProperty
        String version;

        @JsonProperty
        List<ConnectorPlugin> plugins;

        @JsonProperty
        Metrics metrics;
    }

    @JsonFilter(FIELDS_PARAM)
    static class Relationships {
        @JsonProperty
        JsonApiRelationshipToMany kafkaClusters;

        @JsonProperty
        JsonApiRelationshipToMany connectors;

        @JsonIgnore
        List<Connector> connectorResources = Collections.emptyList();

    }

    public ConnectCluster(String id) {
        super(id, API_TYPE, new Attributes(), new Relationships());
    }

    public static ConnectCluster fromId(String id) {
        return new ConnectCluster(id);
    }

    /**
     * Constructs a "cursor" Topic from the encoded string representation of the subset
     * of Topic fields used to compare entities for pagination/sorting.
     */
    public static ConnectCluster fromCursor(JsonObject cursor) {
        return PaginatedKubeResource.fromCursor(cursor, ConnectCluster::fromId);
    }

    @Override
    public JsonApiMeta metaFactory() {
        return new Meta();
    }

    public void commit(String commit) {
        attributes.commit = commit;
    }

    public void kafkaClusterId(String kafkaClusterId) {
        attributes.kafkaClusterId = kafkaClusterId;
    }

    public String version() {
        return attributes.version;
    }

    public void version(String version) {
        attributes.version = version;
    }

    public ConnectCluster plugins(List<ConnectorPlugin> plugins) {
        attributes.plugins = plugins;
        return this;
    }

    public void kafkaClusters(List<String> clusterIds) {
        relationships.kafkaClusters = new JsonApiRelationshipToMany(clusterIds.stream()
                .map(id -> new Identifier(KafkaCluster.API_TYPE, id))
                .toList());
    }

    public ConnectCluster connectors(List<Connector> connectors, boolean include) {
        if (include) {
            relationships.connectorResources = connectors;
        }
        relationships.connectors = new JsonApiRelationshipToMany(connectors.stream()
                .map(JsonApiResource::identifier)
                .toList());
        return this;
    }

    public Metrics metrics() {
        return attributes.metrics;
    }

    public void metrics(Metrics metrics) {
        attributes.metrics = metrics;
    }
}
