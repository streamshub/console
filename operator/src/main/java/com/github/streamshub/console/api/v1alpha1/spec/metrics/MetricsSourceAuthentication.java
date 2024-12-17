package com.github.streamshub.console.api.v1alpha1.spec.metrics;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.fabric8.generator.annotation.ValidationRule;
import io.sundr.builder.annotations.Buildable;

@Buildable
@JsonInclude(JsonInclude.Include.NON_NULL)
@ValidationRule(
        value = "has(self.token) || (has(self.username) && has(self.password))",
        message = "One of `token` or `username` + `password` must be provided")
public class MetricsSourceAuthentication {

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