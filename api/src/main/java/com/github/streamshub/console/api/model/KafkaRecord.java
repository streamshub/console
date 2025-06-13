package com.github.streamshub.console.api.model;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRelationshipToOne;
import com.github.streamshub.console.api.model.jsonapi.JsonApiResource;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRootData;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRootDataList;
import com.github.streamshub.console.api.support.ErrorCategory;

import io.xlate.validation.constraints.Expression;

@Schema(name = "KafkaRecord")
@Expression(
    when = "self.type != null",
    value = "self.type == '" + KafkaRecord.API_TYPE + "'",
    message = "resource type conflicts with operation",
    node = "type",
    payload = ErrorCategory.ResourceConflict.class)
public class KafkaRecord extends JsonApiResource<KafkaRecord.Attributes, KafkaRecord.Relationships> {

    public static final String API_TYPE = "records";
    public static final String FIELDS_PARAM = "fields[" + API_TYPE + "]";

    public static final class Fields {
        public static final String PARTITION = "partition";
        public static final String OFFSET = "offset";
        public static final String TIMESTAMP = "timestamp";
        public static final String TIMESTAMP_TYPE = "timestampType";
        public static final String HEADERS = "headers";
        public static final String KEY = "key";
        public static final String VALUE = "value";
        public static final String SIZE = "size";
        public static final String KEY_SCHEMA = "keySchema";
        public static final String VALUE_SCHEMA = "valueSchema";

        public static final String DEFAULT =
                    PARTITION +
                    ", " + OFFSET +
                    ", " + TIMESTAMP +
                    ", " + TIMESTAMP_TYPE +
                    ", " + HEADERS +
                    ", " + KEY +
                    ", " + VALUE +
                    ", " + SIZE +
                    ", " + KEY_SCHEMA +
                    ", " + VALUE_SCHEMA;

        public static final List<String> ALL = List.of(
                PARTITION,
                OFFSET,
                TIMESTAMP,
                TIMESTAMP_TYPE,
                HEADERS,
                KEY,
                VALUE,
                SIZE,
                KEY_SCHEMA,
                VALUE_SCHEMA);

        private Fields() {
            // Prevent instances
        }
    }

    @Schema(name = "KafkaRecordDataList")
    public static final class KafkaRecordDataList extends JsonApiRootDataList<KafkaRecord> {
        public KafkaRecordDataList(List<KafkaRecord> data) {
            super(data);
        }
    }

    @Schema(name = "KafkaRecordData")
    public static final class KafkaRecordData extends JsonApiRootData<KafkaRecord> {
        @JsonCreator
        public KafkaRecordData(@JsonProperty("data") KafkaRecord data) {
            super(data);
        }
    }

    @JsonFilter("fieldFilter")
    @Schema(name = "KafkaRecordAttributes")
    static class Attributes {
        @JsonIgnore
        String topic;

        @JsonProperty
        @Schema(description = "The record's partition within the topic")
        Integer partition;

        @JsonProperty
        @Schema(readOnly = true, description = "The record's offset within the topic partition")
        Long offset;

        @JsonProperty
        @Schema(description = "Timestamp associated with the record. The type is indicated by `timestampType`. When producing a record, this value will be used as the record's `CREATE_TIME`.", format = "date-time")
        Instant timestamp;

        @JsonProperty
        @Schema(readOnly = true, description = "Type of timestamp associated with the record")
        String timestampType;

        @JsonProperty
        @Schema(description = "Record headers, key/value pairs")
        Map<String, String> headers;

        @JsonProperty
        @Schema(description = "Record key")
        String key;

        @JsonProperty
        @NotNull
        @Schema(description = "Record value")
        String value;

        @JsonProperty
        @Schema(readOnly = true, description = "Size of the uncompressed record, not including the overhead of the record in the log segment.")
        Long size;
    }

    @JsonFilter("fieldFilter")
    static class Relationships {
        @JsonProperty
        JsonApiRelationshipToOne keySchema;

        @JsonProperty
        JsonApiRelationshipToOne valueSchema;
    }

    @JsonCreator
    public KafkaRecord() {
        super(null, API_TYPE, new Attributes(), new Relationships());
    }

    public KafkaRecord(String topic) {
        this();
        attributes.topic = topic;
    }

    public KafkaRecord(String topic, Integer partition, Instant timestamp, Map<String, String> headers, String key, String value, Long size) {
        this(topic);
        attributes.partition = partition;
        attributes.timestamp = timestamp;
        attributes.headers = headers;
        attributes.key = key;
        attributes.value = value;
        attributes.size = size;
    }

    @JsonIgnore
    public URI buildUri(UriBuilder builder, String topicName) {
        builder.queryParam(Fields.PARTITION, attributes.partition);
        builder.queryParam(Fields.OFFSET, attributes.offset);
        return builder.build(topicName);
    }

    @AssertTrue(message = "invalid timestamp")
    @JsonIgnore
    public boolean isTimestampValid() {
        if (attributes.timestamp == null) {
            return true;
        }

        try {
            return attributes.timestamp.isAfter(Instant.ofEpochMilli(-1));
        } catch (Exception e) {
            return false;
        }
    }

    public Integer partition() {
        return attributes.partition;
    }

    public void partition(Integer partition) {
        attributes.partition = partition;
    }

    public Long offset() {
        return attributes.offset;
    }

    public void offset(Long offset) {
        attributes.offset = offset;
    }

    public Instant timestamp() {
        return attributes.timestamp;
    }

    public void timestamp(Instant timestamp) {
        attributes.timestamp = timestamp;
    }

    public String timestampType() {
        return attributes.timestampType;
    }

    public void timestampType(String timestampType) {
        attributes.timestampType = timestampType;
    }

    public Map<String, String> headers() {
        return attributes.headers;
    }

    public void headers(Map<String, String> headers) {
        attributes.headers = headers;
    }

    public String key() {
        return attributes.key;
    }

    public void key(String key) {
        attributes.key = key;
    }

    public String value() {
        return attributes.value;
    }

    public void value(String value) {
        attributes.value = value;
    }

    public Long size() {
        return attributes.size;
    }

    public void size(Long size) {
        attributes.size = size;
    }

    public JsonApiRelationshipToOne keySchema() {
        return relationships.keySchema;
    }

    public void keySchema(JsonApiRelationshipToOne keySchema) {
        relationships.keySchema = keySchema;
    }

    public JsonApiRelationshipToOne valueSchema() {
        return relationships.valueSchema;
    }

    public void valueSchema(JsonApiRelationshipToOne valueSchema) {
        relationships.valueSchema = valueSchema;
    }
}
