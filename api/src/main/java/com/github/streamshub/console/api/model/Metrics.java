package com.github.streamshub.console.api.model;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

public record Metrics(
        @JsonProperty
        Map<String, List<Metrics.ValueMetric>> values,

        @JsonProperty
        Map<String, List<Metrics.RangeMetric>> ranges) {

    public Metrics() {
        this(new LinkedHashMap<>(), new LinkedHashMap<>());
    }

    public interface Metric {
        Map<String, String> attributes();
    }

    @Schema(additionalProperties = String.class)
    public static record ValueMetric(
            @JsonProperty
            String value,

            @JsonAnyGetter
            @Schema(hidden = true)
            Map<String, String> attributes) implements Metric {
    }

    @Schema(additionalProperties = String.class)
    public static record RangeMetric(
            @JsonProperty
            @Schema(implementation = String[][].class)
            List<RangeEntry> range,

            @JsonAnyGetter
            @Schema(hidden = true)
            Map<String, String> attributes) implements Metric {
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({"when", "value"})
    public static record RangeEntry(Instant when, String value) {
    }
}
