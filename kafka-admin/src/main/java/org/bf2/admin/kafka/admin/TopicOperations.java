package org.bf2.admin.kafka.admin;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.kafka.admin.Config;
import io.vertx.kafka.admin.ConfigEntry;
import io.vertx.kafka.admin.KafkaAdminClient;
import io.vertx.kafka.admin.NewPartitions;
import io.vertx.kafka.admin.NewTopic;
import io.vertx.kafka.admin.TopicDescription;
import io.vertx.kafka.client.common.ConfigResource;
import org.bf2.admin.kafka.admin.model.AdminServerException;
import org.bf2.admin.kafka.admin.model.ErrorType;
import org.bf2.admin.kafka.admin.model.TopicComparator;
import org.bf2.admin.kafka.admin.model.Types;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

@ApplicationScoped
public class TopicOperations {

    public static final short DEFAULT_PARTITIONS = 1;

    @Inject
    @ConfigProperty(name = "kafka.admin.replication.factor", defaultValue = "3")
    short replicationFactor;

    public CompletionStage<Types.Topic> createTopic(KafkaAdminClient ac, Types.NewTopic inputTopic) {
        NewTopic newKafkaTopic = new NewTopic();
        Promise<Types.Topic> prom = Promise.promise();

        Map<String, String> config = new HashMap<>();
        List<Types.ConfigEntry> configObject = inputTopic.getSettings().getConfig();
        if (configObject != null) {
            configObject.forEach(item -> {
                config.put(item.getKey(), item.getValue());
            });
        }

        newKafkaTopic.setName(inputTopic.getName());
        newKafkaTopic.setReplicationFactor(replicationFactor);
        newKafkaTopic.setNumPartitions(inputTopic.getSettings().getNumPartitions() == null ? DEFAULT_PARTITIONS : inputTopic.getSettings().getNumPartitions());
        if (config != null) {
            newKafkaTopic.setConfig(config);
        }

        ac.createTopics(Collections.singletonList(newKafkaTopic), res -> {
            if (res.failed()) {
                prom.fail(res.cause());
                ac.close();
            } else {
                getTopicDescAndConf(ac, inputTopic.getName()).future()
                    .onComplete(desc -> {
                        if (desc.failed()) {
                            prom.fail(desc.cause());
                        } else {
                            prom.complete(desc.result());
                        }
                        ac.close();
                    });
            }
        });

        return prom.future().toCompletionStage();
    }

    public CompletionStage<Types.Topic> describeTopic(KafkaAdminClient ac, String topicToDescribe) {
        return getTopicDescAndConf(ac, topicToDescribe).future().toCompletionStage();
    }

    private static Promise<Types.Topic> getTopicDescAndConf(KafkaAdminClient ac, String topicToDescribe) {
        Promise<Types.Topic> result = Promise.promise();
        Types.Topic tmp = new Types.Topic();
        ConfigResource resource = new ConfigResource(org.apache.kafka.common.config.ConfigResource.Type.TOPIC, topicToDescribe);

        ac.describeTopics(Collections.singletonList(topicToDescribe))
            .compose(topics -> {
                io.vertx.kafka.admin.TopicDescription topicDesc = topics.get(topicToDescribe);
                return Future.succeededFuture(getTopicDesc(topicDesc));
            })
            .compose(topic -> {
                tmp.setName(topic.getName());
                tmp.setIsInternal(topic.getIsInternal());
                tmp.setPartitions(topic.getPartitions());
                return Future.succeededFuture();
            })
            .compose(kkk -> ac.describeConfigs(Collections.singletonList(resource))
            .compose(topics -> {
                Config cfg = topics.get(resource);
                tmp.setConfig(getTopicConf(cfg));
                return Future.succeededFuture(tmp);
            }))
            .onComplete(f -> {
                if (f.succeeded()) {
                    result.complete(f.result());
                } else {
                    result.fail(f.cause());
                }
            });
        return result;
    }

    public CompletionStage<Types.TopicList> getTopicList(KafkaAdminClient ac, Pattern pattern, Types.DeprecatedPageRequest pageRequest, Types.TopicSortParams orderByInput) {
        Promise<Set<String>> describeTopicsNamesPromise = Promise.promise();
        Promise<Map<String, io.vertx.kafka.admin.TopicDescription>> describeTopicsPromise = Promise.promise();
        Promise<Map<ConfigResource, Config>> describeTopicConfigPromise = Promise.promise();
        Promise<Types.TopicList> prom = Promise.promise();

        List<ConfigResource> configResourceList = new ArrayList<>();
        List<Types.Topic> fullDescription = new ArrayList<>();

        ac.listTopics(describeTopicsNamesPromise);
        describeTopicsNamesPromise.future()
            .compose(topics -> {
                List<String> filteredList = topics.stream()
                        .filter(topicName -> byName(pattern, prom).test(topicName))
                        .collect(Collectors.toList());
                ac.describeTopics(filteredList, describeTopicsPromise);
                return describeTopicsPromise.future();
            }).compose(topics -> {
                topics.entrySet().forEach(topicWithDescription -> {
                    Types.Topic desc = getTopicDesc(topicWithDescription.getValue());
                    fullDescription.add(desc);
                    ConfigResource resource = new ConfigResource(org.apache.kafka.common.config.ConfigResource.Type.TOPIC, desc.getName());
                    configResourceList.add(resource);
                });
                ac.describeConfigs(configResourceList, describeTopicConfigPromise);
                return describeTopicConfigPromise.future();
            }).compose(topicsConfigurations -> {
                List<Types.Topic> fullTopicDescriptions = new ArrayList<>();
                fullDescription.forEach(topicWithDescription -> {
                    ConfigResource resource = new ConfigResource(org.apache.kafka.common.config.ConfigResource.Type.TOPIC, topicWithDescription.getName());
                    Config cfg = topicsConfigurations.get(resource);
                    topicWithDescription.setConfig(getTopicConf(cfg));
                    fullTopicDescriptions.add(topicWithDescription);
                });

                if (Types.SortDirectionEnum.DESC.equals(orderByInput.getOrder())) {
                    fullTopicDescriptions.sort(new TopicComparator(orderByInput.getField()).reversed());
                } else {
                    fullTopicDescriptions.sort(new TopicComparator(orderByInput.getField()));
                }


                Types.TopicList topicList = new Types.TopicList();
                List<Types.Topic> croppedList;
                if (pageRequest.isDeprecatedFormat()) {
                    // deprecated
                    if (pageRequest.getOffset() > fullTopicDescriptions.size()) {
                        return Future.failedFuture(new AdminServerException(ErrorType.INVALID_REQUEST, "Offset (" + pageRequest.getOffset() + ") cannot be greater than topic list size (" + fullTopicDescriptions.size() + ")"));
                    }
                    int tmpLimit = pageRequest.getLimit();
                    if (tmpLimit == 0) {
                        tmpLimit = fullTopicDescriptions.size();
                    }
                    croppedList = fullTopicDescriptions.subList(pageRequest.getOffset(), Math.min(pageRequest.getOffset() + tmpLimit, fullTopicDescriptions.size()));
                    topicList.setOffset(pageRequest.getOffset());
                    topicList.setLimit(pageRequest.getLimit());
                    topicList.setCount(croppedList.size());
                } else {
                    if (fullTopicDescriptions.size() > 0 && (pageRequest.getPage() - 1) * pageRequest.getSize() >= fullTopicDescriptions.size()) {
                        return Future.failedFuture(new AdminServerException(ErrorType.INVALID_REQUEST, "Requested pagination incorrect. Beginning of list greater than full list size (" + fullTopicDescriptions.size() + ")"));
                    }
                    croppedList = fullTopicDescriptions.subList((pageRequest.getPage() - 1) * pageRequest.getSize(), Math.min(pageRequest.getPage() * pageRequest.getSize(), fullTopicDescriptions.size()));
                    topicList.setPage(pageRequest.getPage());
                    topicList.setSize(pageRequest.getSize());
                    topicList.setTotal(fullTopicDescriptions.size());
                }

                topicList.setItems(croppedList);

                return Future.succeededFuture(topicList);
            }).onComplete(finalRes -> {
                if (finalRes.failed()) {
                    prom.fail(finalRes.cause());
                } else {
                    prom.complete(finalRes.result());
                }
                ac.close();
            });

        return prom.future().toCompletionStage();
    }

    static Predicate<String> byName(Pattern pattern, Promise<?> prom) {
        return topic -> {
            if (pattern == null) {
                return true;
            } else {
                try {
                    Matcher matcher = pattern.matcher(topic);
                    return matcher.find();
                } catch (PatternSyntaxException ex) {
                    prom.fail(ex);
                    return false;
                }
            }
        };
    }

    public CompletionStage<List<String>> deleteTopics(KafkaAdminClient ac, List<String> topicsToDelete) {
        Promise<List<String>> prom = Promise.promise();

        ac.deleteTopics(topicsToDelete, res -> {
            if (res.failed()) {
                prom.fail(res.cause());
            } else {
                prom.complete(topicsToDelete);
            }
            ac.close();
        });

        return prom.future().toCompletionStage();
    }

    public CompletionStage<Types.Topic> updateTopic(KafkaAdminClient ac, String topicName, Types.TopicSettings topicToUpdate) {
        Promise<Types.Topic> prom = Promise.promise();
        List<ConfigEntry> ceList = new ArrayList<>();
        if (topicToUpdate.getConfig() != null) {
            topicToUpdate.getConfig().stream().forEach(cfgEntry -> {
                ConfigEntry ce = new ConfigEntry(cfgEntry.getKey(), cfgEntry.getValue());
                ceList.add(ce);
            });
        }
        Config cfg = new Config(ceList);

        ConfigResource resource = new ConfigResource(org.apache.kafka.common.config.ConfigResource.Type.TOPIC, topicName);

        // we have to describe first, otherwise we cannot determine whether the topic exists or not (alterConfigs returns just server error)
        getTopicDescAndConf(ac, topicName).future()
                .compose(topic -> {
                    Promise<Void> updateTopicPartitions = Promise.promise();
                    if (topicToUpdate.getNumPartitions() != null && topicToUpdate.getNumPartitions() != topic.getPartitions().size()) {
                        ac.createPartitions(Collections.singletonMap(topic.getName(), new NewPartitions(topicToUpdate.getNumPartitions(), null)), updateTopicPartitions);
                    } else {
                        updateTopicPartitions.complete();
                    }
                    return updateTopicPartitions.future();
                })
                .compose(i -> {
                    Promise<Void> updateTopicConfigPromise = Promise.promise();
                    ac.alterConfigs(Collections.singletonMap(resource, cfg), updateTopicConfigPromise);
                    return updateTopicConfigPromise.future();
                })
                .compose(update -> getTopicDescAndConf(ac, topicName).future())
                .onComplete(desc -> {
                    if (desc.failed()) {
                        prom.fail(desc.cause());
                    } else {
                        prom.complete(desc.result());
                    }
                    ac.close();
                });

        return prom.future().toCompletionStage();
    }

    private static List<Types.ConfigEntry> getTopicConf(Config cfg) {
        List<ConfigEntry> entries = cfg.getEntries();
        List<Types.ConfigEntry> topicConfigEntries = new ArrayList<>();
        entries.stream().forEach(entry -> {
            Types.ConfigEntry ce = new Types.ConfigEntry();
            ce.setKey(entry.getName());
            ce.setValue(entry.getValue());
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
        topic.setName(topicDesc.getName());
        topic.setIsInternal(topicDesc.isInternal());
        List<Types.Partition> partitions = new ArrayList<>();
        topicDesc.getPartitions().forEach(part -> {
            Types.Partition partition = new Types.Partition();
            Types.Node leader = new Types.Node();
            leader.setId(part.getLeader().getId());

            List<Types.Node> replicas = new ArrayList<>();
            part.getReplicas().forEach(rep -> {
                Types.Node replica = new Types.Node();
                replica.setId(rep.getId());
                replicas.add(replica);
            });

            List<Types.Node> inSyncReplicas = new ArrayList<>();
            part.getIsr().forEach(isr -> {
                Types.Node inSyncReplica = new Types.Node();
                inSyncReplica.setId(isr.getId());
                inSyncReplicas.add(inSyncReplica);
            });

            partition.setPartition(part.getPartition());
            partition.setLeader(leader);
            partition.setReplicas(replicas);
            partition.setIsr(inSyncReplicas);
            partitions.add(partition);
        });
        topic.setPartitions(partitions);

        return topic;
    }
}
