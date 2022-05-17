package org.bf2.admin.kafka.admin;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.admin.ConsumerGroupListing;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.ListOffsetsResult.ListOffsetsResultInfo;
import org.apache.kafka.clients.admin.MemberDescription;
import org.apache.kafka.clients.admin.OffsetSpec;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.ConsumerGroupState;
import org.apache.kafka.common.TopicPartition;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
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

    public static CompletionStage<PagedResponse<Types.ConsumerGroup>> getGroupList(Admin ac, Pattern topicPattern, Pattern groupIdPattern,
                                                                                   Types.DeprecatedPageRequest pageRequest, Types.ConsumerGroupSortParams orderByInput) {
        // Obtain list of all consumer groups
        return ac.listConsumerGroups()
            .all()
            .toCompletionStage()
            .thenApply(listings -> listings.stream().map(ConsumerGroupListing::groupId))
            // Include only those group matching query parameter (or all if not specified)
            .thenApply(groupIds -> groupIds.filter(groupId -> groupIdPattern.matcher(groupId).find()).collect(Collectors.toList()))
            // Obtain description for all selected consumer groups
            .thenCompose(groupIds -> fetchDescriptions(ac, groupIds, topicPattern, -1, BLANK_ORDER))
            .thenApply(groupDescriptions -> groupDescriptions
                 .sorted(Types.SortDirectionEnum.DESC.equals(orderByInput.getOrder()) ?
                     new ConsumerGroupComparator(orderByInput.getField()).reversed() :
                         new ConsumerGroupComparator(orderByInput.getField()))
                 .collect(Collectors.<Types.ConsumerGroup>toList()))
            .thenApply(list -> {
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
            });
    }

    public static CompletionStage<List<String>> deleteGroup(Admin ac, List<String> groupsToDelete) {
        return ac.deleteConsumerGroups(groupsToDelete)
                .all()
                .toCompletionStage()
                .thenApply(nothing -> groupsToDelete);
    }

    @SuppressWarnings({"checkstyle:JavaNCSS", "checkstyle:MethodLength"})
    public static CompletionStage<PagedResponse<TopicPartitionResetResult>> resetGroupOffset(Admin ac, Types.ConsumerGroupOffsetResetParameters parameters) {
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
        List<CompletableFuture<Void>> promises = new ArrayList<>();

        if (parameters.getTopics() == null || parameters.getTopics().isEmpty()) {
            // reset everything
            promises.add(
                 ac.listConsumerGroupOffsets(parameters.getGroupId())
                    .partitionsToOffsetAndMetadata()
                    .toCompletionStage()
                    .thenAccept(offsets -> topicPartitionsToReset.addAll(offsets.keySet()))
                    .toCompletableFuture());
        } else {
            parameters.getTopics()
                .stream()
                .map(topic -> {
                    if (topic.getPartitions() == null || topic.getPartitions().isEmpty()) {
                        return ac.describeTopics(Set.of(topic.getTopic()))
                                .all()
                                .toCompletionStage()
                                .thenAccept(topicsDesc -> {
                                    topicsDesc.entrySet().forEach(topicEntry -> {
                                        topicsDesc.get(topicEntry.getKey()).partitions().forEach(partition -> {
                                            topicPartitionsToReset.add(new TopicPartition(topicEntry.getKey(), partition.partition()));
                                        });
                                    });
                                });
                    }

                    topicPartitionsToReset.addAll(topic.getPartitions().stream()
                                                  .map(part -> new TopicPartition(topic.getTopic(), part))
                                                  .collect(Collectors.toList()));
                    return CompletableFuture.completedFuture((Void) null);
                })
                .map(CompletionStage::toCompletableFuture)
                .forEach(promises::add);
        }

        // get the set of partitions we want to reset
        return CompletableFuture.allOf(promises.toArray(CompletableFuture[]::new))
                .thenCompose(nothing -> validatePartitionsResettable(ac, parameters.getGroupId(), topicPartitionsToReset))
                .thenApply(nothing -> {
                    Map<TopicPartition, OffsetSpec> partitionsToFetchOffset = new HashMap<>();

                    topicPartitionsToReset.forEach(topicPartition -> {
                        OffsetSpec offsetSpec;
                        // absolute - just for the warning that set offset could be higher than latest
                        switch (parameters.getOffset()) {
                            case LATEST:
                                offsetSpec = OffsetSpec.latest();
                                break;
                            case EARLIEST:
                                offsetSpec = OffsetSpec.earliest();
                                break;
                            case TIMESTAMP:
                                try {
                                    offsetSpec = OffsetSpec.forTimestamp(ZonedDateTime.parse(parameters.getValue(), DATE_TIME_FORMATTER).toInstant().toEpochMilli());
                                } catch (DateTimeParseException e) {
                                    throw new AdminServerException(ErrorType.INVALID_REQUEST, "Timestamp must be in format 'yyyy-MM-dd'T'HH:mm:ssz'" + e.getMessage());
                                }
                                break;
                            case ABSOLUTE:
                                // we are checking whether offset is not negative (set behind latest)
                                offsetSpec = OffsetSpec.latest();
                                break;
                            default:
                                throw new AdminServerException(ErrorType.INVALID_REQUEST, "Offset can be 'absolute', 'latest', 'earliest' or 'timestamp' only");
                        }

                        partitionsToFetchOffset.put(topicPartition, offsetSpec);
                    });

                    return partitionsToFetchOffset;
                })
                .thenCompose(partitionsToFetchOffset ->
                    ac.listOffsets(partitionsToFetchOffset)
                        .all()
                        .toCompletionStage()
                        .thenApply(partitionsOffsets -> {
                            if (parameters.getOffset() == OffsetType.ABSOLUTE) {
                                // numeric offset provided; check whether x > latest
                                return partitionsOffsets.entrySet()
                                    .stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey,
                                                              entry -> {
                                                                  long requestedOffset = Long.parseLong(parameters.getValue());

                                                                  if (entry.getValue().offset() < requestedOffset) {
                                                                      log.warnf("Selected offset %d is larger than latest %d", requestedOffset, entry.getValue().offset());
                                                                  }

                                                                  return new ListOffsetsResultInfo(requestedOffset,
                                                                                                   entry.getValue().timestamp(),
                                                                                                   entry.getValue().leaderEpoch());
                                                              }));
                            } else {
                                return partitionsOffsets.entrySet()
                                    .stream()
                                    .collect(Collectors.toMap(Map.Entry::getKey,
                                                              entry -> new ListOffsetsResultInfo(entry.getValue().offset(),
                                                                                                 entry.getValue().timestamp(),
                                                                                                 entry.getValue().leaderEpoch())));
                            }
                        }))
                .thenCompose(newOffsets ->
                    // assemble new offsets object
                    ac.listConsumerGroupOffsets(parameters.getGroupId())
                        .partitionsToOffsetAndMetadata()
                        .toCompletionStage()
                        .thenApply(list -> {
                            if (list.isEmpty()) {
                                throw new AdminServerException(ErrorType.INVALID_REQUEST, "Consumer Group " + parameters.getGroupId() + " does not consume any topics/partitions");
                            }

                            return newOffsets.entrySet()
                                .stream()
                                .collect(Collectors.toMap(Map.Entry::getKey,
                                                          entry -> new OffsetAndMetadata(newOffsets.get(entry.getKey()).offset(),
                                                                                         list.get(entry.getKey()) == null ? null : list.get(entry.getKey()).metadata())));
                        }))
                .thenCompose(newOffsets -> {
                    log.info("resetting offsets");
                    return ac.alterConsumerGroupOffsets(parameters.getGroupId(), newOffsets)
                        .all()
                        .toCompletionStage();
                })
                .thenCompose(nothing -> {
                    return ac.listConsumerGroupOffsets(parameters.getGroupId())
                        .partitionsToOffsetAndMetadata()
                        .toCompletionStage()
                        .thenApply(results -> {
                            var result = results
                                    .entrySet()
                                    .stream()
                                    .map(entry -> {
                                        Types.TopicPartitionResetResult reset = new Types.TopicPartitionResetResult();
                                        reset.setTopic(entry.getKey().topic());
                                        reset.setPartition(entry.getKey().partition());
                                        reset.setOffset(entry.getValue().offset());
                                        return reset;
                                    })
                                    .collect(Collectors.toList());

                            return Types.PagedResponse.forItems(Types.TopicPartitionResetResult.class, result);
                        });
                });
    }

    static CompletionStage<Void> validatePartitionsResettable(Admin ac, String groupId, Set<TopicPartition> topicPartitionsToReset) {
        Map<TopicPartition, List<MemberDescription>> topicPartitions = new ConcurrentHashMap<>();

        List<String> requestedTopics = topicPartitionsToReset
                .stream()
                .map(TopicPartition::topic)
                .collect(Collectors.toList());

        final CompletableFuture<Void> topicDescribe;

        if (requestedTopics.isEmpty()) {
            topicDescribe = CompletableFuture.completedFuture(null);
        } else {
            topicDescribe = ac.describeTopics(requestedTopics)
                .all()
                .toCompletionStage()
                .thenAccept(describedTopics -> {
                    describedTopics.entrySet()
                        .stream()
                        .flatMap(entry ->
                            entry.getValue()
                                .partitions()
                                .stream()
                                .map(part -> new TopicPartition(entry.getKey(), part.partition())))
                        .forEach(topicPartition ->
                            topicPartitions.compute(topicPartition, (key, value) -> addTopicPartition(value, null)));
                })
                .exceptionally(error -> {
                    if (ErrorType.isCausedBy(error, UnknownTopicOrPartitionException.class)) {
                        throw new AdminServerException(ErrorType.TOPIC_PARTITION_INVALID);
                    }
                    throw new RuntimeException(error);
                })
                .toCompletableFuture();
        }

        CompletableFuture<Void> groupDescribe = ac.describeConsumerGroups(List.of(groupId))
            .all()
            .toCompletionStage()
            .thenApply(groups -> groups.get(groupId))
            .thenAccept(description -> description.members()
                   .stream()
                   .filter(member -> member.clientId() != null)
                   .flatMap(member -> member.assignment()
                         .topicPartitions()
                         .stream()
                         .map(part -> Map.entry(part, member)))
                   .forEach(entry ->
                       topicPartitions.compute(entry.getKey(), (key, value) -> addTopicPartition(value, entry.getValue()))))
            .toCompletableFuture();

        return CompletableFuture.allOf(topicDescribe, groupDescribe)
                .thenAccept(nothing ->
                    topicPartitionsToReset.forEach(topicPartition ->
                        validatePartitionResettable(topicPartitions, topicPartition)));
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
                                           topicPartition.topic(),
                                           topicPartition.partition());
            throw new AdminServerException(ErrorType.TOPIC_PARTITION_INVALID, message);
        } else if (!topicClients.get(topicPartition).isEmpty()) {
            /*
             * Reject the request if any of the topic partitions
             * being reset is also being consumed by a client.
             */
            String clients = topicClients.get(topicPartition)
                .stream()
                .map(member -> String.format("{ memberId: %s, clientId: %s }", member.consumerId(), member.clientId()))
                .collect(Collectors.joining(", "));
            String message = String.format("Topic %s, partition %d has connected clients: [%s]",
                                           topicPartition.topic(),
                                           topicPartition.partition(),
                                           clients);

            throw new AdminServerException(ErrorType.GROUP_NOT_EMPTY, message);
        }
    }

    public static CompletionStage<Types.ConsumerGroup> describeGroup(Admin ac, String groupToDescribe, Types.ConsumerGroupDescriptionSortParams orderBy, int partitionFilter) {
        return fetchDescriptions(ac, List.of(groupToDescribe), MATCH_ALL, partitionFilter, orderBy)
            .thenApply(groupDescriptions -> groupDescriptions.findFirst().orElse(null))
            .thenApply(groupDescription -> {
                if (groupDescription == null || ConsumerGroupState.DEAD.equals(groupDescription.getState())) {
                    throw new GroupIdNotFoundException("Group " + groupToDescribe + " does not exist");
                }
                return groupDescription;
            });
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
                .filter(topicPartition -> pattern.matcher(topicPartition.topic()).find())
                .collect(Collectors.toList());

        return groupDescriptions.stream().map(group -> {
            Types.ConsumerGroup grp = new Types.ConsumerGroup();
            Set<Types.Consumer> members = new HashSet<>();

            if (group.members().isEmpty()) {
                assignedTopicPartitions.forEach(pa -> {
                    Types.Consumer member = getConsumer(groupOffsets, topicOffsets, group, pa);
                    members.add(member);
                });
            } else {
                assignedTopicPartitions.forEach(pa -> {
                    group.members().stream().forEach(mem -> {
                        if (!mem.assignment().topicPartitions().isEmpty()) {
                            Types.Consumer member = getConsumer(groupOffsets, topicOffsets, group, pa);
                            if (memberMatchesPartitionFilter(member, partitionFilter)) {
                                if (mem.assignment().topicPartitions().contains(pa)) {
                                    member.setMemberId(mem.consumerId());
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
                            member.setMemberId(mem.consumerId());
                            member.setTopic(null);
                            member.setPartition(-1);
                            member.setGroupId(group.groupId());
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
            grp.setGroupId(group.groupId());
            grp.setState(group.state());
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
        member.setTopic(pa.topic());
        member.setPartition(pa.partition());
        member.setGroupId(group.groupId());
        long offset = groupOffsets.get(pa) == null ? 0 : groupOffsets.get(pa).offset();
        long logEndOffset = topicOffsets.get(pa) == null ? 0 : topicOffsets.get(pa).offset();
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
    static CompletionStage<Stream<Types.ConsumerGroup>> fetchDescriptions(Admin ac,
                                                                     List<String> groupIds,
                                                                     Pattern topicPattern,
                                                                     int partitionFilter,
                                                                     Types.ConsumerGroupDescriptionSortParams memberOrder) {

        List<ConsumerGroupInfo> consumerGroupInfos = new ArrayList<>(groupIds.size());

        return ac.describeConsumerGroups(groupIds)
            .all()
            .toCompletionStage()
            .thenApply(groups -> groups.entrySet()
                   .stream()
                   // Fetch the offsets for consumer groups
                   .map(entry -> ac.listConsumerGroupOffsets(entry.getKey())
                        .partitionsToOffsetAndMetadata()
                        .toCompletionStage()
                        .thenAccept(offsets -> consumerGroupInfos.add(new ConsumerGroupInfo(entry.getValue(), offsets)))
                        .toCompletableFuture())
                   .toArray(CompletableFuture[]::new))
            .thenCompose(CompletableFuture::allOf)
            .thenApply(nothing -> toListLatestOffsetMap(consumerGroupInfos))
            .thenCompose(offsetMap -> ac.listOffsets(offsetMap).all().toCompletionStage())
            .thenApply(latestOffsets -> consumerGroupInfos.stream()
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
            .collect(Collectors.toMap(Function.identity(), p -> OffsetSpec.latest()));
    }
}
