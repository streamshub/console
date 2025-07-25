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

                    rulePermissions.add(new ConsolePermission(resource, resourceNames, actions));
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
                    Collection<String> resourceNames = rule.getResourceNames();
                    rulePermissions.add(new ConsolePermission(
                            kafkaContext.securityResourcePath(resource),
                            kafkaContext.auditDisplayResourcePath(resource),
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

            for (var action : actions) {
                for (var resource : auditRule.getResources()) {
                    Collection<String> resourceNames = auditRule.getResourceNames();

                    if (resourceNames.isEmpty()) {
                        auditRules.put(
                                new ConsolePermission(
                                        resource,
                                        Collections.emptySet(),
                                        action),
                                auditRule.getDecision());
                    } else {
                        if ("kafkas".equals(resource)) {
                            resourceNames = mapKafkaClusterIds(resourceNames);
                        }

                        for (String name : resourceNames) {
                            auditRules.put(
                                    new ConsolePermission(
                                            resource,
                                            List.of(name),
                                            action),
                                    auditRule.getDecision());
                        }
                    }
                }
            }
        }
    }

    private void buildKafkaAuditRules(Map<Permission, Decision> auditRules, KafkaContext kafkaContext) {
        var kafkaCluster = kafkaContext.clusterConfig();

        for (var auditRule : kafkaCluster.getSecurity().getAudit()) {
            Set<Privilege> actions = auditActions(auditRule);

            for (var action : actions) {
                for (var resource : auditRule.getResources()) {
                    String securityPath = kafkaContext.securityResourcePath(resource);

                    if (auditRule.getResourceNames().isEmpty()) {
                        auditRules.put(
                                new ConsolePermission(securityPath, Collections.emptySet(), action),
                                auditRule.getDecision());
                    } else {
                        for (String name : auditRule.getResourceNames()) {
                            auditRules.put(
                                    new ConsolePermission(securityPath, List.of(name), action),
                                    auditRule.getDecision());
                        }
                    }
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
            var clusterIdFromKey = clusterKeyIds.get(name);
            var clusterIdsFromName = clusterNameIds.get(name);

            if (clusterIdFromKey != null) {
                updatedNames.add(clusterIdFromKey);
            } else if (clusterIdsFromName != null) {
                if (clusterIdsFromName.size() == 1) {
                    /*
                     * When we can determine a single cluster using a name (without namespace),
                     * use it without logging anything. This is the backward-compatible behavior.
                     */
                    updatedNames.add(clusterIdsFromName.get(0));
                } else {
                    log.warnf("""
                            Kafka cluster '%' in security rule resolves to multiple cluster \
                            configurations. Clusters with a namespace must prefix the name \
                            with the namespace value. Possible Kafka clusters: %s""",
                            name,
                            clusterIdsFromName);
                }
            } else {
                log.warnf("""
                        Unknown Kafka cluster '%' in security rule. \
                        Clusters with a namespace must prefix the name with the namespace \
                        value. Known Kafka clusters: %s""", name, clusterKeyIds.keySet());
            }
        }

        return updatedNames;
    }
}
