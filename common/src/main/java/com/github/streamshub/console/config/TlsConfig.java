package com.github.streamshub.console.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TlsConfig {

    @NotNull
    @Valid
    private Value certificate;

    @NotNull
    @Valid
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
