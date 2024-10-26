package com.github.streamshub.console.api.v1alpha1.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.streamshub.console.config.Named;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Prometheus implements Named {

    @Required
    private String name;
    @Required
    private Type type;
    private String url;
    private Authentication authentication;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public enum Type {
        EMBEDDED("embedded"),
        OPENSHIFT_MONITORING("openshift-monitoring"),
        STANDALONE("standalone");

        private final String value;

        private Type(String value) {
            this.value = value;
        }

        @JsonValue
        public String value() {
            return value;
        }

        @JsonCreator
        public static Type fromValue(String value) {
            if (value == null) {
                return STANDALONE;
            }

            for (var type : values()) {
                if (type.value.equals(value.trim())) {
                    return type;
                }
            }

            throw new IllegalArgumentException("Invalid Prometheus type: " + value);
        }
    }

    @JsonTypeInfo(use = Id.DEDUCTION)
    @JsonSubTypes({ @JsonSubTypes.Type(Basic.class), @JsonSubTypes.Type(Bearer.class) })
    abstract static class Authentication {
    }

    public static class Basic extends Authentication {
        @Required
        private String username;
        @Required
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class Bearer extends Authentication {
        @Required
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
