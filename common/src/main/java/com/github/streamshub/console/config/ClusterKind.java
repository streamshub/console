package com.github.streamshub.console.config;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum ClusterKind {
  STRIMI_KAFKA("kafkas.kafka.strimzi.io"),
  KROXYLICIOUS_VIRTUAL_KAFKA_CLUSTER("virtualkafkaclusters.kroxylicious.io");

  private final String value;

  private ClusterKind(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static ClusterKind forValue(String value) {
    for (var kind : values()) {
      if (kind.value.equals(value)) {
        return kind;
      }
    }
    throw new IllegalArgumentException("Unknown cluster kind: " + value);
  }
}
