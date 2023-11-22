package com.github.eyefloaters.console.api.model;

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

    public static record ValueMetric(
            @JsonProperty
            String value,

            @JsonAnyGetter
            Map<String, String> attributes) {
    }

    public static record RangeMetric(
            @JsonProperty
            List<RangeEntry> range,

            @JsonAnyGetter
            Map<String, String> attributes) {
    }

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({"when", "value"})
    @Schema(implementation = String[].class)
    public static record RangeEntry(Instant when, String value) {
    }
}
