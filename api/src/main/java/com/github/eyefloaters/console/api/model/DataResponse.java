package com.github.eyefloaters.console.api.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.github.eyefloaters.console.api.support.ErrorCategory;

public abstract class DataResponse<T> {

    @Valid
    @NotNull(payload = ErrorCategory.InvalidResource.class)
    private final T data;

    protected DataResponse(T data) {
        this.data = data;
    }

    public T getData() {
        return data;
    }
}
