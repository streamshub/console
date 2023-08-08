package com.github.eyefloaters.console.api.model;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
public class OffsetInfo {

    String kind = "Offset";
    private long offset;
    private Instant timestamp;
    private Integer leaderEpoch;

    public OffsetInfo() {
    }

    public OffsetInfo(long offset, Instant timestamp, Integer leaderEpoch) {
        super();
        this.offset = offset;
        this.timestamp = timestamp;
        this.leaderEpoch = leaderEpoch;
    }

    public String getKind() {
        return kind;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getLeaderEpoch() {
        return leaderEpoch;
    }

    public void setLeaderEpoch(Integer leaderEpoch) {
        this.leaderEpoch = leaderEpoch;
    }

}
