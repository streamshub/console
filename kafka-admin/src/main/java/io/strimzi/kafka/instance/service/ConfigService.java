package io.strimzi.kafka.instance.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.config.ConfigResource;

import io.strimzi.kafka.instance.model.ConfigEntry;
import io.strimzi.kafka.instance.model.Either;

@ApplicationScoped
public class ConfigService {

    @Inject
    Supplier<Admin> clientSupplier;

    public CompletionStage<Map<String, ConfigEntry>> describeConfigs(ConfigResource.Type type, String name) {
        ConfigResource nodeKey = new ConfigResource(type, name);

        return describeConfigs(clientSupplier.get(), List.of(nodeKey))
            .thenApply(configs -> configs.get(name))
            .thenApply(configs -> {
                if (configs.isPrimaryPresent()) {
                    return configs.getPrimary();
                }
                throw new CompletionException(configs.getAlternate());
            });
    }

    public CompletionStage<Map<String, ConfigEntry>> alterConfigs(ConfigResource.Type type, String name, Map<String, ConfigEntry> alteredConfigs) {
        Admin adminClient = clientSupplier.get();
        var resourceKey = new ConfigResource(type, name);

        return adminClient.incrementalAlterConfigs(Map.of(resourceKey, fromMap(alteredConfigs)))
            .values()
            .get(resourceKey)
            .toCompletionStage()
            .thenCompose(nothing -> describeConfigs(adminClient, List.of(resourceKey)))
            .thenApply(configs -> configs.get(name))
            .thenApply(configs -> {
                if (configs.isPrimaryPresent()) {
                    return configs.getPrimary();
                }
                throw new CompletionException(configs.getAlternate());
            });
    }

    CompletionStage<Map<String, Either<Map<String, ConfigEntry>, Throwable>>> describeConfigs(Admin adminClient, List<ConfigResource> keys) {
        Map<String, Either<Map<String, ConfigEntry>, Throwable>> result = new LinkedHashMap<>(keys.size());

        var pendingDescribes = adminClient.describeConfigs(keys)
            .values()
            .entrySet()
            .stream()
            .map(entry ->
                entry.getValue().whenComplete((description, error) ->
                    result.put(entry.getKey().name(), either(description, error))))
            .map(KafkaFuture::toCompletionStage)
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new);

        var promise = new CompletableFuture<Map<String, Either<Map<String, ConfigEntry>, Throwable>>>();

        CompletableFuture.allOf(pendingDescribes)
                .thenApply(nothing -> promise.complete(result))
                .exceptionally(promise::completeExceptionally);

        return promise;
    }

    Either<Map<String, ConfigEntry>, Throwable> either(Config entries, Throwable error) {
        Either<Map<String, ConfigEntry>, Throwable> either;

        if (error != null) {
            either = Either.ofAlternate(error);
        } else {
            either = Either.of(toMap(entries.entries()));
        }

        return either;
    }

    Map<String, ConfigEntry> toMap(Collection<org.apache.kafka.clients.admin.ConfigEntry> entries) {
        return entries.stream()
                .sorted(Comparator.comparing(org.apache.kafka.clients.admin.ConfigEntry::name))
                .collect(Collectors.toMap(
                        org.apache.kafka.clients.admin.ConfigEntry::name,
                        ConfigEntry::fromKafkaModel));
    }

    Collection<AlterConfigOp> fromMap(Map<String, ConfigEntry> configs) {
        return configs.entrySet()
                .stream()
                .map(e -> toKafkaModel(e.getValue()))
                .map(entry -> new AlterConfigOp(entry, AlterConfigOp.OpType.SET))
                .toList();
    }

    org.apache.kafka.clients.admin.ConfigEntry toKafkaModel(ConfigEntry entry) {
        return new org.apache.kafka.clients.admin.ConfigEntry(entry.getName(), entry.getValue());
    }
}
