package com.github.streamshub.console.api.security;

import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule;
import org.apache.kafka.common.security.plain.PlainLoginModule;
import org.apache.kafka.common.security.scram.ScramLoginModule;

import io.quarkus.security.credential.Credential;

public class SaslJaasConfigCredential implements Credential {

    private static final String SASL_OAUTH_CONFIG_TEMPLATE = OAuthBearerLoginModule.class.getName()
            + " required"
            + " oauth.access.token=\"%s\" ;";

    private static final String BASIC_TEMPLATE = "%s required username=\"%%s\" password=\"%%s\" ;";
    private static final String SASL_PLAIN_CONFIG_TEMPLATE = BASIC_TEMPLATE.formatted(PlainLoginModule.class.getName());
    private static final String SASL_SCRAM_CONFIG_TEMPLATE = BASIC_TEMPLATE.formatted(ScramLoginModule.class.getName());

    public static SaslJaasConfigCredential forOAuthLogin(String accessToken) {
        return new SaslJaasConfigCredential(SASL_OAUTH_CONFIG_TEMPLATE.formatted(accessToken));
    }

    public static SaslJaasConfigCredential forPlainLogin(String username, String password) {
        return new SaslJaasConfigCredential(SASL_PLAIN_CONFIG_TEMPLATE.formatted(username, password));
    }

    public static SaslJaasConfigCredential forScramLogin(String username, String password) {
        return new SaslJaasConfigCredential(SASL_SCRAM_CONFIG_TEMPLATE.formatted(username, password));
    }

    private final String value;

    private SaslJaasConfigCredential(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
