package org.bf2.admin.kafka.admin;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.ConfigEntry;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.config.ConfigResource;
import org.bf2.admin.kafka.admin.model.AdminServerException;
import org.bf2.admin.kafka.admin.model.ErrorType;
import org.bf2.admin.kafka.admin.model.TopicComparator;
import org.bf2.admin.kafka.admin.model.Types;
import org.bf2.admin.kafka.admin.model.Types.PagedResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ApplicationScoped
public class TopicOperations {

    public static final Integer DEFAULT_PARTITIONS = 1;

    @Inject
    @ConfigProperty(name = "kafka.admin.replication.factor", defaultValue = "3")
    short replicationFactor;

    public CompletionStage<Types.Topic> createTopic(Admin ac, Types.NewTopic inputTopic) {
        String topicName = inputTopic.getName();
        int numPartitions = Objects.requireNonNullElse(inputTopic.getSettings().getNumPartitions(), DEFAULT_PARTITIONS);
        Map<String, String> config = Optional.ofNullable(inputTopic.getSettings().getConfig())
            .orElseGet(Collections::emptyList)
            .stream()
            .collect(Collectors.toMap(Types.ConfigEntry::getKey, Types.ConfigEntry::getValue));

        NewTopic newKafkaTopic = new NewTopic(topicName, numPartitions, replicationFactor)
                .configs(config);

        return ac.createTopics(Set.of(newKafkaTopic))
            .all()
            .toCompletionStage()
            .thenCompose(nothing -> getTopicDescAndConf(ac, Set.of(topicName)))
            .thenApply(result -> result.get(topicName));
    }

    public CompletionStage<Types.Topic> describeTopic(Admin ac, String topicToDescribe) {
        return getTopicDescAndConf(ac, Set.of(topicToDescribe))
                .thenApply(result -> result.get(topicToDescribe));
    }

    private static CompletionStage<Map<String, Types.Topic>> getTopicDescAndConf(Admin ac, Set<String> topicNames) {
        var describeStage = ac.describeTopics(topicNames)
                .all()
                .toCompletionStage()
                .thenApply(topics -> topics.entrySet()
                           .stream()
                           .collect(Collectors.toMap(Map.Entry::getKey, e -> getTopicDesc(e.getValue()))));

        var configsStage = ac.describeConfigs(topicNames.stream().map(name -> new ConfigResource(ConfigResource.Type.TOPIC, name)).collect(Collectors.toList()))
                .all()
                .toCompletionStage()
                .thenApply(configs -> configs.entrySet()
                           .stream()
                           .collect(Collectors.toMap(Map.Entry::getKey, e -> getTopicConf(e.getValue()))));

        return describeStage.thenCombine(configsStage, (topics, configs) -> {
            for (var config : configs.entrySet()) {
                topics.get(config.getKey().name()).setConfig(config.getValue());
            }

            return topics;
        });
    }

    public CompletionStage<PagedResponse<Types.Topic>> getTopicList(Admin ac, Pattern pattern, Types.DeprecatedPageRequest pageRequest, Types.TopicSortParams orderByInput) {
        return ac.listTopics()
            .names()
            .toCompletionStage()
            .thenApply(topicNames -> topicNames.stream()
                       .filter(name -> byName(pattern).test(name))
                       .collect(Collectors.toSet()))
            .thenCompose(topicNames -> getTopicDescAndConf(ac, topicNames))
            .thenApply(topicsConfigurations -> {
                Comparator<Types.Topic> comparator;

                if (Types.SortDirectionEnum.DESC.equals(orderByInput.getOrder())) {
                    comparator = new TopicComparator(orderByInput.getField()).reversed();
                } else {
                    comparator = new TopicComparator(orderByInput.getField());
                }

                return topicsConfigurations.values().stream().sorted(comparator).collect(Collectors.toList());
            })
            .thenApply(topicsConfigurations -> {
                if (pageRequest.isDeprecatedFormat()) {
                    Types.TopicList topicList = new Types.TopicList();
                    List<Types.Topic> croppedList;

                    // deprecated
                    if (pageRequest.getOffset() > topicsConfigurations.size()) {
                        throw new AdminServerException(ErrorType.INVALID_REQUEST, "Offset (" + pageRequest.getOffset() + ") cannot be greater than topic list size (" + topicsConfigurations.size() + ")");
                    }
                    int tmpLimit = pageRequest.getLimit();
                    if (tmpLimit == 0) {
                        tmpLimit = topicsConfigurations.size();
                    }
                    croppedList = topicsConfigurations.subList(pageRequest.getOffset(), Math.min(pageRequest.getOffset() + tmpLimit, topicsConfigurations.size()));
                    topicList.setOffset(pageRequest.getOffset());
                    topicList.setLimit(pageRequest.getLimit());
                    topicList.setCount(croppedList.size());
                    topicList.setItems(croppedList);

                    return topicList;
                }

                return PagedResponse.forPage(pageRequest, Types.Topic.class, topicsConfigurations);
            });
    }

    static Predicate<String> byName(Pattern pattern) {
        return topic -> {
            if (pattern == null) {
                return true;
            } else {
                Matcher matcher = pattern.matcher(topic);
                return matcher.find();
            }
        };
    }

    public CompletionStage<List<String>> deleteTopics(Admin ac, List<String> topicsToDelete) {
        return ac.deleteTopics(topicsToDelete)
            .all()
            .toCompletionStage()
            .thenApply(nothing -> topicsToDelete);
    }

    public CompletionStage<Types.Topic> updateTopic(Admin ac, String topicName, Types.TopicSettings topicToUpdate) {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);

        List<ConfigEntry> configEntries = Optional.ofNullable(topicToUpdate.getConfig())
            .orElseGet(Collections::emptyList)
            .stream()
            .map(config -> new ConfigEntry(config.getKey(), config.getValue()))
            .collect(Collectors.toList());
        Config cfg = new Config(configEntries);

        // we have to describe first, otherwise we cannot determine whether the topic exists or not (alterConfigs returns just server error)
        return getTopicDescAndConf(ac, Set.of(topicName))
            .thenApply(topics -> topics.get(topicName))
            .thenCompose(topic -> {
                if (topicToUpdate.getNumPartitions() != null && topicToUpdate.getNumPartitions() != topic.getPartitions().size()) {
                    return ac.createPartitions(Map.of(topicName, NewPartitions.increaseTo(topicToUpdate.getNumPartitions())))
                        .all()
                        .toCompletionStage()
                        .thenApply(nothing -> topic);
                }

                return CompletableFuture.completedStage(topic);
            })
            .thenCompose(topic -> ac.alterConfigs(Map.of(resource, cfg))
                    .all()
                    .toCompletionStage()
                    .thenApply(nothing -> topic))
            .thenCompose(topic -> getTopicDescAndConf(ac, Set.of(topicName)))
            .thenApply(topics -> topics.get(topicName));
    }

    private static List<Types.ConfigEntry> getTopicConf(Config cfg) {
        Collection<ConfigEntry> entries = cfg.entries();
        List<Types.ConfigEntry> topicConfigEntries = new ArrayList<>();
        entries.stream().forEach(entry -> {
            Types.ConfigEntry ce = new Types.ConfigEntry();
            ce.setKey(entry.name());
            ce.setValue(entry.value());
            topicConfigEntries.add(ce);
        });
        return topicConfigEntries;
    }

    /**
     * @param topicDesc topic to describe
     * @returntopic description without configuration
     */
    private static Types.Topic getTopicDesc(TopicDescription topicDesc) {
        Types.Topic topic = new Types.Topic();
        topic.setName(topicDesc.name());
        topic.setIsInternal(topicDesc.isInternal());
        List<Types.Partition> partitions = new ArrayList<>();
        topicDesc.partitions().forEach(part -> {
            Types.Partition partition = new Types.Partition();
            Types.Node leader = new Types.Node();
            leader.setId(part.leader().id());

            List<Types.Node> replicas = new ArrayList<>();
            part.replicas().forEach(rep -> {
                Types.Node replica = new Types.Node();
                replica.setId(rep.id());
                replicas.add(replica);
            });

            List<Types.Node> inSyncReplicas = new ArrayList<>();
            part.isr().forEach(isr -> {
                Types.Node inSyncReplica = new Types.Node();
                inSyncReplica.setId(isr.id());
                inSyncReplicas.add(inSyncReplica);
            });

            partition.setPartition(part.partition());
            partition.setLeader(leader);
            partition.setReplicas(replicas);
            partition.setIsr(inSyncReplicas);
            partitions.add(partition);
        });
        topic.setPartitions(partitions);

        return topic;
    }
}
