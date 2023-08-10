package com.github.eyefloaters.console.api.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(value = Include.NON_NULL)
@Schema(description = "An object containing references to the primary source of the error.")
public class ErrorSource {

    /**
     * A JSON Pointer [RFC6901] to the value in the request document that caused the
     * error [e.g. "/data" for a primary data object, or "/data/attributes/title"
     * for a specific attribute].
     */
    @Schema(description = """
            A JSON Pointer [RFC6901] to the value in the request document that caused the
            error [e.g. "/data" for a primary data object, or "/data/attributes/title"
            for a specific attribute].
            """)
    String pointer;

    /**
     * A string indicating which URI query parameter caused the error.
     */
    @Schema(description = "A string indicating which URI query parameter caused the error.")
    String parameter;

    /**
     * A string indicating the name of a single request header which caused the
     * error.
     */
    @Schema(description = "A string indicating the name of a single request header which caused the error.")
    String header;

    public String getPointer() {
        return pointer;
    }

    public void setPointer(String pointer) {
        this.pointer = pointer;
    }

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    @Override
    public String toString() {
        if (pointer != null) {
            return "pointer[" + pointer + ']';
        }
        if (parameter != null) {
            return "parameter[" + parameter + ']';
        }
        if (header != null) {
            return "header[" + header + ']';
        }
        return super.toString();
    }
}
