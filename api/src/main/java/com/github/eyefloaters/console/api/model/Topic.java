package com.github.eyefloaters.console.api.model;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.eyefloaters.console.api.support.ComparatorBuilder;
import com.github.eyefloaters.console.api.support.ListRequestContext;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;

@Schema(name = "Topic")
public class Topic extends RelatableResource<Topic.Attributes, Topic.Relationships> {

    public static final String FIELDS_PARAM = "fields[topics]";

    public static final class Fields {
        public static final String NAME = "name";
        public static final String INTERNAL = "internal";
        public static final String PARTITIONS = "partitions";
        public static final String AUTHORIZED_OPERATIONS = "authorizedOperations";
        public static final String CONFIGS = "configs";
        public static final String RECORD_COUNT = "recordCount";
        public static final String TOTAL_LEADER_LOG_BYTES = "totalLeaderLogBytes";
        public static final String CONSUMER_GROUPS = "consumerGroups";
        static final Pattern CONFIG_KEY = Pattern.compile("^configs\\.\"([^\"]+)\"$");

        static final Comparator<Topic> ID_COMPARATOR =
                comparing(Topic::getId);

        static final Comparator<ConfigEntry> CONFIG_COMPARATOR =
                nullsLast(comparing(ConfigEntry::getType, nullsLast(Comparable::compareTo)))
                    .thenComparing(nullsLast(ConfigEntry::compareValues));

        static final Map<String, Map<Boolean, Comparator<Topic>>> COMPARATORS = ComparatorBuilder.bidirectional(
                Map.of("id", ID_COMPARATOR,
                        NAME, comparing(topic -> topic.attributes.name),
                        RECORD_COUNT, nullsLast(comparing(topic -> topic.attributes.getRecordCount())),
                        TOTAL_LEADER_LOG_BYTES, nullsLast(comparing(topic -> topic.attributes.getTotalLeaderLogBytes()))));

        public static final ComparatorBuilder<Topic> COMPARATOR_BUILDER =
                new ComparatorBuilder<>(Topic.Fields::comparator, Topic.Fields.defaultComparator());

        public static final String LIST_DEFAULT = NAME + ", " + INTERNAL;
        public static final String DESCRIBE_DEFAULT =
                NAME + ", "
                + INTERNAL + ", "
                + PARTITIONS + ", "
                + AUTHORIZED_OPERATIONS + ", "
                + RECORD_COUNT + ", "
                + TOTAL_LEADER_LOG_BYTES;

        private Fields() {
            // Prevent instances
        }

        public static Comparator<Topic> defaultComparator() {
            return ID_COMPARATOR;
        }

        public static Comparator<Topic> comparator(String fieldName, boolean descending) {
            if (COMPARATORS.containsKey(fieldName)) {
                return COMPARATORS.get(fieldName).get(descending);
            }

            Matcher configMatcher = CONFIG_KEY.matcher(fieldName);

            if (configMatcher.matches()) {
                String configKey = configMatcher.group(1);
                Comparator<Topic> configComparator = comparing(
                        t -> t.configEntry(configKey),
                        CONFIG_COMPARATOR);

                if (descending) {
                    configComparator = configComparator.reversed();
                }

                return configComparator;
            }

            return null;
        }
    }

    @Schema(name = "TopicListResponse")
    public static final class ListResponse extends DataList<Topic> {
        public ListResponse(List<Topic> data, ListRequestContext<Topic> listSupport) {
            super(data.stream()
                    .map(entry -> {
                        entry.addMeta("page", listSupport.buildPageMeta(entry::toCursor));
                        return entry;
                    })
                    .toList());
            addMeta("page", listSupport.buildPageMeta());
            listSupport.buildPageLinks(Topic::toCursor).forEach(this::addLink);
        }
    }

    @Schema(name = "TopicResponse")
    public static final class SingleResponse extends DataSingleton<Topic> {
        public SingleResponse(Topic data) {
            super(data);
        }
    }

    @JsonFilter("fieldFilter")
    static class Attributes {
        @JsonProperty
        String name;

        @JsonProperty
        boolean internal;

        @JsonProperty
        @Schema(implementation = Object.class, oneOf = { PartitionInfo[].class, Error.class })
        Either<List<PartitionInfo>, Error> partitions;

        @JsonProperty
        @Schema(implementation = Object.class, oneOf = { String[].class, Error.class })
        Either<List<String>, Error> authorizedOperations;

        @JsonProperty
        @Schema(implementation = Object.class, oneOf = { ConfigEntry.ConfigEntryMap.class, Error.class })
        Either<Map<String, ConfigEntry>, Error> configs;

        Attributes(String name, boolean internal) {
            this.name = name;
            this.internal = internal;
        }

        /**
         * Calculates the record count for the entire topic as the sum
         * of the record counts of each individual partition. When the partitions
         * are not available, the record count is null.
         *
         * @return the sum of the record counts for all partitions
         */
        public Long getRecordCount() {
            return partitions.getOptionalPrimary()
                .map(Collection::stream)
                .map(p -> p.map(PartitionInfo::getRecordCount)
                        .filter(Objects::nonNull)
                        .reduce(0L, Long::sum))
                .orElse(null);
        }

        @Schema(readOnly = true, description = """
                The total size, in bytes, of all log segments local to the leaders
                for each of this topic's partition replicas.
                Or null if this information is not available.

                When support for tiered storage (KIP-405) is available, this property
                may also include the size of remote replica storage.
                """)
        public BigInteger getTotalLeaderLogBytes() {
            return partitions.getOptionalPrimary()
                .map(Collection::stream)
                .map(p -> p.map(PartitionInfo::leaderLocalStorage)
                        .filter(Objects::nonNull)
                        .map(BigInteger::valueOf)
                        .reduce(BigInteger::add)
                        .orElse(null))
                .orElse(null);
        }
    }

    @JsonFilter("fieldFilter")
    static class Relationships {
        @JsonProperty
        final DataList<Identifier> consumerGroups = new DataList<>();
    }

    public Topic(String name, boolean internal, String id) {
        super(id, "topics", new Attributes(name, internal), new Relationships());
    }

    public static Topic fromTopicListing(org.apache.kafka.clients.admin.TopicListing listing) {
        return new Topic(listing.name(), listing.isInternal(), listing.topicId().toString());
    }

    public static Topic fromTopicDescription(org.apache.kafka.clients.admin.TopicDescription description) {
        Topic topic = new Topic(description.name(), description.isInternal(), description.topicId().toString());

        topic.attributes.partitions = Either.of(description.partitions()
                .stream()
                .map(PartitionInfo::fromKafkaModel)
                .toList());

        topic.attributes.authorizedOperations = Either.of(Optional.ofNullable(description.authorizedOperations())
                .map(Collection::stream)
                .map(ops -> ops.map(Enum::name).toList())
                .orElse(null));

        return topic;
    }

    /**
     * Constructs a "cursor" Topic from the encoded string representation of the subset
     * of Topic fields used to compare entities for pagination/sorting.
     */
    public static Topic fromCursor(JsonObject cursor) {
        if (cursor == null) {
            return null;
        }

        JsonObject attr = cursor.getJsonObject("attributes");

        Topic topic = new Topic(attr.getString(Fields.NAME, null), false, cursor.getString("id"));

        if (attr.containsKey(Fields.CONFIGS)) {
            var configs = attr.getJsonObject(Fields.CONFIGS)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> ConfigEntry.fromCursor(e.getValue())));
            topic.attributes.configs = Either.of(configs);
        }

        return topic;
    }

    public String toCursor(List<String> sortFields) {
        JsonObjectBuilder cursor = Json.createObjectBuilder()
                .add("id", id);
        JsonObjectBuilder attrBuilder = Json.createObjectBuilder();

        if (sortFields.contains(Fields.NAME)) {
            attrBuilder.add(Fields.NAME, attributes.name);
        }

        JsonObjectBuilder sortedConfigsBuilder = Json.createObjectBuilder();

        sortFields.stream()
            .map(Fields.CONFIG_KEY::matcher)
            .filter(Matcher::matches)
            .forEach(configMatcher -> {
                String configKey = configMatcher.group(1);
                ConfigEntry entry = configEntry(configKey);

                if (entry != null) {
                    sortedConfigsBuilder.add(configKey, entry.toCursor());
                } else {
                    sortedConfigsBuilder.addNull(configKey);
                }
            });

        JsonObject sortedConfigs = sortedConfigsBuilder.build();

        if (!sortedConfigs.isEmpty()) {
            attrBuilder.add(Fields.CONFIGS, sortedConfigs);
        }

        cursor.add("attributes", attrBuilder.build());

        return Base64.getUrlEncoder().encodeToString(cursor.build().toString().getBytes(StandardCharsets.UTF_8));
    }

    public void addPartitions(Either<Topic, Throwable> description) {
        attributes.partitions = description.ifPrimaryOrElse(
                Topic::partitions,
                thrown -> Error.forThrowable(thrown, "Unable to describe topic"));
    }

    public void addAuthorizedOperations(Either<Topic, Throwable> description) {
        attributes.authorizedOperations = description.ifPrimaryOrElse(
                Topic::authorizedOperations,
                thrown -> Error.forThrowable(thrown, "Unable to describe topic"));
    }

    public void addConfigs(Either<Map<String, ConfigEntry>, Throwable> configs) {
        attributes.configs = configs.ifPrimaryOrElse(
                Either::of,
                thrown -> Error.forThrowable(thrown, "Unable to describe topic configs"));
    }

    public String name() {
        return attributes.name;
    }

    public boolean internal() {
        return attributes.internal;
    }

    public Either<List<PartitionInfo>, Error> partitions() {
        return attributes.partitions;
    }

    public Either<List<String>, Error> authorizedOperations() {
        return attributes.authorizedOperations;
    }

    public Either<Map<String, ConfigEntry>, Error> configs() {
        return attributes.configs;
    }

    public DataList<Identifier> consumerGroups() {
        return relationships.consumerGroups;
    }

    ConfigEntry configEntry(String key) {
        return Optional.ofNullable(attributes.configs)
            .flatMap(Either::getOptionalPrimary)
            .map(c -> c.get(key))
            .orElse(null);
    }

}
