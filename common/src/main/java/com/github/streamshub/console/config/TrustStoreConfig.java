package com.github.streamshub.console.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.sundr.builder.annotations.Buildable;
import io.xlate.validation.constraints.Expression;

@Expression(
    classImports = "com.github.streamshub.console.config.TrustStoreConfig",
    when = "self.type == TrustStoreConfig.Type.PEM",
    value = "self.password == null && self.alias == null",
    message = "Trust store password and alias may not be used for PEM trust stores")
@Buildable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrustStoreConfig {

    @NotNull
    private Type type; // NOSONAR

    @NotNull
    @Expression(
        when = "self != null",
        message = "Trust store content.valueFrom is required (content.value may not be used)",
        value = "self.valueFrom != null && self.value == null")
    private Value content;

    @Valid
    private Value password;

    private String alias;

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Value getContent() {
        return content;
    }

    public void setContent(Value content) {
        this.content = content;
    }

    public Value getPassword() {
        return password;
    }

    public void setPassword(Value password) {
        this.password = password;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public enum Type {
        PEM("pem"), PKCS12("p12"), JKS("jks");

        private final String value;

        private Type(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
