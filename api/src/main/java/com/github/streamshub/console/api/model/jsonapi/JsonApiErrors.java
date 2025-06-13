package com.github.streamshub.console.api.model.jsonapi;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.github.streamshub.console.api.support.OASModelFilter;

@Schema(extensions = @Extension(name = OASModelFilter.REMOVE, parseValue = true, value = "[ \"included\" ]"))
public class JsonApiErrors extends JsonApiRoot {

    private final List<JsonApiError> errors;

    public JsonApiErrors(List<JsonApiError> errors) {
        this.errors = errors;
    }

    public List<JsonApiError> getErrors() {
        return errors;
    }
}
