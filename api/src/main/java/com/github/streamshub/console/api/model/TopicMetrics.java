package com.github.streamshub.console.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.streamshub.console.api.model.jsonapi.JsonApiResource;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRootData;
import com.github.streamshub.console.api.model.jsonapi.None;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "TopicMetrics")
public class TopicMetrics {

    public static class MetricsResponse extends JsonApiRootData<MetricsResource> {

        public MetricsResponse(String topicId, Metrics metrics) {
            super(new MetricsResource(topicId, new Attributes(metrics)));
        }
    }

    public static final class MetricsResource extends JsonApiResource<Attributes, None> {

        public MetricsResource(String topicId, Attributes attributes) {
            // "topicMetrics" serves as the JSON:API 'type' field
            super(topicId, "topicMetrics", attributes);
        }
    }

    @Schema(name = "TopicMetricsAttributes")
    public static record Attributes(
        @JsonProperty Metrics metrics
    ) { }
}
