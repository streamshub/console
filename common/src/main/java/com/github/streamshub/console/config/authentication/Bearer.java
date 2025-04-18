package com.github.streamshub.console.config.authentication;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.streamshub.console.config.Value;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(Include.NON_NULL)
public class Bearer {

    @Valid
    private Value token;

    public Value getToken() {
        return token;
    }

    public void setToken(Value token) {
        this.token = token;
    }
}
