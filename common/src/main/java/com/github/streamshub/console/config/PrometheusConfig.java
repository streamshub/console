package com.github.streamshub.console.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class PrometheusConfig implements Trustable {

    @NotBlank(message = "Metrics source `name` is required")
    private String name;
    private Type type;
    @NotBlank(message = "Metrics source `url` is required")
    private String url;
    @Valid
    private Authentication authentication;
    @Valid
    private TrustStoreConfig trustStore;

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

    public TrustStoreConfig getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(TrustStoreConfig trustStore) {
        this.trustStore = trustStore;
    }

    public enum Type {
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
        @NotBlank
        private String username;
        @NotBlank
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
        @NotBlank
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
