package com.github.streamshub.console.api.v1alpha1.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Tls {

    @JsonProperty("certificate")
    @JsonPropertyDescription("TLS certificate content (PEM format)")
    private Value certificate;

    @JsonProperty("key")
    @JsonPropertyDescription("TLS private key content (PEM format)")
    private Value key;

    public Value getCertificate() {
        return certificate;
    }

    public void setCertificate(Value certificate) {
        this.certificate = certificate;
    }

    public Value getKey() {
        return key;
    }

    public void setKey(Value key) {
        this.key = key;
    }
}
