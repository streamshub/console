package com.github.streamshub.console.api.security;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import com.github.streamshub.console.api.model.jsonapi.JsonApiBase;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.config.security.ResourceTypes;

import io.quarkus.security.identity.SecurityIdentity;

@RequestScoped
public class PermissionService {

    private static final Set<String> KAFKA_SUBRESOURCES = Stream.of(ResourceTypes.Kafka.values())
            .map(v -> v.value())
            .collect(Collectors.toSet());

    @Inject
    SecurityIdentity securityIdentity;

    @Inject
    Map<String, KafkaContext> contexts;

    @Inject
    KafkaContext kafkaContext;

    private String resolveResource(String resource) {
        if (KAFKA_SUBRESOURCES.contains(resource)) {
            resource = kafkaContext.securityResourcePath(resource);
        }
        return resource;
    }

    private String resolveResourceAudit(String resource) {
        if (KAFKA_SUBRESOURCES.contains(resource)) {
            resource = kafkaContext.auditDisplayResourcePath(resource);
        }
        return resource;
    }

    private String resolveResourceName(String resource, String name) {
        if (ResourceTypes.Global.KAFKAS.value().equals(resource)) {
            /*
             * Request-scoped context may not be available (e.g. listing /api/kafkas),
             * so find the correct context from the global map using the name, which
             * should be the Kafka cluster's ID.
             */
            var ctx = contexts.get(name);

            if (ctx != null) {
                name = ctx.clusterConfig().clusterKey();
            }
        }
        return name;
    }

    private boolean checkPermission(ConsolePermission required) {
        return securityIdentity.checkPermission(required)
                .subscribeAsCompletionStage()
                .join();
    }

    private ConsolePermissionRequired newRequiredPermission(ResourceTypes.ResourceType<?> resource, Privilege privilege) {
        return new ConsolePermissionRequired(
                resolveResource(resource.value()),
                resolveResourceAudit(resource.value()),
                privilege);
    }

    public <T> Predicate<T> permitted(ResourceTypes.ResourceType<?> resource, Privilege privilege, Function<T, String> nameSource) {
        ConsolePermissionRequired required = newRequiredPermission(resource, privilege);

        return (T item) -> {
            String itemName = nameSource.apply(item);
            required.setResourceName(itemName, resolveResourceName(resource.value(), itemName));
            return checkPermission(required);
        };
    }

    public boolean permitted(ResourceTypes.ResourceType<?> resource, Privilege privilege, String name) {
        ConsolePermissionRequired required = newRequiredPermission(resource, privilege);

        if (name != null) {
            required.setResourceName(name, resolveResourceName(resource.value(), name));
        }

        return checkPermission(required);
    }

    public void assertPermitted(ResourceTypes.ResourceType<?> resource, Privilege privilege, String name) {
        if (!permitted(resource, privilege, name)) {
            throw forbidden(resource, privilege, name);
        }
    }

    public ForbiddenException forbidden(ResourceTypes.ResourceType<?> resource, Privilege privilege, String name) {
        return new ForbiddenException("Access denied: resource={%s} privilege:{%s}, resourceName:{%s}"
                .formatted(resource.value(), privilege, name));
    }

    private Set<Privilege> getPrivileges(ResourceTypes.ResourceType<?> resource, String name) {
        Set<Privilege> possessed = new LinkedHashSet<>();

        for (var privilege : Privilege.ALL.expand()) {
            ConsolePermissionRequired required = newRequiredPermission(resource, privilege);
            required.setResourceName(name, null);
            required.setAudited(false);

            if (checkPermission(required)) {
                possessed.add(privilege);
            }
        }

        return possessed;
    }

    public <T extends JsonApiBase> UnaryOperator<T> addPrivileges(ResourceTypes.ResourceType<?> resource, Function<T, String> nameSource) {
        return (T item) -> addPrivileges(item, resource, nameSource.apply(item));
    }

    public void addPrivileges(Map<String, Object> meta, ResourceTypes.ResourceType<?> resource, String name) {
        meta.put("privileges", getPrivileges(resource, name));
    }

    private <T extends JsonApiBase> T addPrivileges(T item, ResourceTypes.ResourceType<?> resource, String name) {
        item.addMeta("privileges", getPrivileges(resource, name));
        return item;
    }
}
