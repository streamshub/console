package com.github.streamshub.console.api.model;

import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonValue;

@JsonInclude(value = Include.NON_NULL)
public record Node(int id, String host, Integer port, String rack, String nodePool, Set<Role> roles, MetadataState metadataState) {

    public enum Role {
        CONTROLLER,
        BROKER;

        @Override
        @JsonValue
        public String toString() {
            return name().toLowerCase(Locale.ENGLISH);
        }
    }

    public enum MetadataStatus {
        LEADER,
        FOLLOWER,
        OBSERVER;

        @Override
        @JsonValue
        public String toString() {
            return name().toLowerCase(Locale.ENGLISH);
        }
    }

    public static record MetadataState(
        MetadataStatus status,
        long logEndOffset,
        long lag
    ) {
    }

    public static Node fromKafkaModel(org.apache.kafka.common.Node node, String nodePool, Set<Role> roles, MetadataState metadataState) {
        return new Node(node.id(), node.host(), node.port(), node.rack(), nodePool, roles, metadataState);
    }

    public static Node fromMetadataState(int id, String nodePool, Set<Role> roles, MetadataState metadataState) {
        return new Node(id, null, null, null, nodePool, roles, metadataState);
    }

    public static Node fromKafkaModel(org.apache.kafka.common.Node node) {
        return fromKafkaModel(node, null, null, null);
    }

}
