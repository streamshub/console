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
    @JsonPropertyDescription("""
            TLS private key content (PEM format). \
            Note: on OpenShift, when the certificate is presented by the Route \
            (i.e. when `spec.security.openshift` or `spec.security.ldap` is configured, \
            or when the Console is served with re-encrypt termination), the private key is \
            copied into the Route resource. Any user with permission to read Routes in the \
            Console namespace will be able to read the key.
            """)
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
