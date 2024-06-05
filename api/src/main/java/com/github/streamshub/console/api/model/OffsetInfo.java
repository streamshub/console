package com.github.streamshub.console.api.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
public record OffsetInfo(long offset, Instant timestamp, Integer leaderEpoch) {

}
