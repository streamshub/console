package com.github.streamshub.console.api.support;

import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.CDI;

import com.github.streamshub.console.config.Authenticated;
import com.github.streamshub.console.config.AuthenticationConfig;
import com.github.streamshub.console.config.Value;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.runtime.OidcClientConfig.Grant;
import io.quarkus.oidc.client.runtime.TokensHelper;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials;

public class AuthenticationSupport implements Supplier<Optional<String>> {

    private final AuthenticationConfig authConfig;
    private final OidcClient oidcClient;
    private final TokensHelper tokens;

    public AuthenticationSupport(Authenticated serviceConfig) {
        this.authConfig = serviceConfig.getAuthentication();

        if (authConfig instanceof AuthenticationConfig.OIDC oidc) {

            final var builder = io.quarkus.oidc.client.runtime.OidcClientConfig.builder()
                .authServerUrl(oidc.getAuthServerUrl())
                .tokenPath(oidc.getTokenPath())
                .scopes(Optional.ofNullable(oidc.getScopes()).orElseGet(Collections::emptyList))
                .absoluteExpiresIn(oidc.isAbsoluteExpiresIn())
                .clientId(oidc.getClientId());

            setIfPresent(
                    UncheckedIO.call(
                            () -> Value.getValue(oidc.getClientSecret()),
                            () -> "Exception retrieving clientSecret"),
                    secret -> builder.credentials()
                        .clientSecret(secret)
                        .end());

            setIfPresent(oidc.getMethod(), m -> {
                switch (m) {
                    case BASIC:
                        builder.credentials().clientSecret()
                            .method(Credentials.Secret.Method.BASIC)
                            .end();
                        break;
                    case POST:
                        builder.credentials().clientSecret()
                            .method(Credentials.Secret.Method.POST)
                            .end();
                        break;
                    default:
                        break;
                }
            });

            setIfPresent(oidc.getGrantType(), g -> {
                switch (g) {
                    case CLIENT:
                        builder.grant(Grant.Type.CLIENT);
                        break;
                    case PASSWORD:
                        builder.grant(Grant.Type.PASSWORD);
                        break;
                    default:
                        break;
                }
            });

            setIfPresent(oidc.getTrustStore(), t ->
                CDI.current().select(TrustStoreSupport.class).get().configureTruststoreFile(
                    oidc,
                    serviceConfig,
                    builder
                )
            );

            tokens = new TokensHelper();
            oidcClient = CDI.current().select(OidcClients.class)
                    .get()
                    .newClient(builder.build())
                    .await()
                    .atMost(Duration.ofSeconds(10));
        } else {
            tokens = null;
            oidcClient = null;
        }
    }

    static <T> void setIfPresent(T value, Consumer<T> mutator) {
        if (value != null) {
            mutator.accept(value);
        }
    }

    @Override
    public Optional<String> get() {
        String authHeader;

        if (authConfig instanceof AuthenticationConfig.Basic basic) {
            authHeader = "Basic " + Base64.getEncoder().encodeToString("%s:%s".formatted(
                    basic.getUsername(),
                    basic.getPassword())
                    .getBytes());
        } else if (authConfig instanceof AuthenticationConfig.Bearer bearer) {
            authHeader = "Bearer " + bearer.getToken();
        } else if (authConfig instanceof AuthenticationConfig.OIDC) {
            authHeader = tokens.getTokens(oidcClient)
                .onItem()
                .transform(t -> "Bearer " + t.getAccessToken())
                .await()
                .atMost(Duration.ofSeconds(10));
        } else {
            authHeader = null;
        }

        return Optional.ofNullable(authHeader);
    }
}
