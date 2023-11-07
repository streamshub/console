package com.github.eyefloaters.console.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
public record OffsetAndMetadata(
        String topicId,
        String topicName,
        int partition,
        long offset,
        long lag,
        String metadata,
        Integer leaderEpoch
) {
}
