package com.github.streamshub.console.api.v1alpha1.status;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Condition {

    private String status;
    private String reason;
    private String message;
    private String type;
    private String lastTransitionTime;

    @JsonIgnore
    private boolean active = false;

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

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, reason, status, type);
    }

    /**
     * For the purposes of equality, we do not consider the
     * {@link lastTransitionTime} or {@link active}. The {@link active} flag is only
     * used within a single reconcile cycle and determines which conditions should
     * be set in the CR status and which are no longer relevant and may be removed.
     */
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

    /**
     * Constant values for the types used for conditions
     */
    public static final class Types {
        private Types() {
        }

        public static final String READY = "Ready";
        public static final String WARNING = "Warning";
        public static final String ERROR = "Error";
    }

    /**
     * Constant values for the reasons used for conditions
     */
    public static final class Reasons {
        private Reasons() {
        }

        public static final String DEPENDENTS_NOT_READY = "DependentsNotReady";
        public static final String INVALID_CONFIGURATION = "InvalidConfiguration";
        public static final String DEPRECATED_CONFIGURATION = "DeprecatedConfiguration";
        public static final String RECONCILIATION_EXCEPTION = "ReconciliationException";
    }
}
