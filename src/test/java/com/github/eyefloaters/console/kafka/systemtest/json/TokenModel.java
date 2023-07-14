package com.github.eyefloaters.console.kafka.systemtest.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenModel {
    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("expires_in")
    private int expire;

    @JsonProperty("refresh_expires_in")
    private int refreshExpire;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("not-before-policy")
    private String notBeforePolicy;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("session_state")
    private String sessionState;

    public TokenModel() {
    }

    public TokenModel(String accessToken, int expire, int refreshExpire, String tokenType, String notBeforePolicy, String scope, String refreshToken, String sessionState) {
        this.accessToken = accessToken;
        this.expire = expire;
        this.refreshExpire = refreshExpire;
        this.tokenType = tokenType;
        this.notBeforePolicy = notBeforePolicy;
        this.scope = scope;
        this.refreshToken = refreshToken;
        this.sessionState = sessionState;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public int getExpire() {
        return expire;
    }

    public void setExpire(int expire) {
        this.expire = expire;
    }

    public int getRefreshExpire() {
        return refreshExpire;
    }

    public void setRefreshExpire(int refreshExpire) {
        this.refreshExpire = refreshExpire;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getNotBeforePolicy() {
        return notBeforePolicy;
    }

    public void setNotBeforePolicy(String notBeforePolicy) {
        this.notBeforePolicy = notBeforePolicy;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
    }
}