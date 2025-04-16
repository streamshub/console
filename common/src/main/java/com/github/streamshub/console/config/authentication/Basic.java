package com.github.streamshub.console.config.authentication;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.streamshub.console.config.Value;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(Include.NON_NULL)
public class Basic {

    @NotBlank
    private String username;
    @Valid
    private Value password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Value getPassword() {
        return password;
    }

    public void setPassword(Value password) {
        this.password = password;
    }
}
