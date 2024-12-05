package com.github.streamshub.console.api.security;

import java.util.List;
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
    KafkaContext kafkaContext;

    private String resolveResource(String resource) {
        if (KAFKA_SUBRESOURCES.contains(resource)) {
            resource = "kafkas/" + kafkaContext.clusterConfig().getName() + '/' + resource;
        }
        return resource;
    }

    private boolean checkPermission(ConsolePermission required) {
        return securityIdentity.checkPermission(required)
                .subscribeAsCompletionStage()
                .join();
    }

    public <T> Predicate<T> permitted(String resource, Privilege privilege, Function<T, String> name) {
        ConsolePermission required = new ConsolePermission(resolveResource(resource), privilege);

        return (T item) -> {
            required.resourceName(name.apply(item));
            return checkPermission(required);
        };
    }

    public boolean permitted(String resource, Privilege privilege, String name) {
        return checkPermission(new ConsolePermission(resolveResource(resource), List.of(name), privilege));
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
