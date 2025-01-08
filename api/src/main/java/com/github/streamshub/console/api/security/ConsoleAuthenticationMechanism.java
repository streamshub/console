package com.github.streamshub.console.api.security;

import java.io.IOException;
import java.security.Permission;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;
import org.jose4j.jwt.JwtClaims;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.console.api.ClientFactory;
import com.github.streamshub.console.api.model.Error;
import com.github.streamshub.console.api.model.ErrorResponse;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.security.Decision;
import com.github.streamshub.console.config.security.AuditConfig;
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

    private static final SecurityIdentity ANONYMOUS = QuarkusSecurityIdentity.builder()
            .setAnonymous(true)
            .setPrincipal(new QuarkusPrincipal("ANONYMOUS"))
            .build();

    private static final Set<String> UNAUTHENTICATED_PATHS = Set.of("/health", "/metrics", "/openapi", "/swagger-ui");

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
        final String requestPath = context.normalizedPath();

        if (UNAUTHENTICATED_PATHS.stream().anyMatch(unauthn -> requestPath.startsWith(unauthn))) {
            return Uni.createFrom().nullItem();
        }

        if (oidcEnabled()) {
            return oidc.authenticate(context, identityProviderManager)
                    .map(identity -> augmentIdentity(context, identity))
                    .onFailure().invoke(this::maybeLogAuthenticationFailure);
        }

        String clusterId = getClusterId(context);

        if (clusterId == null) {
            return Uni.createFrom().item(createAnonymousIdentity(null));
        }

        var ctx = contexts.get(clusterId);

        if (ctx == null) {
            // No Kafka context to establish identity, become anonymous
            return Uni.createFrom().item(createAnonymousIdentity(null));
        }

        String saslMechanism = ctx.saslMechanism(Admin.class);

        if (ctx.admin() != null || saslMechanism.isEmpty()) {
            // Admin credentials already given or there is no SASL authentication needed
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
                        Error error = category.createError("Authentication credentials missing or invalid", null, null);
                        var responseBody = new ErrorResponse(List.of(error));
                        ChallengeData challenge = new PayloadChallengeData(data, responseBody);
                        return challenge;
                    })
                    .onFailure().recoverWithItem(t -> {
                        var category = ErrorCategory.get(ErrorCategory.ServerError.class);
                        Error error = category.createError("Authentication failed due to internal server error", null, null);
                        var responseBody = new ErrorResponse(List.of(error));
                        ChallengeData challenge = new PayloadChallengeData(500, null, null, responseBody);
                        return challenge;
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

    private String getClusterId(RoutingContext context) {
        Pattern p = Pattern.compile("/api/kafkas/([^/]+)(?:/.*)?");
        Matcher m = p.matcher(context.normalizedPath());
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

    private String getAuthorizationScheme(String saslMechanism) {
        switch (saslMechanism) {
            case OAUTHBEARER:
                return BEARER.trim();
            case PLAIN, SCRAM_SHA256, SCRAM_SHA512:
                return BASIC.trim();
            default:
                return null;
        }
    }

    private SecurityIdentity createAnonymousIdentity(KafkaContext ctx) {
        return createIdentity(ctx, ANONYMOUS);
    }

    private SecurityIdentity augmentIdentity(RoutingContext context, SecurityIdentity identity) {
        if (identity != null) {
            String clusterId = getClusterId(context);
            var ctx = clusterId != null ? contexts.get(clusterId) : null;
            return createIdentity(ctx, identity);
        }
        throw new AuthenticationFailedException();
    }

    private SecurityIdentity createIdentity(KafkaContext ctx, SecurityIdentity source) {
        var builder = QuarkusSecurityIdentity.builder(source);
        addRoleChecker(ctx, builder, source.getPrincipal());
        return builder.build();
    }

    private SecurityIdentity createIdentity(KafkaContext ctx, MultiMap headers, String saslMechanism) {
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

    private SecurityIdentity createOAuthIdentity(KafkaContext ctx, MultiMap headers) {
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

    private SecurityIdentity createBasicIdentity(KafkaContext ctx, MultiMap headers, BiFunction<String, String, Credential> credentialBuilder) {
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

    private void addRoleChecker(KafkaContext ctx, QuarkusSecurityIdentity.Builder builder, Principal principal) {
        var globalSecurity = consoleConfig.getSecurity();
        Optional<SecurityConfig> clusterSecurity = ctx != null
                ? Optional.of(ctx.clusterConfig().getSecurity())
                : Optional.empty();

        var auditRules = mergeAuditRules(
            getAuditRules(globalSecurity.getAudit(), ""),
            clusterSecurity.map(c -> getAuditRules(c.getAudit(), "kafkas/" + ctx.clusterConfig().getName() + '/'))
                .orElseGet(Collections::emptyMap)
        );

        if (globalSecurity.getRoles().isEmpty()
                && clusterSecurity.map(cs -> cs.getRoles().isEmpty()).orElse(true)) {
            // No roles are defined - allow everything
            builder.addPermissionChecker(requiredPermission -> {
                auditLog(principal, requiredPermission, true, auditRules.get(requiredPermission));
                return Uni.createFrom().item(true);
            });

            return;
        }

        Stream<SubjectConfig> globalSubjects = globalSecurity.getSubjects().stream();
        Stream<SubjectConfig> clusterSubjects = clusterSecurity.map(cs -> cs.getSubjects().stream())
                .orElseGet(Stream::empty);

        List<String> roleNames = Stream.concat(clusterSubjects, globalSubjects)
                .filter(sub -> matchesPrincipal(sub, principal))
                .flatMap(sub -> sub.getRoleNames().stream())
                .distinct()
                .toList();

        Stream<Permission> globalPermissions = getPermissions(globalSecurity, roleNames, "");
        Stream<Permission> clusterPermissions = clusterSecurity
                .map(cs -> getPermissions(cs, roleNames, "kafkas/" + ctx.clusterConfig().getName() + '/'))
                .orElseGet(Stream::empty);

        List<Permission> possessedPermissions = Stream.concat(globalPermissions, clusterPermissions).toList();

        builder.addPermissionChecker(requiredPermission -> {
            boolean allowed = possessedPermissions
                    .stream()
                    .anyMatch(possessed -> possessed.implies(requiredPermission));

            auditLog(principal, requiredPermission, allowed, auditRules.get(requiredPermission));
            return Uni.createFrom().item(allowed);
        });
    }

    private void auditLog(Principal principal, Permission required, boolean allowed, Decision audit) {
        if (audit != null && audit.logResult(allowed)) {
            log.infof("%s %s %s", principal.getName(), allowed ? "allowed" : "denied", required);
        } else {
            log.tracef("%s %s %s", principal.getName(), allowed ? "allowed" : "denied", required);
        }
    }

    private void maybeLogAuthenticationFailure(Throwable t) {
        if (t.getCause() instanceof org.jose4j.jwt.consumer.InvalidJwtException ije) {
            log.debugf("Invalid JWT: %s", ije.getErrorDetails());
        }
    }

    private boolean matchesPrincipal(SubjectConfig subjectConfig, Principal principal) {
        String claimName = subjectConfig.getClaim();
        List<String> include = subjectConfig.getInclude();

        if (claimName == null) {
            return include.contains(principal.getName());
        } else if (principal instanceof JsonWebToken jwt) {
            Object claim = jwt.getClaim(claimName);

            if (claim instanceof String) {
                return include.contains(claim);
            }

            // array claim, like set/list of groups
            if (claim instanceof Collection<?> values) {
                for (Object value : values) {
                    if (include.contains(value)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private Stream<Permission> getPermissions(SecurityConfig security, Collection<String> roleNames, String resourcePrefix) {
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

    private Map<Permission, Decision> mergeAuditRules(Map<Permission, Decision> global, Map<Permission, Decision> cluster) {
        return Stream.concat(global.entrySet().stream(), cluster.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<Permission, Decision> getAuditRules(List<AuditConfig> audits, String resourcePrefix) {
        return audits.stream().flatMap(rule -> {
            Map<ConsolePermission, Decision> auditRules = new HashMap<>();
            Set<Privilege> actions = rule.getPrivileges().stream().flatMap(p -> p.expand().stream()).collect(Collectors.toSet());

            for (var action : actions) {
                for (var resource : rule.getResources()) {
                    if (rule.getResourceNames().isEmpty()) {
                        auditRules.put(
                                new ConsolePermission(
                                        resourcePrefix + resource,
                                        Collections.emptySet(),
                                        action),
                                rule.getDecision());
                    } else {
                        for (String name : rule.getResourceNames()) {
                            auditRules.put(
                                    new ConsolePermission(
                                            resourcePrefix + resource,
                                            Collections.singleton(name),
                                            action),
                                    rule.getDecision());
                        }
                    }
                }
            }

            return auditRules.entrySet().stream();
        }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Optional<String[]> getBasicAuthentication(MultiMap headers) {
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

    private Optional<String> getAuthorization(MultiMap headers, String scheme) {
        return Optional.ofNullable(headers.get(HttpHeaders.AUTHORIZATION))
            .filter(header -> header.regionMatches(true, 0, scheme, 0, scheme.length()))
            .map(header -> header.substring(scheme.length()));
    }

    private static class PayloadChallengeData extends ChallengeData {
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
