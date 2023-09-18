package com.github.eyefloaters.console.api.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
public class Node {

    private final int id;
    private final String host;
    private final int port;
    private final String rack;
    private Either<ReplicaInfo, Error> log;

    public Node(int id, String host, int port, String rack) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.rack = rack;
        this.log = null;
    }

    public static Node fromKafkaModel(org.apache.kafka.common.Node node) {
        return new Node(node.id(), node.host(), node.port(), node.rack());
    }

    public int getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getRack() {
        return rack;
    }

    public Either<ReplicaInfo, Error> getLog() {
        return log;
    }

    public void setLog(Either<ReplicaInfo, Error> log) {
        this.log = log;
    }
}
