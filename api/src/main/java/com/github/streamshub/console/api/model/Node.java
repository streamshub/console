package com.github.streamshub.console.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
public record Node(int id, String host, int port, String rack) {

    public static Node fromKafkaModel(org.apache.kafka.common.Node node) {
        return new Node(node.id(), node.host(), node.port(), node.rack());
    }

}
