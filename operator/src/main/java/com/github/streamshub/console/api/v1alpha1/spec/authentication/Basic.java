package com.github.streamshub.console.api.v1alpha1.spec.authentication;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class Basic {

    @Required
    private String username;
    @Required
    private String password;

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
}
