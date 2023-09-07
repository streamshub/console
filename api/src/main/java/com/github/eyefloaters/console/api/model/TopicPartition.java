package com.github.eyefloaters.console.api.model;

public class TopicPartition {

    private String topic;
    private int partition;

    public TopicPartition() {
    }

    public TopicPartition(String topic, int partition) {
        super();
        this.topic = topic;
        this.partition = partition;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getPartition() {
        return partition;
    }

    public void setPartition(int partition) {
        this.partition = partition;
    }

}
