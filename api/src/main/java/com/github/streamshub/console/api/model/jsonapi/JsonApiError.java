package com.github.streamshub.console.api.model.jsonapi;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.media.SchemaProperty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
@Schema(
    properties = {
        @SchemaProperty(
            name = "meta",
            description = "A meta object containing non-standard meta-information about the error."),
        @SchemaProperty(
            name = "links",
            description = """
                a links object that MAY contain the following members:
                * `about`: a link that leads to further details about this particular occurrence of the problem.
                * `type`: a link that identifies the type of error that this particular error is an instance of."""),
    }
)
public class JsonApiError extends JsonApiBase {

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
    final String title;

    @Schema(description = "A human-readable explanation specific to this occurrence of the problem.")
    final String detail;

    @Schema(nullable = true, description = "Reference to the primary source of the error")
    ErrorSource source;

    @JsonIgnore
    final Throwable cause;

    public static JsonApiError forThrowable(Throwable thrown, String message) {
        JsonApiError error = new JsonApiError(message, thrown.getMessage(), thrown);
        error.addMeta("type", "error");
        return error;
    }

    public JsonApiError(String title, String detail, Throwable cause) {
        this.title = title;
        this.detail = detail;
        this.cause = cause;
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

    public String getDetail() {
        return detail;
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

    @Override
    public String toString() {
        return "id=%s title='%s' detail='%s' source=%s".formatted(id, title, detail, source);
    }
}
