package com.github.streamshub.console.config.authentication;

import java.util.Objects;

import jakarta.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.sundr.builder.annotations.Buildable;
import io.xlate.validation.constraints.Expression;

@Expression(
    message = "Exactly one of `basic`, `bearer`, or `oidc` must be configured",
    value = """
            (self.basic != null && self.bearer == null && self.oidc == null) ||
            (self.bearer != null && self.basic == null && self.oidc == null) ||
            (self.oidc != null && self.basic == null && self.bearer == null)
            """)
@Buildable(editableEnabled = false)
@JsonInclude(Include.NON_NULL)
public class AuthenticationConfig {

    @Valid
    private Basic basic;

    @Valid
    private Bearer bearer;

    @Valid
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
