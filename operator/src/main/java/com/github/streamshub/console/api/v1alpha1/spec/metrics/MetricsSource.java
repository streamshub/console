package com.github.streamshub.console.api.v1alpha1.spec.metrics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.annotation.JsonValue;
import com.github.streamshub.console.api.v1alpha1.spec.TrustStore;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MetricsSource {

    @Required
    private String name;
    @Required
    private Type type;
    private String url;

    @JsonPropertyDescription("""
            Trust store configuration for when the metrics source uses \
            TLS certificates signed by an unknown CA.
            """)
    private TrustStore trustStore;

    private MetricsSourceAuthentication authentication;

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

    public TrustStore getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(TrustStore trustStore) {
        this.trustStore = trustStore;
    }

    public MetricsSourceAuthentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(MetricsSourceAuthentication authentication) {
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
}
