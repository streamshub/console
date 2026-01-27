package com.github.streamshub.console.api.support;

import java.time.Duration;
import java.util.Base64;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import jakarta.enterprise.inject.spi.CDI;

import org.jboss.logging.Logger;

import com.github.streamshub.console.config.Value;
import com.github.streamshub.console.config.authentication.Authenticated;
import com.github.streamshub.console.config.authentication.AuthenticationConfig;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.runtime.OidcClientConfig.Grant;
import io.quarkus.oidc.client.runtime.TokensHelper;
import io.quarkus.oidc.common.runtime.config.OidcClientCommonConfig.Credentials;

public class AuthenticationSupport implements Supplier<Optional<String>> {

    private static final Logger LOGGER = Logger.getLogger(AuthenticationSupport.class);
    private final AuthenticationConfig authConfig;
    private final CompletableFuture<OidcClient> oidcClient;
    private final TokensHelper tokens;

    public AuthenticationSupport(Authenticated serviceConfig) {
        this.authConfig = serviceConfig.getAuthentication();

        if (authConfig != null && authConfig.hasOIDC()) {
            var oidc = authConfig.getOidc();
            final var builder = io.quarkus.oidc.client.runtime.OidcClientConfig.builder()
                .id(serviceConfig.getName() + "-oidc-client")
                .authServerUrl(oidc.getAuthServerUrl())
                .tokenPath(oidc.getTokenPath())
                .scopes(Optional.ofNullable(oidc.getScopes()).orElseGet(Collections::emptyList))
                .clientId(oidc.getClientId());

            setIfPresent(oidc.isAbsoluteExpiresIn(), builder::absoluteExpiresIn);
            setIfPresent(
                    Value.getOptional(oidc.getClientSecret()).orElse(null),
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
                        // Unknown grant type
                        return;
                }

                setIfPresent(
                    oidc.getGrantOptions(),
                    options -> builder.grantOptions(g.name().toLowerCase(Locale.ENGLISH), options)
                );
            });

            CDI.current().select(TrustStoreSupport.class).get().configureTruststoreFile(
                oidc,
                serviceConfig,
                builder
            );

            tokens = new TokensHelper();
            oidcClient = CDI.current().select(OidcClients.class)
                    .get()
                    .newClient(builder.build())
                    .subscribeAsCompletionStage();
        } else {
            tokens = null;
            oidcClient = CompletableFuture.failedFuture(new IllegalStateException("OIDC client requested for non-OIDC authentication mechanism"));
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

        if (authConfig == null) {
            authHeader = null;
        } else if (authConfig.hasBasic()) {
            var basic = authConfig.getBasic();
            authHeader = "Basic " + Base64.getEncoder().encodeToString("%s:%s".formatted(
                    basic.getUsername(),
                    Value.getOptional(basic.getPassword()).orElse(""))
                    .getBytes());
        } else if (authConfig.hasBearer()) {
            authHeader = "Bearer " + Value.getOptional(authConfig.getBearer().getToken()).orElseThrow();
        } else if (authConfig.hasOIDC()) {
            OidcClient client;

            try {
                client = oidcClient.get(10, TimeUnit.SECONDS);
            } catch (Exception e) {
                LOGGER.errorf(e, "Failed to obtain OIDC client: %s", e.getMessage());

                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                throw new RuntimeException(e);
            }

            authHeader = tokens.getTokens(client)
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
