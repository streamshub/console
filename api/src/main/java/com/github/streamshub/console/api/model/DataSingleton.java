package com.github.streamshub.console.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.github.streamshub.console.api.support.ErrorCategory;

public abstract class DataSingleton<T> extends JsonApiDocument {

    @Valid
    @NotNull(payload = ErrorCategory.InvalidResource.class)
    private final T data;

    protected DataSingleton(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }
}
