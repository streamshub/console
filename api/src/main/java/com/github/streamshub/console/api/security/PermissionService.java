package com.github.streamshub.console.api.security;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

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

    public <T> Predicate<T> permitted(String resource, Privilege privilege, Function<T, String> nameSource) {
        ConsolePermission required = new ConsolePermission(
                resolveResource(resource),
                resolveResourceAudit(resource),
                Collections.emptyList(),
                privilege);

        return (T item) -> {
            String itemName = nameSource.apply(item);
            required.resourceName(itemName);
            required.resourceNamesDisplay(List.of(resolveResourceName(resource, itemName)));
            return checkPermission(required);
        };
    }

    public boolean permitted(String resource, Privilege privilege, String name) {
        return checkPermission(new ConsolePermission(
                resolveResource(resource),
                resolveResourceAudit(resource),
                List.of(name),
                privilege)
                .resourceNamesDisplay(List.of(resolveResourceName(resource, name))));
    }

    public void assertPermitted(String resource, Privilege privilege, String name) {
        if (!permitted(resource, privilege, name)) {
            throw forbidden(resource, privilege, name);
        }
    }

    public ForbiddenException forbidden(String resource, Privilege privilege, String name) {
        return new ForbiddenException("Access denied: resource={%s} privilege:{%s}, resourceName:{%s}"
                .formatted(resource, privilege, name));
    }
}
