package com.github.eyefloaters.console.api.model;

public class Node {

    private int id;
    private String host;
    private int port;
    private String rack;

    public Node() {
    }

    public Node(int id, String host, int port, String rack) {
        super();
        this.id = id;
        this.host = host;
        this.port = port;
        this.rack = rack;
    }

    public static Node fromKafkaModel(org.apache.kafka.common.Node node) {
        return new Node(node.id(), node.host(), node.port(), node.rack());
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getRack() {
        return rack;
    }

    public void setRack(String rack) {
        this.rack = rack;
    }

}
