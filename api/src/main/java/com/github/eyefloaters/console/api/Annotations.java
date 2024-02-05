package com.github.eyefloaters.console.api;

public enum Annotations {

    /**
     * Annotation that may be placed on a Strimzi Kafka resource to be hidden
     * (ignored) by the console API server. Resources with this annotation will
     * not appear in the Kafka cluster listing and attempts to fetch information
     * using the describeCluster operation will result in a 404 client error.
     */
    CONSOLE_HIDDEN("console-hidden"),

    /**
     * Annotation to identify a listener in Strimzi Kafka resources to be used for
     * connections directly from the Console API.
     */
    CONSOLE_LISTENER("console-listener");

    private static final String NAMESPACE = "eyefloaters.github.com";
    private final String value;

    private Annotations(String name) {
        this.value = NAMESPACE + '/' + name;
    }

    public String value() {
        return value;
    }
}
