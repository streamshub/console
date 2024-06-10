package com.github.streamshub.console.config;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
public class KubernetesConfig {

    boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
