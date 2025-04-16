package com.github.streamshub.console.config.authentication;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.github.streamshub.console.config.TrustStoreConfig;
import com.github.streamshub.console.config.Trustable;
import com.github.streamshub.console.config.Value;

import io.sundr.builder.annotations.Buildable;

@Buildable(editableEnabled = false)
@JsonInclude(Include.NON_NULL)
public class OIDC implements Trustable {
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
    private String clientId;
    private Value clientSecret;
    private Method method;
    private List<String> scopes;
    private Boolean absoluteExpiresIn;
    private GrantType grantType = GrantType.CLIENT;
    private Map<String, String> grantOptions;
    private TrustStoreConfig trustStore;

    @Override
    @JsonIgnore
    public String getName() {
        return "oidc";
    }

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

    public Boolean isAbsoluteExpiresIn() {
        return absoluteExpiresIn;
    }

    public void setAbsoluteExpiresIn(Boolean absoluteExpiresIn) {
        this.absoluteExpiresIn = absoluteExpiresIn;
    }

    public GrantType getGrantType() {
        return grantType;
    }

    public void setGrantType(GrantType grantType) {
        this.grantType = grantType;
    }

    public Map<String, String> getGrantOptions() {
        return grantOptions;
    }

    public void setGrantOptions(Map<String, String> grantOptions) {
        this.grantOptions = grantOptions;
    }

    @Override
    public TrustStoreConfig getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(TrustStoreConfig trustStore) {
        this.trustStore = trustStore;
    }
}
