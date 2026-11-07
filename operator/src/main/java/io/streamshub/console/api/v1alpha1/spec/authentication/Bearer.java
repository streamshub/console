package io.streamshub.console.api.v1alpha1.spec.authentication;

import io.streamshub.console.api.v1alpha1.spec.Value;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class Bearer {

    @Required
    private Value token;

    public Value getToken() {
        return token;
    }

    public void setToken(Value token) {
        this.token = token;
    }
}
