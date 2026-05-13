package com.github.streamshub.console.api.security;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.apache.kafka.clients.admin.Admin;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.console.api.model.jsonapi.JsonApiError;
import com.github.streamshub.console.api.model.jsonapi.JsonApiErrors;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.support.RootCause;

import io.quarkus.oidc.runtime.OidcAuthenticationMechanism;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.vertx.http.runtime.FormAuthConfig.CookieSameSite;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.quarkus.vertx.http.security.Form;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@Alternative
@Priority(1)
@ApplicationScoped
public class ConsoleAuthenticationMechanism implements HttpAuthenticationMechanism {

    private static final Set<String> UNAUTHENTICATED_PATHS = Set.of(
            "/health",
            "/metrics",
            "/openapi",
            "/swagger-ui"
    );

    @Inject
    Logger log;

    @Inject
    ObjectMapper mapper;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    KafkaContext.Manager contextManager;

    @Inject
    IdentitySupport identities;

    @Inject
    OidcAuthenticationMechanism oidc;

    public static HttpAuthenticationMechanism form(String clusterId) {
        String securedPath = "/api/kafkas/" + clusterId;
        return Form.builder()
                        .httpOnlyCookie()
                        .postLocation("/api/kafkas/" + clusterId + "/session")
                        .usernameParameter("u")
                        .passwordParameter("p")
                        .cookieName("streamshub-console-kafka-" + clusterId)
                        .cookiePath(securedPath)
                        .cookieSameSite(CookieSameSite.STRICT)
                        .landingPage(null)
                        .build();
    }

    boolean oidcEnabled() {
        return Objects.nonNull(consoleConfig.getSecurity().getOidc());
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        final String requestPath = context.normalizedPath();

        if (UNAUTHENTICATED_PATHS.stream().anyMatch(requestPath::startsWith)) {
            return Uni.createFrom().nullItem();
        }

        if (oidcEnabled()) {
            return oidc.authenticate(context, identityProviderManager)
                    .map(identity -> identities.augmentIdentity(context, identity))
                    .onFailure().invoke(this::logAuthenticationFailure);
        }

        if ("/api/metadata".equals(requestPath)) {
            // Let metadata be public when OIDC is not enabled
            return Uni.createFrom().nullItem();
        }

        String clusterId = getClusterId(context);

        if (clusterId == null) {
            // Let Kafka cluster listing (home page) be public when OIDC is not enabled
            return Uni.createFrom().item(identities.createAnonymousIdentity(null));
        }

        var ctx = contextManager.acquire(clusterId);

        if (ctx == null) {
            // No Kafka context to establish identity, become anonymous
            return Uni.createFrom().item(identities.createAnonymousIdentity(null));
        }

        try {
            String saslMechanism = ctx.saslMechanism(Admin.class);

            if (ctx.admin() != null || saslMechanism.isEmpty()) {
                // Admin credentials already given or there is no SASL authentication needed
                return Uni.createFrom().item(identities.createAnonymousIdentity(ctx));
            }

            context.put("kafka.id", clusterId);
            context.put("kafka.context", ctx);

            return ctx.formAuthentication().authenticate(context, identityProviderManager)
                .map(identity -> identities.augmentIdentity(context, identity))
                .onFailure().invoke(this::logAuthenticationFailure);
        } finally {
            contextManager.release(ctx);
        }
    }

    @Override
    public Uni<Boolean> sendChallenge(RoutingContext context) {
        return getChallenge(context).map(challengeData -> {
            if (challengeData == null) {
                return false;
            }

            if (!(challengeData instanceof PayloadChallengeData)) {
                // This is challenge data from form-based login
                return true;
            }

            var response = context.response();
            response.setStatusCode(challengeData.status);
            challengeData.getHeaders().forEach(response.headers()::set);
            response.headers().set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);

            try {
                response.send(mapper.writeValueAsString(((PayloadChallengeData) challengeData).payload));
            } catch (IOException e) {
                log.warnf(e, "Failed to serialize challenge response body: %s", e.getMessage());
            }

            return true;
        });
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        if (oidcEnabled()) {
            return oidc.getChallenge(context)
                    .map(data -> {
                        var category = ErrorCategory.get(ErrorCategory.NotAuthenticated.class);
                        JsonApiError error = category.createError("Authentication credentials missing or invalid", null, null);
                        var responseBody = new JsonApiErrors(List.of(error));
                        return (ChallengeData) new PayloadChallengeData(data, responseBody);
                    })
                    .onFailure().recoverWithItem(t -> {
                        var category = ErrorCategory.get(ErrorCategory.ServerError.class);
                        JsonApiError error = category.createError("Authentication failed due to internal server error", null, null);
                        var responseBody = new JsonApiErrors(List.of(error));
                        return new PayloadChallengeData(500, null, null, responseBody);
                    });
        }

        String clusterId = getClusterId(context);

        if (clusterId == null) {
            return Uni.createFrom().nullItem();
        }

        var ctx = contextManager.acquire(clusterId);

        if (ctx == null) {
            return Uni.createFrom().nullItem();
        }

        try {
            return ctx.formAuthentication().getChallenge(context);
        } finally {
            contextManager.release(ctx);
        }
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        if (oidcEnabled()) {
            return oidc.getCredentialTypes();
        }

        return Set.of(UsernamePasswordAuthenticationRequest.class, TrustedAuthenticationRequest.class);
    }

    private String getClusterId(RoutingContext context) {
        Pattern p = Pattern.compile("/api/kafkas/([^/]+)(?:/.*)?");
        Matcher m = p.matcher(context.normalizedPath());
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

    private void logAuthenticationFailure(Throwable t) {
        Throwable rootCause = RootCause.of(t).orElse(t);

        if (rootCause instanceof org.jose4j.jwt.consumer.InvalidJwtException ije) {
            log.debugf("Invalid JWT: %s", ije.getErrorDetails());
        } else {
            log.debugf("Authentication failed: %s", rootCause);
        }
    }

    private static class PayloadChallengeData extends ChallengeData {
        public final Object payload;

        public PayloadChallengeData(int status, CharSequence headerName, String headerContent, Object payload) {
            super(status, headerName, headerContent);
            this.payload = payload;
        }

        public PayloadChallengeData(ChallengeData data, Object payload) {
            super(data.status, data.getHeaders());
            this.payload = payload;
        }
    }
}
