package com.github.streamshub.console.api.v1alpha1.status;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Condition {

    private String status;
    private String reason;
    private String message;
    private String type;
    private String lastTransitionTime;

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
}
