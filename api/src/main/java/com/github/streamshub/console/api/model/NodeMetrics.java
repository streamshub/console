package com.github.streamshub.console.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.streamshub.console.api.model.jsonapi.JsonApiResource;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRootData;
import com.github.streamshub.console.api.model.jsonapi.None;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "NodeMetrics")
public class NodeMetrics {

    public static class MetricsResponse extends JsonApiRootData<MetricsResource> {

        public MetricsResponse(String nodeId, Metrics metrics) {
            super(new MetricsResource(nodeId, new Attributes(metrics)));
        }
    }

    public static final class MetricsResource extends JsonApiResource<Attributes, None> {

        public MetricsResource(String nodeId, Attributes attributes) {
            super(nodeId, "nodeMetrics", attributes);
        }
    }

    @Schema(name = "NodeMetricsAttributes")
    public static record Attributes(
        @JsonProperty Metrics metrics
    ) { }
}
