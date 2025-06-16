package com.github.streamshub.console.api.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionInfo;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.streamshub.console.api.model.jsonapi.JsonApiError;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(value = Include.NON_NULL)
public class PartitionInfo {

    final int partition;
    final List<PartitionReplica> replicas;
    final Integer leaderId;

    @Schema(implementation = Object.class, oneOf = { OffsetInfo.class, JsonApiError.class })
    private static final class OffsetInfoOrError {
    }

    @Schema(additionalProperties = OffsetInfoOrError.class)
    Map<String, Either<OffsetInfo, JsonApiError>> offsets;

    public PartitionInfo(int partition, List<PartitionReplica> replicas, Integer leaderId) {
        super();
        this.partition = partition;
        this.leaderId = leaderId;
        this.replicas = replicas;
    }

    public static PartitionInfo fromKafkaModel(TopicPartitionInfo info) {
        Integer leaderId = Optional.ofNullable(info.leader()).map(Node::id).orElse(null);
        List<Integer> isr = info.isr().stream().map(Node::id).toList();
        List<PartitionReplica> replicas = info.replicas()
                .stream()
                .map(replica -> PartitionReplica.fromKafkaModel(replica, isr))
                .toList();
        return new PartitionInfo(info.partition(), replicas, leaderId);
    }

    static <P> Either<P, JsonApiError> primaryOrError(Either<P, Throwable> either, String message) {
        return either.ifPrimaryOrElse(
                Either::of,
                thrown -> JsonApiError.forThrowable(thrown, message));
    }

    public void addOffset(String key, Either<OffsetInfo, Throwable> offset) {
        if (this.offsets == null) {
            this.offsets = new LinkedHashMap<>(4);
        }

        this.offsets.put(key, primaryOrError(offset, "Unable to fetch partition offset"));
    }

    public void setReplicaLocalStorage(int nodeId, Either<ReplicaLocalStorage, Throwable> storage) {
        getReplica(nodeId).ifPresent(replica ->
            replica.localStorage(primaryOrError(storage, "Unable to fetch replica log metadata")));
    }

    public int getPartition() {
        return partition;
    }

    public Integer getLeaderId() {
        return leaderId;
    }

    public List<PartitionReplica> getReplicas() {
        return replicas;
    }

    public Map<String, Either<OffsetInfo, JsonApiError>> getOffsets() {
        return offsets;
    }

    public boolean online() {
        return leaderId != null;
    }

    @JsonProperty
    public String status() {
        if (!online()) {
            return "Offline";
        }

        return replicas.stream()
            .filter(Predicate.not(PartitionReplica::inSync))
            .findFirst()
            .map(ignored -> "UnderReplicated")
            .orElse("FullyReplicated");
    }

    @JsonProperty
    @Schema(readOnly = true, description = """
            The total size of the log segments local to the leader replica, in bytes.
            Or null if this is unavailable for any reason.
            """)
    public Long leaderLocalStorage() {
        return getReplica(leaderId)
            .map(PartitionReplica::localStorage)
            .filter(Objects::nonNull)
            .filter(Either::isPrimaryPresent)
            .map(Either::getPrimary)
            .map(ReplicaLocalStorage::size)
            .orElse(null);
    }

    Optional<Long> getOffset(String key) {
        return Optional.ofNullable(offsets)
            .map(o -> o.get(key))
            .flatMap(Either::getOptionalPrimary)
            .map(OffsetInfo::offset);
    }

    Optional<PartitionReplica> getReplica(Integer nodeId) {
        if (nodeId == null) {
            return Optional.empty();
        }

        return replicas.stream()
            .filter(replica -> replica.nodeId() == nodeId)
            .findFirst();
    }
}
