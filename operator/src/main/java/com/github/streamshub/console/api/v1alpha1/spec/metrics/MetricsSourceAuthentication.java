package com.github.streamshub.console.api.v1alpha1.spec.metrics;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.streamshub.console.api.v1alpha1.spec.authentication.Authentication;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricsSourceAuthentication extends Authentication {

    private String username;
    private String password;
    private String token;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}