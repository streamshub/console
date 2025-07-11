package com.github.streamshub.console.config;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.streamshub.console.config.authentication.Authenticated;
import com.github.streamshub.console.config.authentication.AuthenticationConfig;

import io.sundr.builder.annotations.Buildable;

@JsonInclude(Include.NON_NULL)
@Buildable(editableEnabled = false)
public class KafkaConnectConfig implements Authenticated, Trustable {

    @NotBlank(message = "Kafka Connect `name` is required")
    private String name;

    private String namespace;

    @NotBlank(message = "Kafka Connect `url` is required")
    private String url;

    @Valid
    private AuthenticationConfig authentication;

    @Valid
    private TrustStoreConfig trustStore;

    @NotEmpty(message = "Kafka Connect `kafkaClusters` must contain at least 1 entry")
    private List<@NotBlank String> kafkaClusters = new ArrayList<>();

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
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

    public List<String> getKafkaClusters() {
        return kafkaClusters;
    }

    public void setKafkaClusters(List<String> kafkaClusters) {
        this.kafkaClusters = kafkaClusters;
    }

}
