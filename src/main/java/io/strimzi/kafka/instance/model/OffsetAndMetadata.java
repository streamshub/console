package io.strimzi.kafka.instance.model;

public class OffsetAndMetadata {

    private long offset;
    private String metadata;
    private Integer leaderEpoch;

    public OffsetAndMetadata() {
    }

    public OffsetAndMetadata(long offset, String metadata, Integer leaderEpoch) {
        super();
        this.offset = offset;
        this.metadata = metadata;
        this.leaderEpoch = leaderEpoch;
    }

    public long getOffset() {
        return offset;
    }

    public void setOffset(long offset) {
        this.offset = offset;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Integer getLeaderEpoch() {
        return leaderEpoch;
    }

    public void setLeaderEpoch(Integer leaderEpoch) {
        this.leaderEpoch = leaderEpoch;
    }

}
