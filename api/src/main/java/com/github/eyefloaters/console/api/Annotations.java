package com.github.eyefloaters.console.api;

public enum Annotations {

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

    @Override
    public String toString() {
        return value();
    }

    public String value() {
        return value;
    }
}
