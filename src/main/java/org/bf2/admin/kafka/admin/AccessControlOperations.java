package org.bf2.admin.kafka.admin;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.acl.AclBinding;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import org.bf2.admin.kafka.admin.model.AdminServerException;
import org.bf2.admin.kafka.admin.model.ErrorType;
import org.bf2.admin.kafka.admin.model.Types;
import org.bf2.admin.kafka.admin.model.Types.PagedResponse;
import org.bf2.admin.kafka.admin.model.Types.SortDirectionEnum;
import org.jboss.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class AccessControlOperations {

    public static final String INVALID_ACL_RESOURCE_OPERATION = "Invalid ACL binding resourceType or operation";

    private static final Logger log = Logger.getLogger(AccessControlOperations.class);
    private static final String WILDCARD_PRINCIPAL = KafkaPrincipal.USER_TYPE + ":*";

    private static final TypeReference<Map<String, List<String>>> TYPEREF_MAP_LIST_STRING =
        new TypeReference<>() {
            // Intentionally blank
        };

    public static final TypeReference<Types.AclBinding> TYPEREF_ACL_BINDING =
        new TypeReference<>() {
            // Intentionally blank
        };

    public static final Map<String, Function<AclBinding, String>> SORT_KEYS;

    static {
        Map<String, Function<AclBinding, String>> sortKeys = new LinkedHashMap<>();
        // Entries added in order of default column sorting priority
        sortKeys.put(Types.AclBinding.PROP_PERMISSION, binding -> binding.entry().permissionType().name());
        sortKeys.put(Types.AclBinding.PROP_PRINCIPAL, binding -> binding.entry().principal());
        sortKeys.put(Types.AclBinding.PROP_RESOURCE_TYPE, binding -> binding.pattern().resourceType().name());
        sortKeys.put(Types.AclBinding.PROP_RESOURCE_NAME, binding -> binding.pattern().name());
        sortKeys.put(Types.AclBinding.PROP_PATTERN_TYPE, binding -> binding.pattern().patternType().name());
        sortKeys.put(Types.AclBinding.PROP_OPERATION, binding -> binding.entry().operation().name());
        SORT_KEYS = Collections.unmodifiableMap(sortKeys);
    }

    @Inject
    KafkaAdminConfigRetriever config;

    private Map<String, List<String>> resourceOperations;

    static class AccessControlOperationException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        AccessControlOperationException(Throwable cause) {
            super(cause);
        }
    }

    @PostConstruct
    public void initialize() {
        try {
            this.resourceOperations = new ObjectMapper().readValue(config.getAclResourceOperations(), TYPEREF_MAP_LIST_STRING);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse value of ACL resource-operations", e);
            throw new AccessControlOperationException(e);
        }
    }

    public CompletionStage<Void> createAcl(Admin client, Types.AclBinding binding) {
        if (!validAclBinding(binding)) {
            return CompletableFuture.failedStage(new AdminServerException(ErrorType.INVALID_ACL_RESOURCE_OP));
        }

        Promise<Void> promise = Promise.promise();

        client.createAcls(List.of(binding.toKafkaBinding()))
            .all()
            .whenComplete((nothing, exception) -> {
                if (exception != null) {
                    promise.fail(exception);
                } else {
                    promise.complete();
                }
            });

        return promise.future().toCompletionStage();
    }

    public CompletionStage<PagedResponse<Types.AclBinding>> getAcls(Admin client,
                        Types.AclBindingFilterParams filter,
                        Types.PageRequest pageRequest,
                        Types.AclBindingSortParams sortOrder) {

        Promise<PagedResponse<Types.AclBinding>> promise = Promise.promise();
        var pendingResults = new ArrayList<KafkaFuture<Collection<AclBinding>>>(2);

        pendingResults.add(client.describeAcls(filter.toKafkaBindingFilter()).values());

        if (!filter.getPrincipal().isBlank() && !WILDCARD_PRINCIPAL.equals(filter.getPrincipal())) {
            // Include results that apply for "all principals"
            filter.setPrincipal(WILDCARD_PRINCIPAL);
            pendingResults.add(client.describeAcls(filter.toKafkaBindingFilter()).values());
        }

        KafkaFuture.allOf(pendingResults.toArray(KafkaFuture[]::new))
            .whenComplete((nothing, error) ->
                collectBindings(pendingResults, sortOrder, error)
                    .map(bindings -> PagedResponse.forPage(pageRequest, Types.AclBinding.class, bindings))
                    .onFailure(promise::fail)
                    .onSuccess(promise::complete));

        return promise.future().toCompletionStage();
    }

    public CompletionStage<PagedResponse<Types.AclBinding>> deleteAcls(Admin client,
                           Types.AclBindingFilterParams filter) {

        Promise<PagedResponse<Types.AclBinding>> promise = Promise.promise();

        client.deleteAcls(List.of(filter.toKafkaBindingFilter()))
            .all()
            .whenComplete((bindingCollection, error) ->
                collectBindings(bindingCollection, error)
                    .map(bindings -> PagedResponse.forItems(Types.AclBinding.class, bindings))
                    .onFailure(promise::fail)
                    .onSuccess(promise::complete));

        return promise.future().toCompletionStage();
    }

    private boolean validAclBinding(Types.AclBinding binding) {
        return resourceOperations.getOrDefault(binding.getResourceType().name().toLowerCase(Locale.ENGLISH),
                                               Collections.emptyList())
                .contains(binding.getOperation().name().toLowerCase(Locale.ENGLISH));
    }

    static Future<List<Types.AclBinding>> collectBindings(Collection<AclBinding> bindings, Throwable error) {
        Promise<List<Types.AclBinding>> promise = Promise.promise();
        if (error == null) {
            promise.complete(bindings.stream().map(Types.AclBinding::fromKafkaBinding).collect(Collectors.toList()));
        } else {
            promise.fail(error);
        }
        return promise.future();
    }

    static Future<List<Types.AclBinding>> collectBindings(List<KafkaFuture<Collection<AclBinding>>> pendingResults,
                                                          Types.AclBindingSortParams sortOrder, Throwable error) {
        Promise<List<Types.AclBinding>> promise = Promise.promise();

        if (error == null) {
            promise.complete(pendingResults.stream()
                .map(kafkaFuture -> {
                    // The future is already completed, nothing _should_ be thrown
                    try {
                        return kafkaFuture.get();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AccessControlOperationException(e);
                    } catch (Exception e) {
                        throw new AccessControlOperationException(e);
                    }
                })
                .flatMap(Collection::stream)
                .sorted(getComparator(sortOrder))
                .map(Types.AclBinding::fromKafkaBinding)
                .collect(Collectors.toList()));
        } else {
            promise.fail(error);
        }

        return promise.future();
    }

    static Comparator<AclBinding> getComparator(Types.AclBindingSortParams sortOrder) {
        Map<String, Function<AclBinding, String>> sortKeys = new LinkedHashMap<>(SORT_KEYS);
        Comparator<AclBinding> comparator;
        String requestedSortKey = sortOrder.getField().getValue();

        if (sortKeys.containsKey(requestedSortKey)) {
            comparator = Comparator.comparing(sortKeys.remove(requestedSortKey));
        } else {
            comparator = Comparator.comparing(sortKeys.remove(Types.AclBinding.PROP_PERMISSION));
        }

        if (sortOrder.getOrder() == SortDirectionEnum.DESC) {
            comparator = comparator.reversed();
        }

        for (Map.Entry<String, Function<AclBinding, String>> entry : sortKeys.entrySet()) {
            comparator = comparator.thenComparing(entry.getValue());
        }

        return comparator;
    }
}
