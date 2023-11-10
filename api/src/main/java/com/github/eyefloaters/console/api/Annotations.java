package com.github.eyefloaters.console.api;

public enum Annotations {

    /**
     * Annotation to identify a listener in Strimzi Kafka resources to be used for
     * connections directly from the Console API.
     */
    CONSOLE_LISTENER("console-listener"),

    /**
     * Annotation to identify a listener in Strimzi Kafka resources to be used for
     * public connections. This may be used to differentiate a listener to be
     * exposed via the KafkaCluster resource and published in the UI.
     */
    EXPOSED_LISTENER("exposed-listener");

    private static final String NAMESPACE = "eyefloaters.github.com";
    private final String value;

    private Annotations(String name) {
        this.value = NAMESPACE + '/' + name;
    }

    public String value() {
        return value;
    }
}
