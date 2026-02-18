package com.github.streamshub.console.api.security;

import java.security.Permission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.security.AuditConfig;
import com.github.streamshub.console.config.security.Decision;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.config.security.ResourceTypes;

@ApplicationScoped
class PermissionCache {

    @Inject
    Logger log;

    @Inject
    @ConfigProperty(name = "console.security.cache-permissions", defaultValue = "true")
    boolean cachePermissions;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    Map<String, KafkaContext> contexts;


    Map<String, List<Permission>> permissionsCache;
    Map<Permission, Decision> auditRulesCache;

    @PostConstruct
    void initialize() {
        if (cachePermissions) {
            permissionsCache = buildPermissions();
            auditRulesCache = buildAuditRules();
        }
    }

    Map<String, List<Permission>> getPermissions() {
        if (cachePermissions) {
            return permissionsCache;
        }
        return buildPermissions();
    }

    Map<Permission, Decision> getAuditRules() {
        if (cachePermissions) {
            return auditRulesCache;
        }
        return buildAuditRules();
    }

    private Map<String, List<Permission>> buildPermissions() {
        Map<String, List<Permission>> permissions = new HashMap<>();

        buildGlobalPermissions(permissions);

        for (var kafkaCluster : consoleConfig.getKafka().getClusters()) {
            contexts.values()
                .stream()
                .filter(ctx -> ctx.clusterConfig().equals(kafkaCluster))
                .findFirst()
                .ifPresentOrElse(
                    context -> buildKafkaPermissions(permissions, context),
                    () -> log.debugf(
                            "No KafkaContext found for cluster %s, permissions will not be processed",
                            kafkaCluster.clusterKey()));
        }

        return permissions;
    }

    private void buildGlobalPermissions(Map<String, List<Permission>> permissions) {
        for (var role : consoleConfig.getSecurity().getRoles()) {
            String roleName = role.getName();

            for (var rule : role.getRules()) {
                List<Permission> rulePermissions = new ArrayList<>();
                Privilege[] actions = rule.getPrivileges().toArray(Privilege[]::new);

                for (var resource : rule.getResources()) {
                    Collection<String> resourceNames = rule.getResourceNames();

                    if ("kafkas".equals(resource)) {
                        resourceNames = mapKafkaClusterIds(resourceNames);
                    }

                    rulePermissions.add(new ConsolePermissionPossessed(resource, resourceNames, actions));
                }

                permissions.computeIfAbsent(roleName, k -> new ArrayList<>()).addAll(rulePermissions);
            }
        }
    }

    private void buildKafkaPermissions(Map<String, List<Permission>> permissions, KafkaContext kafkaContext) {
        var kafkaCluster = kafkaContext.clusterConfig();

        for (var role : kafkaCluster.getSecurity().getRoles()) {
            String roleName = role.getName();

            for (var rule : role.getRules()) {
                List<Permission> rulePermissions = new ArrayList<>();
                Privilege[] actions = rule.getPrivileges().toArray(Privilege[]::new);

                for (var resource : rule.getResources()) {
                    if ("consumerGroups".equals(resource)) {
                        log.warnf("Kafka cluster %s role %s uses deprecated resource `consumerGroups`. Use `groups` instead.",
                                kafkaCluster.getName(),
                                roleName);
                        resource = ResourceTypes.Kafka.GROUPS.value();
                    }

                    Collection<String> resourceNames = rule.getResourceNames();
                    rulePermissions.add(new ConsolePermissionPossessed(
                            kafkaContext.securityResourcePath(resource),
                            resourceNames,
                            actions));
                }

                permissions.computeIfAbsent(roleName, k -> new ArrayList<>()).addAll(rulePermissions);
            }
        }
    }

    private Map<Permission, Decision> buildAuditRules() {
        Map<Permission, Decision> auditRules = new HashMap<>();

        buildGlobalAuditRules(auditRules);

        for (var kafkaCluster : consoleConfig.getKafka().getClusters()) {
            contexts.values()
                .stream()
                .filter(ctx -> ctx.clusterConfig().equals(kafkaCluster))
                .findFirst()
                .ifPresentOrElse(
                    context -> buildKafkaAuditRules(auditRules, context),
                    () -> log.debugf(
                            "No KafkaContext found for cluster %s, audit rules will not be processed",
                            kafkaCluster.clusterKey()));
        }

        return auditRules;
    }

    private void buildGlobalAuditRules(Map<Permission, Decision> auditRules) {
        for (var auditRule : consoleConfig.getSecurity().getAudit()) {
            Set<Privilege> actions = auditActions(auditRule);
            Collection<String> resourceNames = auditRule.getResourceNames();

            for (var resource : auditRule.getResources()) {
                if ("kafkas".equals(resource)) {
                    resourceNames = mapKafkaClusterIds(resourceNames);
                }

                buildAuditRules(auditRules, resource, resourceNames, actions, auditRule.getDecision());
            }
        }
    }

    private void buildKafkaAuditRules(Map<Permission, Decision> auditRules, KafkaContext kafkaContext) {
        var kafkaCluster = kafkaContext.clusterConfig();

        for (var auditRule : kafkaCluster.getSecurity().getAudit()) {
            Set<Privilege> actions = auditActions(auditRule);
            Collection<String> resourceNames = auditRule.getResourceNames();

            for (var resource : auditRule.getResources()) {
                resource = kafkaContext.securityResourcePath(resource);

                buildAuditRules(auditRules, resource, resourceNames, actions, auditRule.getDecision());
            }
        }
    }

    private void buildAuditRules(Map<Permission, Decision> auditRules,
            String resource, Collection<String> resourceNames, Set<Privilege> actions, Decision decision) {

        for (var action : actions) {
            if (resourceNames.isEmpty()) {
                auditRules.put(new ConsolePermissionPossessed(resource, Collections.emptySet(), action), decision);
            } else {
                for (String name : resourceNames) {
                    auditRules.put(new ConsolePermissionPossessed(resource, List.of(name), action), decision);
                }
            }
        }
    }

    private Set<Privilege> auditActions(AuditConfig auditRule) {
        return auditRule.getPrivileges().stream().flatMap(p -> p.expand().stream()).collect(Collectors.toSet());
    }

    private Collection<String> mapKafkaClusterIds(Collection<String> kafkaClusterNames) {
        if (kafkaClusterNames.isEmpty()) {
            return kafkaClusterNames;
        }

        Map<String, String> clusterKeyIds = contexts.values()
                .stream()
                .collect(Collectors.toMap(c -> c.clusterConfig().clusterKey(), KafkaContext::clusterId));
        Map<String, List<String>> clusterNameIds = contexts.values()
                .stream()
                .collect(Collectors.groupingBy(
                    c -> c.clusterConfig().getName(),
                    Collectors.mapping(KafkaContext::clusterId, Collectors.toList())
                ));

        Set<String> updatedNames = new LinkedHashSet<>(kafkaClusterNames.size());

        for (String name : kafkaClusterNames) {
            if ("*".equals(name)) {
                updatedNames.addAll(mapKafkaClusterIdWildcard(clusterKeyIds));
            } else if (ConsolePermissionPossessed.isDelimitedRegex(name)) {
                updatedNames.addAll(mapKafkaClusterIdRegex(name, clusterKeyIds));
            } else {
                updatedNames.addAll(mapKafkaClusterIdLiteral(name, clusterKeyIds, clusterNameIds));
            }
        }

        return updatedNames;
    }

    private Collection<String> mapKafkaClusterIdWildcard(Map<String, String> clusterKeyIds) {
        Collection<String> allClusterIds = clusterKeyIds.values();

        if (log.isDebugEnabled()) {
            log.debugf("Expanding wildcard to include all Kafka clusters: %s", allClusterIds);
        }

        return allClusterIds;
    }

    private Collection<String> mapKafkaClusterIdRegex(String name, Map<String, String> clusterKeyIds) {
        Pattern pattern = Pattern.compile(name.substring(1, name.length() - 1));
        List<String> matchedClusterIds = clusterKeyIds.entrySet()
            .stream()
            .filter(entry -> {
                String clusterKey = entry.getKey();
                boolean matched = pattern.matcher(clusterKey).matches();
                if (log.isDebugEnabled()) {
                    String matchMsg = matched ? "matched" : "did not match";
                    log.debugf("Cluster key %s %s regex pattern %s", clusterKey, matchMsg, pattern);
                }
                return matched;
            })
            .map(Map.Entry::getValue)
            .toList();

        if (log.isDebugEnabled()) {
            log.debugf("Expanding regex %s to include Kafka clusters: %s", name, matchedClusterIds);
        }

        return matchedClusterIds.isEmpty() ? Collections.singleton(ConsolePermission.UNMATCHABLE) : matchedClusterIds;
    }

    private Collection<String> mapKafkaClusterIdLiteral(String name, Map<String, String> clusterKeyIds, Map<String, List<String>> clusterNameIds) {
        var clusterIdFromKey = clusterKeyIds.get(name);
        var clusterIdsFromName = clusterNameIds.get(name);

        if (clusterIdFromKey != null) {
            return Collections.singleton(clusterIdFromKey);
        } else if (clusterIdsFromName != null) {
            if (clusterIdsFromName.size() == 1) {
                /*
                 * When we can determine a single cluster using a name (without namespace),
                 * use it without logging anything. This is the backward-compatible behavior.
                 */
                return Collections.singleton(clusterIdsFromName.get(0));
            } else {
                log.warnf("""
                        Kafka cluster '%s' in security rule resolves to multiple cluster \
                        configurations. Clusters with a namespace must prefix the name \
                        with the namespace value. Possible Kafka clusters: %s""",
                        name,
                        clusterIdsFromName);
            }
        } else {
            log.warnf("""
                    Unknown Kafka cluster '%s' in security rule. \
                    Clusters with a namespace must prefix the name with the namespace \
                    value. Known Kafka clusters: %s""", name, clusterKeyIds.keySet());
        }

        return Collections.singleton(ConsolePermission.UNMATCHABLE);
    }
}
