package com.github.streamshub.console.api.security;

import java.io.IOException;
import java.security.Permission;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule;
import org.jboss.logging.Logger;
import org.jose4j.jwt.JwtClaims;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.console.api.ClientFactory;
import com.github.streamshub.console.api.model.Error;
import com.github.streamshub.console.api.model.ErrorResponse;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.config.security.SecurityConfig;
import com.github.streamshub.console.config.security.SubjectConfig;

import io.quarkus.oidc.runtime.OidcAuthenticationMechanism;
import io.quarkus.oidc.runtime.OidcJwtCallerPrincipal;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.credential.Credential;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AnonymousAuthenticationRequest;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.identity.request.TokenAuthenticationRequest;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.core.MultiMap;
import io.vertx.ext.web.RoutingContext;

@Alternative
@Priority(1)
@ApplicationScoped
public class ConsoleAuthenticationMechanism implements HttpAuthenticationMechanism {

    public static final String OAUTHBEARER = OAuthBearerLoginModule.OAUTHBEARER_MECHANISM;
    public static final String PLAIN = "PLAIN";
    public static final String SCRAM_SHA256 = "SCRAM-SHA-256";
    public static final String SCRAM_SHA512 = "SCRAM-SHA-512";

    private static final String BEARER = "Bearer ";
    private static final String BASIC = "Basic ";

    private static class Audit extends java.util.logging.Level {
        private static final long serialVersionUID = 1L;

        Audit() {
            super("AUDIT", java.util.logging.Level.INFO.intValue() - 1);
        }
    }

    private static final java.util.logging.Level AUDIT = new Audit();

    private static final SecurityIdentity ANONYMOUS = QuarkusSecurityIdentity.builder()
            .setAnonymous(true)
            .setPrincipal(new QuarkusPrincipal("ANONYMOUS"))
            .build();

    @Inject
    Logger log;

    @Inject
    ObjectMapper mapper;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    Map<String, KafkaContext> contexts;

    @Inject
    OidcAuthenticationMechanism oidc;

    boolean oidcEnabled() {
        return Objects.nonNull(consoleConfig.getSecurity().getOidc());
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        if (oidcEnabled()) {
            return oidc.authenticate(context, identityProviderManager)
                    .map(identity -> {
                        if (identity != null) {
                            String clusterId = getClusterId(context);
                            var ctx = clusterId != null ? contexts.get(clusterId) : null;
                            return createIdentity(ctx, identity);
                        }
                        throw new AuthenticationFailedException();
                    });
        }

        String clusterId = getClusterId(context);

        if (clusterId == null) {
            return Uni.createFrom().item(createAnonymousIdentity(null));
        }

        var ctx = contexts.get(clusterId);

        if (ctx == null) {
            return Uni.createFrom().item(createAnonymousIdentity(null));
        }

        String saslMechanism = ctx.saslMechanism(Admin.class);

        if (ctx.admin() != null || saslMechanism.isEmpty()) {
            return Uni.createFrom().item(createAnonymousIdentity(ctx));
        }

        var identity = createIdentity(ctx, context.request().headers(), saslMechanism);

        if (identity != null) {
            return Uni.createFrom().item(identity);
        }

        return Uni.createFrom().failure(new AuthenticationFailedException());
    }

    @Override
    public Uni<Boolean> sendChallenge(RoutingContext context) {
        return getChallenge(context).map(challengeData -> {
            if (challengeData == null) {
                return false;
            }

            var response = context.response();
            response.setStatusCode(challengeData.status);

            if (challengeData.headerName != null) {
                response.headers().set(challengeData.headerName, challengeData.headerContent);
            }

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
                        Error error = category.createError("Authentication credentials missing or invalid", null, null);
                        var responseBody = new ErrorResponse(List.of(error));
                        return new PayloadChallengeData(data, responseBody);
                    });
        }

        String clusterId = getClusterId(context);

        if (clusterId == null) {
            return Uni.createFrom().nullItem();
        }

        var ctx = contexts.get(clusterId);

        if (ctx == null) {
            return Uni.createFrom().nullItem();
        }

        String saslMechanism = ctx.saslMechanism(Admin.class);
        String scheme = getAuthorizationScheme(saslMechanism);
        ChallengeData challenge;

        if (scheme != null) {
            var category = ErrorCategory.get(ErrorCategory.NotAuthenticated.class);
            Error error = category.createError("Authentication credentials missing or invalid", null, null);
            var responseBody = new ErrorResponse(List.of(error));
            challenge = new PayloadChallengeData(401, "WWW-Authenticate", scheme, responseBody);
        } else {
            log.warnf("Access not permitted to cluster %s with unknown SASL mechanism '%s'",
                    clusterId, saslMechanism);
            var category = ErrorCategory.get(ErrorCategory.ResourceNotFound.class);
            Error error = category.createError(ClientFactory.NO_SUCH_KAFKA_MESSAGE.formatted(clusterId), null, null);
            var responseBody = new ErrorResponse(List.of(error));
            challenge = new PayloadChallengeData(404, null, null, responseBody);
        }

        return Uni.createFrom().item(challenge);
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        if (oidcEnabled()) {
            return oidc.getCredentialTypes();
        }

        return Set.of(
            AnonymousAuthenticationRequest.class,
            TokenAuthenticationRequest.class,
            UsernamePasswordAuthenticationRequest.class
        );
    }

    String getClusterId(RoutingContext context) {
        Pattern p = Pattern.compile("/api/kafkas/([^/]+)(?:/.*)?");
        Matcher m = p.matcher(context.normalizedPath());
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

    String getAuthorizationScheme(String saslMechanism) {
        switch (saslMechanism) {
            case OAUTHBEARER:
                return BEARER.trim();
            case PLAIN, SCRAM_SHA256, SCRAM_SHA512:
                return BASIC.trim();
            default:
                return null;
        }
    }

    SecurityIdentity createAnonymousIdentity(KafkaContext ctx) {
        return createIdentity(ctx, ANONYMOUS);
    }

    SecurityIdentity createIdentity(KafkaContext ctx, SecurityIdentity source) {
        var builder = QuarkusSecurityIdentity.builder(source);
        addRoleChecker(ctx, builder, source.getPrincipal());
        return builder.build();
    }

    SecurityIdentity createIdentity(KafkaContext ctx, MultiMap headers, String saslMechanism) {
        switch (saslMechanism) {
            case OAUTHBEARER:
                return createOAuthIdentity(ctx, headers);
            case PLAIN:
                return createBasicIdentity(ctx, headers, SaslJaasConfigCredential::forPlainLogin);
            case SCRAM_SHA256, SCRAM_SHA512:
                return createBasicIdentity(ctx, headers, SaslJaasConfigCredential::forScramLogin);
            default:
                return null;
        }
    }

    SecurityIdentity createOAuthIdentity(KafkaContext ctx, MultiMap headers) {
        return getAuthorization(headers, BEARER)
            .map(accessToken -> {
                var builder = QuarkusSecurityIdentity.builder();
                builder.addCredential(SaslJaasConfigCredential.forOAuthLogin(accessToken));
                Principal principal;

                try {
                    var claims = JwtClaims.parse(accessToken);
                    principal = new OidcJwtCallerPrincipal(claims, null);
                } catch (Exception e) {
                    log.infof("JWT access token could not be parsed: %s", e.getMessage());
                    principal = new QuarkusPrincipal("UNKNOWN");
                }

                builder.setPrincipal(principal);
                addRoleChecker(ctx, builder, principal);
                return builder.build();
            })
            .orElse(null);
    }

    SecurityIdentity createBasicIdentity(KafkaContext ctx, MultiMap headers, BiFunction<String, String, Credential> credentialBuilder) {
        return getBasicAuthentication(headers)
            .map(userpass -> {
                var builder = QuarkusSecurityIdentity.builder();
                var principal = new QuarkusPrincipal(userpass[0]);
                builder.addCredential(credentialBuilder.apply(userpass[0], userpass[1]));
                builder.setPrincipal(principal);
                addRoleChecker(ctx, builder, principal);
                return builder.build();
            })
            .orElse(null);
    }

    void addRoleChecker(KafkaContext ctx, QuarkusSecurityIdentity.Builder builder, Principal principal) {
        Stream<SubjectConfig> globalSubjects = consoleConfig.getSecurity().getSubjects().stream();
        Stream<SubjectConfig> clusterSubjects = Stream.empty();

        if (ctx != null) {
            clusterSubjects = ctx.clusterConfig().getSecurity().getSubjects().stream();
        }

        List<String> roleNames = Stream.concat(clusterSubjects, globalSubjects)
                .filter(sub -> Objects.isNull(sub.getIssuer()) /* or issuer matches `iss` claim */)
                .filter(sub -> Objects.isNull(sub.getClaim()) /* only without OIDC */)
                .filter(sub -> sub.getInclude().contains(principal.getName()))
                .flatMap(sub -> sub.getRoleNames().stream())
                .distinct()
                .toList();

        Stream<Permission> globalPermissions = getPermissions(consoleConfig.getSecurity(), roleNames, "");
        Stream<Permission> clusterPermissions = Stream.empty();

        if (ctx != null) {
            clusterPermissions = getPermissions(
                ctx.clusterConfig().getSecurity(),
                roleNames,
                "kafkas/" + ctx.clusterId() + '/'
            );
        }

        List<Permission> possessedPermissions = Stream.concat(globalPermissions, clusterPermissions).toList();

        builder.addPermissionChecker(requiredPermission -> {
            boolean allowed = possessedPermissions
                    .stream()
                    .anyMatch(possessed -> possessed.implies(requiredPermission));

            String category = getClass().getPackageName() + (allowed ? ".ALLOW" : ".DENY");

            java.util.logging.Logger.getLogger(category).log(AUDIT, () -> {
                return String.format("Principal %s %s access to %s", principal.getName(), allowed ? "allowed" : "denied", requiredPermission);
            });

            return Uni.createFrom().item(allowed);
        });
    }

    Stream<Permission> getPermissions(SecurityConfig security, Collection<String> roleNames, String resourcePrefix) {
        return security.getRoles()
                .stream()
                .filter(role -> roleNames.contains(role.getName()))
                .flatMap(role -> role.getRules().stream())
                .flatMap(rule -> {
                    List<Permission> rulePermissions = new ArrayList<>();
                    Privilege[] actions = rule.getPrivileges().toArray(Privilege[]::new);

                    for (var resource : rule.getResources()) {
                        rulePermissions.add(new ConsolePermission(
                            resourcePrefix + resource,
                            rule.getResourceNames(),
                            actions
                        ));
                    }

                    return rulePermissions.stream();
                });
    }

    Optional<String[]> getBasicAuthentication(MultiMap headers) {
        return getAuthorization(headers, BASIC)
            .map(Base64.getDecoder()::decode)
            .map(String::new)
            .filter(authn -> authn.indexOf(':') >= 0)
            .map(authn -> new String[] {
                authn.substring(0, authn.indexOf(':')),
                authn.substring(authn.indexOf(':') + 1)
            })
            .filter(userPass -> !userPass[0].isEmpty() && !userPass[1].isEmpty());
    }

    Optional<String> getAuthorization(MultiMap headers, String scheme) {
        return Optional.ofNullable(headers.get(HttpHeaders.AUTHORIZATION))
            .filter(header -> header.regionMatches(true, 0, scheme, 0, scheme.length()))
            .map(header -> header.substring(scheme.length()));
    }

    static class PayloadChallengeData extends ChallengeData {
        public final Object payload;

        public PayloadChallengeData(int status, CharSequence headerName, String headerContent, Object payload) {
            super(status, headerName, headerContent);
            this.payload = payload;
        }

        public PayloadChallengeData(ChallengeData data, Object payload) {
            super(data.status, data.headerName, data.headerContent);
            this.payload = payload;
        }
    }
}
