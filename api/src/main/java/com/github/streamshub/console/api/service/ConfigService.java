package com.github.streamshub.console.api.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AlterConfigOp;
import org.apache.kafka.clients.admin.AlterConfigOp.OpType;
import org.apache.kafka.clients.admin.AlterConfigsOptions;
import org.apache.kafka.common.config.ConfigResource;

import com.github.streamshub.console.api.model.ConfigEntry;
import com.github.streamshub.console.api.model.Either;
import com.github.streamshub.console.api.support.KafkaContext;

@ApplicationScoped
public class ConfigService {

    @Inject
    KafkaContext kafkaContext;

    public CompletionStage<Map<String, ConfigEntry>> describeConfigs(ConfigResource.Type type, String name) {
        ConfigResource nodeKey = new ConfigResource(type, name);

        return describeConfigs(kafkaContext.admin(), List.of(nodeKey))
            .thenApply(configs -> configs.get(name))
            .thenApply(configs -> configs.getOrThrow(CompletionException::new));
    }

    /**
     * Alter the provided map of configurations using
     * {@linkplain Admin#incrementalAlterConfigs(Map)}. Map entries with a null
     * value will be {@linkplain OpType#DELETE deleted/reverted to default}, whereas
     * non-null entries will be {@linkplain OpType#SET set}.
     *
     * @param type           resource type
     * @param name           resource name
     * @param alteredConfigs map of altered configurations, null values allowed
     * @return CompletionStage "promise" that completes when all operations
     *         submitted to {@linkplain Admin#incrementalAlterConfigs(Map)}.
     *         complete
     */
    public CompletionStage<Void> alterConfigs(ConfigResource.Type type, String name, Map<String, ConfigEntry> alteredConfigs, boolean validateOnly) {
        Admin adminClient = kafkaContext.admin();
        var resourceKey = new ConfigResource(type, name);

        return adminClient.incrementalAlterConfigs(Map.of(resourceKey, fromMap(alteredConfigs)), new AlterConfigsOptions()
                .validateOnly(validateOnly))
            .values()
            .get(resourceKey)
            .toCompletionStage();
    }

    CompletionStage<Map<String, Either<Map<String, ConfigEntry>, Throwable>>> describeConfigs(Admin adminClient, List<ConfigResource> keys) {
        Map<String, Either<Map<String, ConfigEntry>, Throwable>> result = new LinkedHashMap<>(keys.size());

        var pendingDescribes = adminClient.describeConfigs(keys)
            .values()
            .entrySet()
            .stream()
            .map(entry ->
                entry.getValue().toCompletionStage().<Void>handle((description, error) -> {
                    result.put(entry.getKey().name(), Either.of(description, error, conf -> toMap(conf.entries())));
                    return null;
                }))
            .map(CompletionStage::toCompletableFuture)
            .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(pendingDescribes).thenApply(nothing -> result);
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
                .map(e -> toAlterConfigOp(e.getKey(), e.getValue()))
                .toList();
    }

    AlterConfigOp toAlterConfigOp(String key, ConfigEntry entry) {
        AlterConfigOp.OpType opType = entry != null ? OpType.SET : OpType.DELETE;
        String value = Optional.ofNullable(entry).map(ConfigEntry::getValue).orElse(null);
        var configEntry = new org.apache.kafka.clients.admin.ConfigEntry(key, value);
        return new AlterConfigOp(configEntry, opType);
    }
}
