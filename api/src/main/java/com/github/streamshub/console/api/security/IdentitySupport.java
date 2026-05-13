package com.github.streamshub.console.api.security;

import java.net.URI;
import java.security.Permission;
import java.security.Principal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.JsonString;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.config.security.Decision;
import com.github.streamshub.console.config.security.KafkaSecurityConfig;
import com.github.streamshub.console.config.security.SubjectConfig;

import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.runtime.OidcClientConfig.Grant;
import io.quarkus.security.AuthenticationFailedException;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;

@ApplicationScoped
public class IdentitySupport {

    public static final String OAUTHBEARER = OAuthBearerLoginModule.OAUTHBEARER_MECHANISM;
    public static final String PLAIN = "PLAIN";
    public static final String SCRAM_SHA256 = "SCRAM-SHA-256";
    public static final String SCRAM_SHA512 = "SCRAM-SHA-512";

    public static final String FULL_NAME = "fullName";

    private static final SecurityIdentity ANONYMOUS = QuarkusSecurityIdentity.builder()
            .setAnonymous(true)
            .setPrincipal(new QuarkusPrincipal("ANONYMOUS"))
            .addAttribute(FULL_NAME, "Anonymous")
            .build();

    public static final String AUDIT_LOG_NAME = "io.streamshub.console.security.audit";
    private static final Logger AUDIT = Logger.getLogger(AUDIT_LOG_NAME);

    @Inject
    OidcClients oidcClients;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    Map<String, KafkaContext> contexts;

    @Inject
    PermissionCache permissionCache;

    public SecurityIdentity createAnonymousIdentity(KafkaContext ctx) {
        return createIdentity(ctx, ANONYMOUS);
    }

    public SecurityIdentity augmentIdentity(RoutingContext context, SecurityIdentity identity) {
        if (identity != null) {
            String clusterId = getClusterId(context);
            var ctx = clusterId != null ? contexts.get(clusterId) : null;
            return createIdentity(ctx, identity);
        }
        throw new AuthenticationFailedException("No identity found");
    }

    private String getClusterId(RoutingContext context) {
        Pattern p = Pattern.compile("/api/kafkas/([^/]+)(?:/.*)?");
        Matcher m = p.matcher(context.normalizedPath());
        if (m.matches()) {
            return m.group(1);
        }
        return null;
    }

    private SecurityIdentity createIdentity(KafkaContext ctx, SecurityIdentity source) {
        var builder = QuarkusSecurityIdentity.builder(source);
        Principal principal = Optional.ofNullable(source.getAttribute("principal.name"))
                .map(String.class::cast)
                .map(QuarkusPrincipal::new)
                .map(Principal.class::cast)
                .orElseGet(source::getPrincipal);

        builder.setPrincipal(principal);
        addRoleChecker(ctx, builder, principal);
        return builder.build();
    }

    public Uni<SaslJaasConfigCredential> createCredential(KafkaContext kafkaContext, UsernamePasswordAuthenticationRequest request) {
        switch (kafkaContext.saslMechanism(Admin.class)) {
            case OAUTHBEARER:
                return createOAuthCredential(kafkaContext, request);
            case PLAIN:
                return Uni.createFrom().item(SaslJaasConfigCredential.forPlainLogin(
                        request.getUsername(),
                        new String(request.getPassword().getPassword())
                ));
            case SCRAM_SHA256, SCRAM_SHA512:
                return Uni.createFrom().item(SaslJaasConfigCredential.forScramLogin(
                        request.getUsername(),
                        new String(request.getPassword().getPassword())
                ));
            default:
                return null;
        }
    }

    private Uni<SaslJaasConfigCredential> createOAuthCredential(
            KafkaContext kafkaContext,
            UsernamePasswordAuthenticationRequest request) {

        var tokenUrl = kafkaContext.tokenUrl()
                .orElseThrow(() -> new IllegalStateException("Token endpoint URL not available"));

        var tokenPath = URI.create(tokenUrl).getPath();

        if (tokenPath.isEmpty()) {
            throw new IllegalArgumentException("Token endpoint URL missing path: " + tokenUrl);
        }

        var baseUrl = tokenUrl.substring(0, tokenUrl.indexOf(tokenPath));

        // Build OIDC client config with user-provided credentials
        var config = io.quarkus.oidc.client.runtime.OidcClientConfig.builder()
                .id("streamshub-console-kafka-oidc-client-" + kafkaContext.clusterId())
                .authServerUrl(baseUrl)
                .discoveryEnabled(false)
                .tokenPath(tokenPath)
                .clientId(request.getUsername())
                .credentials()
                    .clientSecret(new String(request.getPassword().getPassword()))
                    .end()
                .grant(Grant.Type.CLIENT)
                .build();

        return oidcClients.newClient(config)
                .onItem()
                .transformToUni(client -> client.getTokens(Collections.emptyMap()))
                .onItem()
                // Create SASL/OAUTHBEARER credential with the access token
                .transform(tokens -> SaslJaasConfigCredential.forOAuthLogin(tokens.getAccessToken()));
    }

    private void addRoleChecker(KafkaContext ctx, QuarkusSecurityIdentity.Builder builder, Principal principal) {
        var applicationPermissions = permissionCache.getPermissions();
        var auditRules = permissionCache.getAuditRules();

        if (applicationPermissions.isEmpty()) {
            // No roles are defined - allow everything
            builder.addPermissionChecker(requiredPermission -> {
                auditLog(principal, requiredPermission, true, auditRules);
                return Uni.createFrom().item(true);
            });

            return;
        }

        var roleNames = getPrincipalRoles(principal, ctx);

        List<Permission> possessedPermissions = applicationPermissions
                .entrySet()
                .stream()
                .filter(entry -> roleNames.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .flatMap(List::stream)
                .toList();

        builder.addPermissionChecker(requiredPermission -> {
            var grantingPermission = possessedPermissions
                    .stream()
                    .filter(possessed -> possessed.implies(requiredPermission))
                    .findFirst();

            boolean allowed = grantingPermission.isPresent();

            auditLog(principal, requiredPermission, allowed, auditRules);
            return Uni.createFrom().item(allowed);
        });
    }

    private Set<String> getPrincipalRoles(Principal principal, KafkaContext ctx) {
        Stream<SubjectConfig> globalSubjects = consoleConfig
                .getSecurity()
                .getSubjects()
                .stream();

        Stream<SubjectConfig> clusterSubjects = Optional
                .ofNullable(ctx)
                .map(KafkaContext::clusterConfig)
                .map(KafkaClusterConfig::getSecurity)
                .map(KafkaSecurityConfig::getSubjects)
                .map(Collection::stream)
                .orElseGet(Stream::empty);

        return Stream.concat(clusterSubjects, globalSubjects)
                .filter(sub -> matchesPrincipal(sub, principal))
                .flatMap(sub -> sub.getRoleNames().stream())
                .distinct()
                .collect(Collectors.toSet());
    }

    private void auditLog(Principal principal, Permission required, boolean allowed, Map<Permission, Decision> auditRules) {
        if (required instanceof ConsolePermissionRequired consoleRequired && !consoleRequired.isAudited()) {
            return;
        }

        for (Map.Entry<Permission, Decision> entry : auditRules.entrySet()) {
            if (entry.getValue().logResult(allowed) && entry.getKey().implies(required)) {
                AUDIT.infof("%s %s %s", principal.getName(), allowed ? "allowed" : "denied", required);
                return;
            }
        }

        AUDIT.tracef("%s %s %s", principal.getName(), allowed ? "allowed" : "denied", required);
    }

    private boolean matchesPrincipal(SubjectConfig subjectConfig, Principal principal) {
        String claimName = subjectConfig.getClaim();
        List<String> include = subjectConfig.getInclude();

        if (claimName == null) {
            return include.contains(principal.getName());
        } else if (principal instanceof JsonWebToken jwt) {
            Object claim = jwt.getClaim(claimName);

            // array claim, like set/list of groups
            if (claim instanceof Collection<?> values) {
                for (Object value : values) {
                    if (isIncluded(include, value)) {
                        return true;
                    }
                }
            } else {
                return isIncluded(include, claim);
            }
        }

        return false;
    }

    private static boolean isIncluded(List<String> include, Object value) {
        if (value instanceof JsonString jsonString) {
            value = jsonString.getString();
        }

        return value instanceof String && include.contains(value);
    }
}
