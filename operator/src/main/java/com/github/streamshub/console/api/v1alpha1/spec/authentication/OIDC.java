package com.github.streamshub.console.api.v1alpha1.spec.authentication;

import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.streamshub.console.api.v1alpha1.spec.TrustStore;
import com.github.streamshub.console.api.v1alpha1.spec.Value;

import io.fabric8.generator.annotation.Required;
import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OIDC {

    /**
     * Determines whether the client Id and secret are passed via HTTP Basic or as
     * POST form parameters.
     */
    public enum Method {
        BASIC, POST;

        @JsonCreator
        public static Method forValue(String value) {
            return valueOf(value.toUpperCase(Locale.ROOT));
        }
    }

    public enum GrantType {
        CLIENT, PASSWORD;

        @JsonCreator
        public static GrantType forValue(String value) {
            return valueOf(value.toUpperCase(Locale.ROOT));
        }
    }

    private String authServerUrl;
    private String tokenPath;
    @Required
    private String clientId;
    private Value clientSecret;
    private Method method;
    private List<String> scopes;
    private boolean absoluteExpiresIn = false;
    private GrantType grantType = GrantType.CLIENT;
    private TrustStore trustStore;

    public String getAuthServerUrl() {
        return authServerUrl;
    }

    public void setAuthServerUrl(String authServerUrl) {
        this.authServerUrl = authServerUrl;
    }

    public String getTokenPath() {
        return tokenPath;
    }

    public void setTokenPath(String tokenPath) {
        this.tokenPath = tokenPath;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Value getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(Value clientSecret) {
        this.clientSecret = clientSecret;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }

    public boolean isAbsoluteExpiresIn() {
        return absoluteExpiresIn;
    }

    public void setAbsoluteExpiresIn(boolean absoluteExpiresIn) {
        this.absoluteExpiresIn = absoluteExpiresIn;
    }

    public GrantType getGrantType() {
        return grantType;
    }

    public void setGrantType(GrantType grantType) {
        this.grantType = grantType;
    }

    public TrustStore getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(TrustStore trustStore) {
        this.trustStore = trustStore;
    }
}
