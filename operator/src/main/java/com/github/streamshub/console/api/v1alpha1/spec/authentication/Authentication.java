package com.github.streamshub.console.api.v1alpha1.spec.authentication;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonClassDescription;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.fabric8.generator.annotation.ValidationRule;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonClassDescription("""
    Define authentication mechanism for HTTP clients use to integrate services \
    to the console. For example, authentication for a remote Prometheus instance.
    """)
@ValidationRule(
    value = "has(self.basic) || has(self.bearer) || has(self.oidc)",
    message = "One of `basic`, `bearer`, or `oidc` must be provided")
public class Authentication {

    private Basic basic;
    private Bearer bearer;
    private OIDC oidc;

    @JsonIgnore
    public boolean hasBasic() {
        return Objects.nonNull(basic);
    }

    @JsonIgnore
    public boolean hasBearer() {
        return Objects.nonNull(bearer);
    }

    @JsonIgnore
    public boolean hasOIDC() {
        return Objects.nonNull(oidc);
    }

    public Basic getBasic() {
        return basic;
    }

    public void setBasic(Basic basic) {
        this.basic = basic;
    }

    public Bearer getBearer() {
        return bearer;
    }

    public void setBearer(Bearer bearer) {
        this.bearer = bearer;
    }

    public OIDC getOidc() {
        return oidc;
    }

    public void setOidc(OIDC oidc) {
        this.oidc = oidc;
    }
}
