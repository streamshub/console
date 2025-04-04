package com.github.streamshub.console.config;

import java.util.List;
import java.util.Locale;

import jakarta.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

@JsonTypeInfo(use = Id.DEDUCTION)
@JsonSubTypes({
    @JsonSubTypes.Type(AuthenticationConfig.Basic.class),
    @JsonSubTypes.Type(AuthenticationConfig.Bearer.class),
    @JsonSubTypes.Type(AuthenticationConfig.OIDC.class),
})
public abstract class AuthenticationConfig {
    private AuthenticationConfig() {
    }

    public static class Basic extends AuthenticationConfig {
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

    public static class Bearer extends AuthenticationConfig {
        @NotBlank
        private String token;

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }

    public static class OIDC extends AuthenticationConfig implements Trustable {
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
        private boolean absoluteExpiresIn = false;
        private GrantType grantType = GrantType.CLIENT;
        private TrustStoreConfig trustStore;

        @Override
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

        @Override
        public TrustStoreConfig getTrustStore() {
            return trustStore;
        }

        public void setTrustStore(TrustStoreConfig trustStore) {
            this.trustStore = trustStore;
        }
    }
}
