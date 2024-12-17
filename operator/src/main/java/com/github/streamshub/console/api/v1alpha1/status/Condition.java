package com.github.streamshub.console.api.v1alpha1.status;

import java.time.Instant;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Condition {

    public static final class Types {
        private Types() {
        }

        public static final String READY = "Ready";
        public static final String ERROR = "Error";
    }

    public static final class Reasons {
        private Reasons() {
        }

        public static final String DEPENDENTS_NOT_READY = "DependentsNotReady";
        public static final String INVALID_CONFIGURATION = "InvalidConfiguration";
        public static final String RECONCILIATION_EXCEPTION = "ReconciliationException";
    }

    private String status;
    private String reason;
    private String message;
    private String type;
    private String lastTransitionTime;
    @JsonIgnore
    private Instant lastUpdatedTime = Instant.MIN;

    @JsonPropertyDescription("The status of the condition, either True, False or Unknown.")
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @JsonPropertyDescription("The reason for the condition's last transition (a single word in CamelCase).")
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @JsonPropertyDescription("The unique identifier of a condition, used to distinguish between other conditions in the resource.")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @JsonPropertyDescription("Last time the condition of a type changed from one status to another. " +
            "The required format is 'yyyy-MM-ddTHH:mm:ssZ', in the UTC time zone")
    public String getLastTransitionTime() {
        return lastTransitionTime;
    }

    public void setLastTransitionTime(String lastTransitionTime) {
        this.lastTransitionTime = lastTransitionTime;
    }

    @JsonPropertyDescription("Human-readable message indicating details about the condition's last transition.")
    @JsonInclude(value = JsonInclude.Include.NON_NULL)
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public void setLastUpdatedTime(Instant lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, reason, status, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof Condition))
            return false;
        Condition other = (Condition) obj;
        return Objects.equals(message, other.message)
                && Objects.equals(reason, other.reason)
                && Objects.equals(status, other.status)
                && Objects.equals(type, other.type);
    }

    @Override
    public String toString() {
        return """
                { \
                type = "%s", \
                status = "%s", \
                reason = "%s", \
                message = "%s", \
                lastTransitionTime = "%s" \
                }""".formatted(type, status, reason, message, lastTransitionTime);
    }
}
