package com.github.streamshub.console.api.model.jsonapi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.streamshub.console.api.support.ErrorCategory;

public class JsonApiRootData<T> extends JsonApiRoot {

    @Valid
    @NotNull(payload = ErrorCategory.InvalidResource.class)
    private final T data;

    protected JsonApiRootData(T data) {
        this.data = data;
    }

    @JsonProperty
    public T getData() {
        return data;
    }
}
