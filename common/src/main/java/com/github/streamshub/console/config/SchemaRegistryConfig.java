package com.github.streamshub.console.config;

import jakarta.validation.constraints.NotBlank;

public class SchemaRegistryConfig {

    @NotBlank(message = "Schema registry `url` is required")
    String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
