package com.github.streamshub.console.api.model;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

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
import com.github.streamshub.console.api.model.kubernetes.KubeApiResource;
import com.github.streamshub.console.api.model.kubernetes.KubeAttributes;
import com.github.streamshub.console.api.model.kubernetes.PaginatedKubeResource;
import com.github.streamshub.console.api.support.ComparatorBuilder;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.ListRequestContext;

import io.xlate.validation.constraints.Expression;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;

@Schema(
    name = "KafkaUser",
    properties = {
        @SchemaProperty(name = "type", enumeration = KafkaUser.API_TYPE),
        @SchemaProperty(name = "meta", implementation = KafkaUser.Meta.class)
    })
@Expression(
    value = "self.id != null",
    message = "resource ID is required",
    node = "id",
    payload = ErrorCategory.InvalidResource.class)
@Expression(
    when = "self.type != null",
    value = "self.type == '" + KafkaUser.API_TYPE + "'",
    message = "resource type conflicts with operation",
    node = "type",
    payload = ErrorCategory.ResourceConflict.class)
public class KafkaUser extends KubeApiResource<KafkaUser.Attributes, KafkaUser.Relationships> implements PaginatedKubeResource {

    public static final String API_TYPE = "kafkaUsers";
    public static final String FIELDS_PARAM = "fields[" + API_TYPE + "]";

    public static class Fields {
        public static final String NAME = "name";
        public static final String NAMESPACE = "namespace";
        public static final String CREATION_TIMESTAMP = "creationTimestamp";

        static final Comparator<KafkaUser> ID_COMPARATOR =
                comparing(KafkaUser::getId, nullsLast(String::compareTo));

        static final Map<String, Map<Boolean, Comparator<KafkaUser>>> COMPARATORS =
                ComparatorBuilder.bidirectional(
                        Map.of("id", ID_COMPARATOR,
                                NAME, comparing(KafkaUser::name),
                                NAMESPACE, comparing(KafkaUser::namespace),
                                CREATION_TIMESTAMP, comparing(KafkaUser::creationTimestamp)));

        public static final ComparatorBuilder<KafkaUser> COMPARATOR_BUILDER =
                new ComparatorBuilder<>(KafkaUser.Fields::comparator, KafkaUser.Fields.defaultComparator());

        public static final String LIST_DEFAULT =
                NAME + ", "
                + NAMESPACE + ", "
                + CREATION_TIMESTAMP;

        public static final String DESCRIBE_DEFAULT =
                NAME + ", "
                + NAMESPACE + ", "
                + CREATION_TIMESTAMP;

        private Fields() {
            // Prevent instances
        }

        public static Comparator<KafkaUser> defaultComparator() {
            return ID_COMPARATOR;
        }

        public static Comparator<KafkaUser> comparator(String fieldName, boolean descending) {
            return COMPARATORS.getOrDefault(fieldName, Collections.emptyMap()).get(descending);
        }
    }

    @Schema(name = "KafkaUserDataList")
    public static final class DataList extends JsonApiRootDataList<KafkaUser> {
        public DataList(List<KafkaUser> data, ListRequestContext<KafkaUser> listSupport) {
            super(data.stream()
                    .map(entry -> {
                        entry.addMeta("page", listSupport.buildPageMeta(entry::toCursor));
                        return entry;
                    })
                    .toList());
            addMeta("page", listSupport.buildPageMeta());
            listSupport.meta().forEach(this::addMeta);
            listSupport.buildPageLinks(KafkaUser::toCursor).forEach(this::addLink);
        }
    }

    @Schema(name = "KafkaUserData")
    public static final class Data extends JsonApiRootData<KafkaUser> {
        @JsonCreator
        public Data(@JsonProperty("data") KafkaUser data) {
            super(data);
        }
    }

    @Schema(name = "KafkaUserMeta", additionalProperties = Object.class)
    @JsonInclude(value = Include.NON_NULL)
    static final class Meta extends JsonApiMeta {
    }

    @JsonFilter(FIELDS_PARAM)
    public static class Attributes extends KubeAttributes {

    }

    @JsonFilter(FIELDS_PARAM)
    static class Relationships {
    }

    public KafkaUser(String id) {
        super(id, API_TYPE, new Attributes(), new Relationships());
    }

    @JsonCreator
    public KafkaUser(String id, String type, Meta meta, Attributes attributes, Relationships relationships) {
        super(id, type, meta, attributes, relationships);
    }

    public static KafkaUser fromId(String id) {
        return new KafkaUser(id);
    }

    /**
     * Constructs a "cursor" Topic from the encoded string representation of the subset
     * of Topic fields used to compare entities for pagination/sorting.
     */
    public static KafkaUser fromCursor(JsonObject cursor) {
        return PaginatedKubeResource.fromCursor(cursor, KafkaUser::fromId);
    }

    @Override
    public JsonApiMeta metaFactory() {
        return new Meta();
    }
}
