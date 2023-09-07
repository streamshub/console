package com.github.eyefloaters.console.api.model;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Schema(name = "KafkaRecordAttributes")
@JsonFilter("fieldFilter")
public class KafkaRecord {

    public static final class Fields {
        public static final String PARTITION = "partition";
        public static final String OFFSET = "offset";
        public static final String TIMESTAMP = "timestamp";
        public static final String TIMESTAMP_TYPE = "timestampType";
        public static final String HEADERS = "headers";
        public static final String KEY = "key";
        public static final String VALUE = "value";

        public static final String DEFAULT =
                    PARTITION +
                    ", " + OFFSET +
                    ", " + TIMESTAMP +
                    ", " + TIMESTAMP_TYPE +
                    ", " + HEADERS +
                    ", " + KEY +
                    ", " + VALUE;

        private Fields() {
            // Prevent instances
        }
    }

    @Schema(name = "KafkaRecordListResponse")
    public static final class ListResponse extends DataListResponse<RecordResource> {
        public ListResponse(List<KafkaRecord> data) {
            super(data.stream().map(RecordResource::new).toList());
        }
    }

    @Schema(name = "KafkaRecordResponse")
    public static final class SingleResponse extends DataResponse<RecordResource> {
        public SingleResponse(KafkaRecord data) {
            super(new RecordResource(data));
        }
    }

    @Schema(name = "KafkaRecord")
    public static final class RecordResource extends Resource<KafkaRecord> {
        public RecordResource(KafkaRecord data) {
            super(null, "records", data);
        }
    }

    @JsonIgnore
    String topic;

    @Schema(description = "The record's partition within the topic")
    Integer partition;

    @Schema(readOnly = true, description = "The record's offset within the topic partition")
    Long offset;

    @Schema(description = "Timestamp associated with the record. The type is indicated by `timestampType`. When producing a record, this value will be used as the record's `CREATE_TIME`.", format = "date-time")
    Instant timestamp;

    @Schema(readOnly = true, description = "Type of timestamp associated with the record")
    String timestampType;

    @Schema(description = "Record headers, key/value pairs")
    Map<String, String> headers;

    @Schema(description = "Record key")
    String key;

    @NotNull
    @Schema(description = "Record value")
    String value;

    public KafkaRecord() {
        super();
    }

    public KafkaRecord(String topic) {
        this.topic = topic;
    }

    public KafkaRecord(String topic, Integer partition, Instant timestamp, Map<String, String> headers, String key, String value) {
        this(topic);
        this.partition = partition;
        this.timestamp = timestamp;
        this.headers = headers;
        this.key = key;
        this.value = value;
    }

    @JsonIgnore
    public URI buildUri(UriBuilder builder, String topicName) {
        builder.queryParam(Fields.PARTITION, partition);
        builder.queryParam(Fields.OFFSET, offset);
        return builder.build(topicName);
    }

    @AssertTrue(message = "invalid timestamp")
    @JsonIgnore
    public boolean isTimestampValid() {
        if (timestamp == null) {
            return true;
        }

        try {
            return timestamp.isAfter(Instant.ofEpochMilli(-1));
        } catch (Exception e) {
            return false;
        }
    }

    public Integer getPartition() {
        return partition;
    }

    public void setPartition(Integer partition) {
        this.partition = partition;
    }

    public Long getOffset() {
        return offset;
    }

    public void setOffset(Long offset) {
        this.offset = offset;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public String getTimestampType() {
        return timestampType;
    }

    public void setTimestampType(String timestampType) {
        this.timestampType = timestampType;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
