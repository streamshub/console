package com.github.streamshub.console.config;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.sundr.builder.annotations.Buildable;

@JsonInclude(Include.NON_NULL)
@Buildable(editableEnabled = false)
public class SchemaRegistryConfig implements Named {

    @NotBlank(message = "Schema registry `name` is required")
    String name;

    @NotBlank(message = "Schema registry `url` is required")
    String url;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
