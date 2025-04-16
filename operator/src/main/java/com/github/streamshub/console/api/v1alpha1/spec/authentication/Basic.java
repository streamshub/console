package com.github.streamshub.console.api.v1alpha1.spec.authentication;

import com.github.streamshub.console.api.v1alpha1.spec.Value;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class Basic {

    @Required
    private String username;
    @Required
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
