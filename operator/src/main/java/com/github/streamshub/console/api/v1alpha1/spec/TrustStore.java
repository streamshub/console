package com.github.streamshub.console.api.v1alpha1.spec;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TrustStore {

    @Required
    @JsonProperty("type")
    private Type type; // NOSONAR

    @Required
    @JsonProperty("content")
    @JsonPropertyDescription("Content of the trust store")
    private Value content;

    @JsonProperty("password")
    @JsonPropertyDescription("Content used to access the trust store, if necessary")
    private Value password;

    @JsonProperty("alias")
    @JsonPropertyDescription("Alias to select the appropriate certificate when multiple certificates are included.")
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
