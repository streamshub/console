package com.github.eyefloaters.console.api.model;

import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
public class Error {

    @Schema(description = "A meta object containing non-standard meta-information about the error")
    Map<String, String> meta;

    @Schema(description = "A unique identifier for this particular occurrence of the problem.")
    String id;

    @Schema(description = "The HTTP status code applicable to this problem, expressed as a string value.")
    String status;

    @Schema(description = "An application-specific error code, expressed as a string value")
    String code;

    @Schema(description = """
            A short, human-readable summary of the problem that does not change from
            occurrence to occurrence of the problem
            """)
    String title;

    @Schema(description = "A human-readable explanation specific to this occurrence of the problem.")
    String detail;

    @Schema(nullable = true, description = "Reference to the primary source of the error")
    ErrorSource source;

    @JsonIgnore
    Throwable cause;

    public static Error forThrowable(Throwable thrown, String message) {
        Error error = new Error(message, thrown.getMessage(), thrown);
        error.meta = Map.of("type", "error");
        return error;
    }

    public Error(String title, String detail, Throwable cause) {
        this.title = title;
        this.detail = detail;
        this.cause = cause;
    }

    public Map<String, String> getMeta() {
        return meta;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public ErrorSource getSource() {
        return source;
    }

    public void setSource(ErrorSource source) {
        this.source = source;
    }

    public Throwable getCause() {
        return cause;
    }
}
