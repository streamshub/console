package com.github.streamshub.console.api.model;

import java.util.List;

public class ErrorResponse extends JsonApiDocument {

    private final List<Error> errors;

    public ErrorResponse(List<Error> errors) {
        this.errors = errors;
    }

    public List<Error> getErrors() {
        return errors;
    }
}
