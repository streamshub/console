package org.bf2.admin.kafka.admin;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.kafka.admin.ConsumerGroupDescription;
import io.vertx.kafka.admin.ConsumerGroupListing;
import io.vertx.kafka.admin.KafkaAdminClient;
import io.vertx.kafka.admin.ListOffsetsResultInfo;
import io.vertx.kafka.admin.MemberDescription;
import io.vertx.kafka.admin.OffsetSpec;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.bf2.admin.kafka.admin.model.AdminServerException;
import org.bf2.admin.kafka.admin.model.ConsumerGroupComparator;
import org.bf2.admin.kafka.admin.model.ErrorType;
import org.bf2.admin.kafka.admin.model.Types;
import org.bf2.admin.kafka.admin.model.Types.ConsumerGroupOffsetResetParameters.OffsetType;
import org.bf2.admin.kafka.admin.model.Types.PagedResponse;
import org.bf2.admin.kafka.admin.model.Types.TopicPartitionResetResult;
import org.jboss.logging.Logger;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.ToLongFunction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings({"checkstyle:CyclomaticComplexity", "checkstyle:NPathComplexity"})
public class ConsumerGroupOperations {

    protected static final Logger log = Logger.getLogger(ConsumerGroupOperations.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssz");
    private static final Pattern MATCH_ALL = Pattern.compile(".*");
    private static final Types.ConsumerGroupDescriptionSortParams BLANK_ORDER =
            new Types.ConsumerGroupDescriptionSortParams(Types.ConsumerGroupDescriptionOrderKey.PARTITION, Types.SortDirectionEnum.ASC);

    public static CompletionStage<PagedResponse<Types.ConsumerGroup>> getGroupList(KafkaAdminClient ac, Pattern topicPattern, Pattern groupIdPattern,
                                                                                   Types.DeprecatedPageRequest pageRequest, Types.ConsumerGroupSortParams orderByInput) {
        Promise<PagedResponse<Types.ConsumerGroup>> prom = Promise.promise();

        // Obtain list of all consumer groups
        ac.listConsumerGroups()
            .map(groups -> groups.stream()
                 .map(ConsumerGroupListing::getGroupId)
                 // Include only those group matching query parameter (or all if not specified)
                 .filter(groupId -> groupIdPattern.matcher(groupId).find())
                 .collect(Collectors.toList()))
            // Obtain description for all selected consumer groups
            .compose(groupDescriptions -> fetchDescriptions(ac, groupDescriptions, topicPattern, -1, BLANK_ORDER))
            .map(groupDescriptions -> groupDescriptions
                 .sorted(Types.SortDirectionEnum.DESC.equals(orderByInput.getOrder()) ?
                     new ConsumerGroupComparator(orderByInput.getField()).reversed() :
                         new ConsumerGroupComparator(orderByInput.getField()))
                 .collect(Collectors.<Types.ConsumerGroup>toList()))
            .map(list -> {
                if (pageRequest.isDeprecatedFormat()) {
                    if (pageRequest.getOffset() > list.size()) {
                        throw new AdminServerException(ErrorType.INVALID_REQUEST, "Offset (" + pageRequest.getOffset() + ") cannot be greater than consumer group list size (" + list.size() + ")");
                    }

                    int tmpLimit = pageRequest.getLimit();
                    if (tmpLimit == 0) {
                        tmpLimit = list.size();
                    }

                    var response = new Types.ConsumerGroupList();
                    response.setLimit(pageRequest.getLimit());
                    response.setOffset(pageRequest.getOffset());

                    var croppedList = list.subList(pageRequest.getOffset(), Math.min(pageRequest.getOffset() + tmpLimit, list.size()));
                    response.setCount(croppedList.size());
                    response.setItems(croppedList);

                    return response;
                }

                return PagedResponse.forPage(pageRequest, Types.ConsumerGroup.class, list);
            })
            .onComplete(finalRes -> {
                if (finalRes.failed()) {
                    prom.fail(finalRes.cause());
                } else {
                    prom.complete(finalRes.result());
                }
                ac.close();
            });

        return prom.future().toCompletionStage();
    }

    public static CompletionStage<List<String>> deleteGroup(KafkaAdminClient ac, List<String> groupsToDelete) {
        Promise<List<String>> prom = Promise.promise();
        ac.deleteConsumerGroups(groupsToDelete, res -> {
            if (res.failed()) {
                prom.fail(res.cause());
            } else {
                prom.complete(groupsToDelete);
            }
            ac.close();
        });

        return prom.future().toCompletionStage();
    }

    @SuppressWarnings({"checkstyle:JavaNCSS", "checkstyle:MethodLength"})
    public static CompletionStage<PagedResponse<TopicPartitionResetResult>> resetGroupOffset(KafkaAdminClient ac, Types.ConsumerGroupOffsetResetParameters parameters) {
        Promise<PagedResponse<TopicPartitionResetResult>> prom = Promise.promise();

        switch (parameters.getOffset()) {
            case EARLIEST:
            case LATEST:
                break;
            default:
                if (parameters.getValue() == null) {
                    throw new AdminServerException(ErrorType.INVALID_REQUEST, "value is required when " + parameters.getOffset().getValue() + " offset is used.");
                }
        }

        Set<TopicPartition> topicPartitionsToReset = new HashSet<>();

        @SuppressWarnings("rawtypes") // CompositeFuture#join requires raw type
        List<Future> promises = new ArrayList<>();

        if (parameters.getTopics() == null || parameters.getTopics().isEmpty()) {
            // reset everything
            Promise<Void> promise = Promise.promise();
            promises.add(promise.future());

            ac.listConsumerGroupOffsets(parameters.getGroupId())
                    .onSuccess(consumerGroupOffsets -> {
                        consumerGroupOffsets.entrySet().forEach(offset -> {
                            topicPartitionsToReset.add(offset.getKey());
                        });
                        promise.complete();
                    })
                    .onFailure(promise::fail);
        } else {
            parameters.getTopics().forEach(paramPartition -> {
                Promise<Void> promise = Promise.promise();
                promises.add(promise.future());
                if (paramPartition.getPartitions() == null || paramPartition.getPartitions().isEmpty()) {
                    ac.describeTopics(Collections.singletonList(paramPartition.getTopic())).compose(topicsDesc -> {
                        topicsDesc.entrySet().forEach(topicEntry -> {
                            topicsDesc.get(topicEntry.getKey()).getPartitions().forEach(partition -> {
                                topicPartitionsToReset.add(new TopicPartition(topicEntry.getKey(), partition.getPartition()));
                            });
                        });
                        promise.complete();
                        return Future.succeededFuture(topicPartitionsToReset);
                    })
                    .onFailure(promise::fail);
                } else {
                    paramPartition.getPartitions().forEach(numPartition -> {
                        topicPartitionsToReset.add(new TopicPartition(paramPartition.getTopic(), numPartition));
                    });
                    promise.complete();
                }
            });
        }

        // get the set of partitions we want to reset
        CompositeFuture.join(promises).compose(i -> {
            if (i.failed()) {
                return Future.failedFuture(i.cause());
            } else {
                return Future.succeededFuture();
            }
        }).compose(nothing -> {
            return validatePartitionsResettable(ac, parameters.getGroupId(), topicPartitionsToReset);
        }).compose(nothing -> {
            Map<TopicPartition, OffsetSpec> partitionsToFetchOffset = new HashMap<>();
            topicPartitionsToReset.forEach(topicPartition -> {
                OffsetSpec offsetSpec;
                // absolute - just for the warning that set offset could be higher than latest
                switch (parameters.getOffset()) {
                    case LATEST:
                        offsetSpec = OffsetSpec.LATEST;
                        break;
                    case EARLIEST:
                        offsetSpec = OffsetSpec.EARLIEST;
                        break;
                    case TIMESTAMP:
                        try {
                            offsetSpec = OffsetSpec.TIMESTAMP(ZonedDateTime.parse(parameters.getValue(), DATE_TIME_FORMATTER).toInstant().toEpochMilli());
                        } catch (DateTimeParseException e) {
                            throw new AdminServerException(ErrorType.INVALID_REQUEST, "Timestamp must be in format 'yyyy-MM-dd'T'HH:mm:ssz'" + e.getMessage());
                        }
                        break;
                    case ABSOLUTE:
                        // we are checking whether offset is not negative (set behind latest)
                        offsetSpec = OffsetSpec.LATEST;
                        break;
                    default:
                        throw new AdminServerException(ErrorType.INVALID_REQUEST, "Offset can be 'absolute', 'latest', 'earliest' or 'timestamp' only");
                }

                partitionsToFetchOffset.put(topicPartition, offsetSpec);
            });
            return Future.succeededFuture(partitionsToFetchOffset);
        }).compose(partitionsToFetchOffset -> {
            Promise<Map<TopicPartition, ListOffsetsResultInfo>> promise = Promise.promise();
            ac.listOffsets(partitionsToFetchOffset, partitionsOffsets -> {
                if (partitionsOffsets.failed()) {
                    promise.fail(partitionsOffsets.cause());
                    return;
                }
                if (parameters.getOffset() == OffsetType.ABSOLUTE) {
                    // numeric offset provided; check whether x > latest
                    promise.complete(partitionsOffsets.result().entrySet().stream().collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> {
                            if (entry.getValue().getOffset() < Long.parseLong(parameters.getValue())) {
                                log.warnf("Selected offset %s is larger than latest %d", parameters.getValue(), entry.getValue().getOffset());
                            }
                            return new ListOffsetsResultInfo(Long.parseLong(parameters.getValue()), entry.getValue().getTimestamp(), entry.getValue().getLeaderEpoch());
                        })));
                } else {
                    Map<TopicPartition, ListOffsetsResultInfo> kokot = partitionsOffsets.result().entrySet().stream().collect(Collectors.toMap(
                        entry -> entry.getKey(),
                        entry -> new ListOffsetsResultInfo(partitionsOffsets.result().get(entry.getKey()).getOffset(), entry.getValue().getTimestamp(), entry.getValue().getLeaderEpoch())));
                    promise.complete(kokot);
                }
            });
            return promise.future();
        }).compose(newOffsets -> {
            // assemble new offsets object
            Promise<Map<TopicPartition, OffsetAndMetadata>> promise = Promise.promise();
            ac.listConsumerGroupOffsets(parameters.getGroupId(), list -> {
                if (list.failed()) {
                    promise.fail(list.cause());
                    return;
                }
                if (list.result().isEmpty()) {
                    promise.fail(new AdminServerException(ErrorType.INVALID_REQUEST, "Consumer Group " + parameters.getGroupId() + " does not consume any topics/partitions"));
                    return;
                }
                promise.complete(newOffsets.entrySet().stream().collect(Collectors.toMap(
                    entry -> entry.getKey(),
                    entry -> new OffsetAndMetadata(newOffsets.get(entry.getKey()).getOffset(), list.result().get(entry.getKey()) == null ? null : list.result().get(entry.getKey()).getMetadata()))));
            });
            return promise.future();
        }).compose(newOffsets -> {
            Promise<Void> promise = Promise.promise();
            ac.alterConsumerGroupOffsets(parameters.getGroupId(), newOffsets, res -> {
                if (res.failed()) {
                    promise.fail(res.cause());
                    return;
                }
                promise.complete();
                log.info("resetting offsets");
            });
            return promise.future();
        }).compose(i -> {
            Promise<Types.PagedResponse<Types.TopicPartitionResetResult>> promise = Promise.promise();

            ac.listConsumerGroupOffsets(parameters.getGroupId(), res -> {
                if (res.failed()) {
                    promise.fail(res.cause());
                    return;
                }

                var result = res.result()
                        .entrySet()
                        .stream()
                        .map(entry -> {
                            Types.TopicPartitionResetResult reset = new Types.TopicPartitionResetResult();
                            reset.setTopic(entry.getKey().getTopic());
                            reset.setPartition(entry.getKey().getPartition());
                            reset.setOffset(entry.getValue().getOffset());
                            return reset;
                        })
                        .collect(Collectors.toList());

                promise.complete(Types.PagedResponse.forItems(Types.TopicPartitionResetResult.class, result));
            });
            return promise.future();
        }).onComplete(res -> {
            if (res.succeeded()) {
                prom.complete(res.result());
            } else {
                prom.fail(res.cause());
            }
            ac.close();
        });

        return prom.future().toCompletionStage();
    }

    static Future<Void> validatePartitionsResettable(KafkaAdminClient ac, String groupId, Set<TopicPartition> topicPartitionsToReset) {
        Map<TopicPartition, List<MemberDescription>> topicPartitions = new ConcurrentHashMap<>();

        List<String> requestedTopics = topicPartitionsToReset
                .stream()
                .map(TopicPartition::getTopic)
                .collect(Collectors.toList());

        Promise<Void> topicDescribe = Promise.promise();

        if (requestedTopics.isEmpty()) {
            topicDescribe.complete();
        } else {
            ac.describeTopics(requestedTopics)
                .onSuccess(describedTopics -> {
                    describedTopics.entrySet()
                        .stream()
                        .flatMap(entry ->
                            entry.getValue()
                                .getPartitions()
                                .stream()
                                .map(part -> new TopicPartition(entry.getKey(), part.getPartition())))
                        .forEach(topicPartition ->
                            topicPartitions.compute(topicPartition, (key, value) -> addTopicPartition(value, null)));

                    topicDescribe.complete();
                })
                .onFailure(error -> {
                    if (ErrorType.isCausedBy(error, UnknownTopicOrPartitionException.class)) {
                        topicDescribe.fail(new AdminServerException(ErrorType.TOPIC_PARTITION_INVALID));
                    } else {
                        topicDescribe.fail(error);
                    }
                });
        }

        Promise<Void> groupDescribe = Promise.promise();

        ac.describeConsumerGroups(List.of(groupId))
            .onSuccess(descriptions -> {
                /*
                 * Find all topic partitions in the group that are actively
                 * being consumed by a client.
                 */
                descriptions.values()
                    .stream()
                    .flatMap(description -> description.getMembers().stream())
                    .filter(member -> member.getClientId() != null)
                    .flatMap(member ->
                        member.getAssignment()
                             .getTopicPartitions()
                             .stream()
                             .map(part -> Map.entry(part, member)))
                    .forEach(entry -> {
                        MemberDescription member = entry.getValue();
                        topicPartitions.compute(entry.getKey(), (key, value) -> addTopicPartition(value, member));
                    });

                groupDescribe.complete();
            })
            .onFailure(groupDescribe::fail);

        return CompositeFuture.all(topicDescribe.future(), groupDescribe.future())
                .map(nothing -> {
                    topicPartitionsToReset.forEach(topicPartition ->
                        validatePartitionResettable(topicPartitions, topicPartition));
                    return null;
                });
    }

    static List<MemberDescription> addTopicPartition(List<MemberDescription> members, MemberDescription newMember) {
        if (members == null) {
            members = new ArrayList<>();
        }

        if (newMember != null) {
            members.add(newMember);
        }

        return members;
    }

    static void validatePartitionResettable(Map<TopicPartition, List<MemberDescription>> topicClients, TopicPartition topicPartition) {
        if (!topicClients.containsKey(topicPartition)) {
            String message = String.format("Topic %s, partition %d is not valid",
                                           topicPartition.getTopic(),
                                           topicPartition.getPartition());
            throw new AdminServerException(ErrorType.TOPIC_PARTITION_INVALID, message);
        } else if (!topicClients.get(topicPartition).isEmpty()) {
            /*
             * Reject the request if any of the topic partitions
             * being reset is also being consumed by a client.
             */
            String clients = topicClients.get(topicPartition)
                .stream()
                .map(member -> String.format("{ memberId: %s, clientId: %s }", member.getConsumerId(), member.getClientId()))
                .collect(Collectors.joining(", "));
            String message = String.format("Topic %s, partition %d has connected clients: [%s]",
                                           topicPartition.getTopic(),
                                           topicPartition.getPartition(),
                                           clients);

            throw new AdminServerException(ErrorType.GROUP_NOT_EMPTY, message);
        }
    }

    public static CompletionStage<Types.ConsumerGroup> describeGroup(KafkaAdminClient ac, String groupToDescribe, Types.ConsumerGroupDescriptionSortParams orderBy, int partitionFilter) {
        Promise<Types.ConsumerGroup> prom = Promise.promise();

        fetchDescriptions(ac, List.of(groupToDescribe), MATCH_ALL, partitionFilter, orderBy)
            .map(groupDescriptions -> groupDescriptions.findFirst().orElse(null))
            .onComplete(res -> {
                if (res.failed()) {
                    prom.fail(res.cause());
                } else {
                    Types.ConsumerGroup groupDescription = res.result();

                    if (groupDescription == null || ConsumerGroupState.DEAD.equals(groupDescription.getState())) {
                        prom.fail(new GroupIdNotFoundException("Group " + groupToDescribe + " does not exist"));
                    } else {
                        prom.complete(groupDescription);
                    }
                }
                ac.close();
            });

        return prom.future().toCompletionStage();
    }

    private static List<Types.ConsumerGroup> getConsumerGroupsDescription(Pattern pattern,
            Types.ConsumerGroupDescriptionSortParams orderBy,
            int partitionFilter,
            Collection<ConsumerGroupDescription> groupDescriptions,
            Map<TopicPartition, OffsetAndMetadata> groupOffsets,
            Map<TopicPartition, ListOffsetsResultInfo> topicOffsets) {

        List<TopicPartition> assignedTopicPartitions = groupOffsets.entrySet()
                .stream()
                .map(Map.Entry::getKey)
                .filter(topicPartition -> pattern.matcher(topicPartition.getTopic()).find())
                .collect(Collectors.toList());

        return groupDescriptions.stream().map(group -> {
            Types.ConsumerGroup grp = new Types.ConsumerGroup();
            Set<Types.Consumer> members = new HashSet<>();

            if (group.getMembers().isEmpty()) {
                assignedTopicPartitions.forEach(pa -> {
                    Types.Consumer member = getConsumer(groupOffsets, topicOffsets, group, pa);
                    members.add(member);
                });
            } else {
                assignedTopicPartitions.forEach(pa -> {
                    group.getMembers().stream().forEach(mem -> {
                        if (!mem.getAssignment().getTopicPartitions().isEmpty()) {
                            Types.Consumer member = getConsumer(groupOffsets, topicOffsets, group, pa);
                            if (memberMatchesPartitionFilter(member, partitionFilter)) {
                                if (mem.getAssignment().getTopicPartitions().contains(pa)) {
                                    member.setMemberId(mem.getConsumerId());
                                } else {
                                    // unassigned partition
                                    member.setMemberId(null);
                                }

                                if (members.contains(member)) {
                                    // some member does not consume the partition, so it was flagged as unconsumed
                                    // another member does consume the partition, so we override the member in result
                                    if (member.getMemberId() != null) {
                                        members.remove(member);
                                        members.add(member);
                                    }
                                } else {
                                    members.add(member);
                                }
                            }
                        } else {
                            // more consumers than topic partitions - consumer is in the group but is not consuming
                            Types.Consumer member = new Types.Consumer();
                            member.setMemberId(mem.getConsumerId());
                            member.setTopic(null);
                            member.setPartition(-1);
                            member.setGroupId(group.getGroupId());
                            member.setLogEndOffset(0);
                            member.setLag(0);
                            member.setOffset(0);
                            if (memberMatchesPartitionFilter(member, partitionFilter)) {
                                members.add(member);
                            }
                        }
                    });
                });
            }

            if (!pattern.pattern().equals(MATCH_ALL.pattern()) && members.isEmpty()) {
                return null;
            }
            grp.setGroupId(group.getGroupId());
            grp.setState(group.getState());
            List<Types.Consumer> sortedList;

            ToLongFunction<Types.Consumer> fun;

            switch (orderBy.getField()) {
                case LAG:
                    fun = Types.Consumer::getLag;
                    break;
                case END_OFFSET:
                    fun = Types.Consumer::getLogEndOffset;
                    break;
                case OFFSET:
                    fun = Types.Consumer::getOffset;
                    break;
                default:
                    fun = Types.Consumer::getPartition;
                    break;
            }

            if (Types.SortDirectionEnum.DESC.equals(orderBy.getOrder())) {
                sortedList = members.stream().sorted(Comparator.comparingLong(fun).reversed()).collect(Collectors.toList());
            } else {
                sortedList = members.stream().sorted(Comparator.comparingLong(fun)).collect(Collectors.toList());
            }

            grp.setConsumers(sortedList);
            grp.setMetrics(calculateGroupMetrics(sortedList));

            return grp;

        }).collect(Collectors.toList());

    }

    private static Types.ConsumerGroupMetrics calculateGroupMetrics(List<Types.Consumer> consumersList) {
        var metrics = new Types.ConsumerGroupMetrics();

        var laggingPartitions = 0;
        var unassignedPartitions = 0;

        for (Types.Consumer consumer : consumersList) {
            if (consumer.getMemberId() == null) {
                unassignedPartitions++;
            } else if (consumer.getLag() > 0) {
                laggingPartitions++;
            }
        }

        var activeConsumers = consumersList.stream()
            .filter(consumer -> consumer.getMemberId() != null)
            .map(Types.Consumer::getMemberId)
            .distinct().count();

        metrics.setActiveConsumers((int) activeConsumers);
        metrics.setUnassignedPartitions(unassignedPartitions);
        metrics.setLaggingPartitions(laggingPartitions);

        return metrics;
    }

    private static boolean memberMatchesPartitionFilter(Types.Consumer member, int partitionFilter) {
        if (partitionFilter < 0) {
            // filter deactivated
            return true;
        } else {
            return member.getPartition() == partitionFilter;
        }
    }

    private static Types.Consumer getConsumer(Map<TopicPartition, OffsetAndMetadata> groupOffsets,
            Map<TopicPartition, ListOffsetsResultInfo> topicOffsets,
            ConsumerGroupDescription group,
            TopicPartition pa) {

        Types.Consumer member = new Types.Consumer();
        member.setTopic(pa.getTopic());
        member.setPartition(pa.getPartition());
        member.setGroupId(group.getGroupId());
        long offset = groupOffsets.get(pa) == null ? 0 : groupOffsets.get(pa).getOffset();
        long logEndOffset = topicOffsets.get(pa) == null ? 0 : topicOffsets.get(pa).getOffset();
        long lag = logEndOffset - offset;
        member.setLag(lag);
        member.setLogEndOffset(logEndOffset);
        member.setOffset(offset);
        return member;
    }

    /**
     * Obtains a future stream of {@link Types.ConsumerGroupDescription}s. Using the provided
     * groupIds list, the following information is fetched for each consumer group:
     *
     * <ul>
     * <li>Consumer group description (using {@link KafkaAdminClient#describeConsumerGroups(List)})
     * <li>Current consumer group offsets (using {@link KafkaAdminClient#listConsumerGroupOffsets(String)})
     * </ul>
     *
     * The unique set of {@link TopicPartition}s for the listed consumer groups will then be used to
     * obtain the current topic offsets using {@link KafkaAdminClient#listOffsets(Map)}).
     *
     * Results will be filtered according to the provided topicPattern and partitionFilter. Sorting
     * of each consumer group's members will be performed based on the provided memberOrder.
     *
     * @param ac Kafka client
     * @param groupIds the groups to describe
     * @param topicPattern regular expression pattern to limit results to matching topics
     * @param partitionFilter partition number to limit results to a specific partition
     * @param memberOrder consumer group member sorting
     * @return future stream of {@link Types.ConsumerGroupDescription}
     */
    @SuppressWarnings("rawtypes")
    static Future<Stream<Types.ConsumerGroup>> fetchDescriptions(KafkaAdminClient ac,
                                                                     List<String> groupIds,
                                                                     Pattern topicPattern,
                                                                     int partitionFilter,
                                                                     Types.ConsumerGroupDescriptionSortParams memberOrder) {

        List<ConsumerGroupInfo> consumerGroupInfos = new ArrayList<>(groupIds.size());

        return ac.describeConsumerGroups(groupIds)
            .map(Map::entrySet)
            .map(descriptions -> descriptions.stream()
                 // Fetch the offsets for consumer groups
                 .map(entry -> ac.listConsumerGroupOffsets(entry.getKey()).map(offsets -> new ConsumerGroupInfo(entry.getValue(), offsets)))
                 .collect(Collectors.<Future>toList()))
            .compose(CompositeFuture::join)
            .map(CompositeFuture::<ConsumerGroupInfo>list)
            .compose(groupInfos -> {
                consumerGroupInfos.addAll(groupInfos);
                // Fetch the topic offsets for all partitions in the selected consumer groups
                return ac.listOffsets(toListLatestOffsetMap(groupInfos));
            })
            .map(latestOffsets -> consumerGroupInfos.stream()
                 .map(e -> getConsumerGroupsDescription(topicPattern, memberOrder, partitionFilter, List.of(e.getDescription()), e.getOffsets(), latestOffsets))
                 .flatMap(List::stream)
                 .filter(Objects::nonNull));
    }

    /**
     * Transform the offsets for all {@link TopicPartition}s from the given list
     * of {@link ConsumerGroupInfo} to a map with each unique
     * {@link TopicPartition} and all values set to {@link OffsetSpec#LATEST}.
     *
     * Used to fetch the latest offsets for all {@link TopicPartition}s
     * included in the listed consumer groups.
     *
     * @param consumerGroupInfos
     *            list of {@link ConsumerGroupInfo}s for mapping
     * @return map of all {@link TopicPartition}s from the provided consumer
     *         groups with values set to {@link OffsetSpec#LATEST}
     */
    static Map<TopicPartition, OffsetSpec> toListLatestOffsetMap(List<ConsumerGroupInfo> consumerGroupInfos) {
        return consumerGroupInfos.stream()
            .map(ConsumerGroupInfo::getOffsets)
            .flatMap(offsets -> offsets.keySet().stream())
            .distinct()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Function.identity(), partition -> OffsetSpec.LATEST));
    }
}
