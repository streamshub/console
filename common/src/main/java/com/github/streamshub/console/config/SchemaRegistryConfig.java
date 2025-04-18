package com.github.streamshub.console.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.streamshub.console.config.authentication.Authenticated;
import com.github.streamshub.console.config.authentication.AuthenticationConfig;

import io.sundr.builder.annotations.Buildable;

@JsonInclude(Include.NON_NULL)
@Buildable(editableEnabled = false)
public class SchemaRegistryConfig implements Authenticated, Trustable {

    @NotBlank(message = "Schema registry `name` is required")
    private String name;

    @NotBlank(message = "Schema registry `url` is required")
    private String url;

    @Valid
    private AuthenticationConfig authentication;

    @Valid
    private TrustStoreConfig trustStore;

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

    @Override
    public AuthenticationConfig getAuthentication() {
        return authentication;
    }

    public void setAuthentication(AuthenticationConfig authentication) {
        this.authentication = authentication;
    }

    @Override
    public TrustStoreConfig getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(TrustStoreConfig trustStore) {
        this.trustStore = trustStore;
    }

}
