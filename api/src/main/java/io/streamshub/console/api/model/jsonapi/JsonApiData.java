package io.streamshub.console.api.model.jsonapi;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.streamshub.console.api.support.ErrorCategory;

public class JsonApiData<T> extends JsonApiBase {

    @Valid
    @NotNull(payload = ErrorCategory.InvalidResource.class)
    private final T data;

    protected JsonApiData(T data) {
        this.data = data;
    }

    @JsonProperty
    public T getData() {
        return data;
    }
}
