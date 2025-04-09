package com.github.streamshub.console.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.sundr.builder.annotations.Buildable;

@JsonInclude(Include.NON_NULL)
@Buildable(editableEnabled = false)
public class PrometheusConfig implements Authenticated, Trustable {

    @NotBlank(message = "Metrics source `name` is required")
    private String name;
    private Type type;
    @NotBlank(message = "Metrics source `url` is required")
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
}
