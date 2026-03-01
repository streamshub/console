package com.github.streamshub.console.api.model;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.json.Json;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue.ValueType;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.streamshub.console.api.model.jsonapi.JsonApiError;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRelationshipToMany;
import com.github.streamshub.console.api.model.jsonapi.JsonApiResource;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRootData;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRootDataList;
import com.github.streamshub.console.api.support.ComparatorBuilder;
import com.github.streamshub.console.api.support.ListRequestContext;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;

@Schema(name = "Topic")
public class Topic extends JsonApiResource<Topic.Attributes, Topic.Relationships> {

    public static final String API_TYPE = "topics";
    public static final String FIELDS_PARAM = "fields[" + API_TYPE + "]";

    public static final class Fields {
        public static final String NAME = "name";
        public static final String VISIBILITY = "visibility";
        public static final String PARTITIONS = "partitions";
        public static final String NUM_PARTITIONS = "numPartitions";
        public static final String AUTHORIZED_OPERATIONS = "authorizedOperations";
        public static final String CONFIGS = "configs";
        public static final String TOTAL_LEADER_LOG_BYTES = "totalLeaderLogBytes";
        public static final String GROUPS = "groups";
        public static final String STATUS = "status";
        static final Pattern CONFIG_KEY = Pattern.compile("^configs\\.\"([^\"]+)\"$");

        static final Comparator<Topic> ID_COMPARATOR =
                comparing(Topic::getId);

        static final Comparator<ConfigEntry> CONFIG_COMPARATOR =
                nullsLast(comparing(ConfigEntry::getType, nullsLast(Comparable::compareTo)))
                    .thenComparing(nullsLast(ConfigEntry::compareValues));

        static final Map<String, Map<Boolean, Comparator<Topic>>> COMPARATORS = ComparatorBuilder.bidirectional(
                Map.of("id", ID_COMPARATOR,
                        NAME, comparing(topic -> topic.attributes.name),
                        TOTAL_LEADER_LOG_BYTES, comparing(
                                topic -> topic.attributes.getTotalLeaderLogBytes(),
                                nullsLast(BigInteger::compareTo))));

        public static final ComparatorBuilder<Topic> COMPARATOR_BUILDER =
                new ComparatorBuilder<>(Topic.Fields::comparator, Topic.Fields.defaultComparator());

        public static final String LIST_DEFAULT = NAME + ", " + VISIBILITY;
        public static final String DESCRIBE_DEFAULT =
                NAME + ", "
                + STATUS + ", "
                + VISIBILITY + ", "
                + PARTITIONS + ", "
                + NUM_PARTITIONS + ", "
                + AUTHORIZED_OPERATIONS + ", "
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

    @Schema(name = "TopicDataList")
    public static final class ListResponse extends JsonApiRootDataList<Topic> {
        public ListResponse(List<Topic> data, ListRequestContext<Topic> listSupport) {
            super(data.stream()
                    .map(entry -> {
                        entry.addMeta("page", listSupport.buildPageMeta(entry::toCursor));
                        return entry;
                    })
                    .toList());
            addMeta("page", listSupport.buildPageMeta());
            listSupport.meta().forEach(this::addMeta);
            listSupport.buildPageLinks(Topic::toCursor).forEach(this::addLink);
        }
    }

    @Schema(name = "TopicData")
    public static final class SingleResponse extends JsonApiRootData<Topic> {
        public SingleResponse(Topic data) {
            super(data);
        }
    }

    @JsonFilter("fieldFilter")
    public static class Attributes {
        @JsonProperty
        String name;

        @JsonIgnore
        boolean internal;

        @JsonProperty
        @Schema(implementation = Object.class, oneOf = { PartitionInfo[].class, JsonApiError.class })
        Either<List<PartitionInfo>, JsonApiError> partitions;

        @JsonProperty
        @Schema(implementation = Object.class, oneOf = { String[].class, JsonApiError.class })
        Either<List<String>, JsonApiError> authorizedOperations;

        @JsonProperty
        @Schema(implementation = Object.class, oneOf = { ConfigEntry.ConfigEntryMap.class, JsonApiError.class })
        Either<Map<String, ConfigEntry>, JsonApiError> configs;

        Attributes(String name, boolean internal) {
            this.name = name;
            this.internal = internal;
        }

        @JsonProperty
        public String status() {
            if (partitions == null) {
                return "Unknown";
            }

            return partitions.getOptionalPrimary()
                .map(p -> {
                    Supplier<Stream<String>> partitionStatuses = () -> p.stream().map(PartitionInfo::status);
                    long offlinePartitions = partitionStatuses.get().filter("Offline"::equals).count();

                    if (offlinePartitions > 0) {
                        if (offlinePartitions < p.size()) {
                            return "PartiallyOffline";
                        } else {
                            return "Offline";
                        }
                    }

                    if (partitionStatuses.get().anyMatch("UnderReplicated"::equals)) {
                        return "UnderReplicated";
                    }

                    return "FullyReplicated";
                })
                .orElse("FullyReplicated");
        }

        @JsonProperty
        @Schema(readOnly = true, description = """
                Derived property indicating whether this is an internal (i.e. system, private) topic or
                an external (i.e. application, public) topic. Internal topics are those that are identified
                explicitly by the Kafka Admin API as internal or (by convention) have a name starting with
                the underscore `_` character. External topics are simply those that are not internal.
                """)
        public String visibility() {
            return internal || name.startsWith("_") ? "internal" : "external";
        }

        @JsonProperty
        @Schema(readOnly = true, description = "The number of partitions in this topic")
        public Integer numPartitions() {
            if (partitions == null) {
                return null;
            }

            return partitions.getOptionalPrimary().map(Collection::size).orElse(null);
        }

        @Schema(readOnly = true, description = """
                The total size, in bytes, of all log segments local to the leaders
                for each of this topic's partition replicas.
                Or null if this information is not available.

                When support for tiered storage (KIP-405) is available, this property
                may also include the size of remote replica storage.
                """)
        public BigInteger getTotalLeaderLogBytes() {
            if (partitions == null) {
                return null;
            }

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
        JsonApiRelationshipToMany groups = new JsonApiRelationshipToMany();
    }

    public Topic(String name, boolean internal, String id) {
        super(id, API_TYPE, new Attributes(name, internal), new Relationships());
    }

    private Topic(String id, Attributes attributes) {
        super(id, API_TYPE, attributes, new Relationships());
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

        Topic topic = new Topic(cursor.getString("id"), new Attributes(attr.getString(Fields.NAME, null), false) {
            @Override
            public BigInteger getTotalLeaderLogBytes() {
                if (attr.containsKey(Fields.TOTAL_LEADER_LOG_BYTES)) {
                    var value = attr.get(Fields.TOTAL_LEADER_LOG_BYTES);
                    return value.getValueType() == ValueType.NUMBER ?
                            JsonNumber.class.cast(value).bigIntegerValue() :
                            null;
                }

                return null;
            }
        });

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

        if (sortFields.contains(Fields.TOTAL_LEADER_LOG_BYTES)) {
            var bytes = attributes.getTotalLeaderLogBytes();
            if (bytes != null) {
                attrBuilder.add(Fields.TOTAL_LEADER_LOG_BYTES, bytes);
            } else {
                attrBuilder.addNull(Fields.TOTAL_LEADER_LOG_BYTES);
            }
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
                thrown -> JsonApiError.forThrowable(thrown, "Unable to describe topic"));
    }

    public void addAuthorizedOperations(Either<Topic, Throwable> description) {
        attributes.authorizedOperations = description.ifPrimaryOrElse(
                Topic::authorizedOperations,
                thrown -> JsonApiError.forThrowable(thrown, "Unable to describe topic"));
    }

    public void addConfigs(Either<Map<String, ConfigEntry>, Throwable> configs) {
        attributes.configs = configs.ifPrimaryOrElse(
                Either::of,
                thrown -> JsonApiError.forThrowable(thrown, "Unable to describe topic configs"));
    }

    public String name() {
        return attributes.name;
    }

    public String visibility() {
        return attributes.visibility();
    }

    public String status() {
        return attributes.status();
    }

    public Either<List<PartitionInfo>, JsonApiError> partitions() {
        return attributes.partitions;
    }

    public Either<List<String>, JsonApiError> authorizedOperations() {
        return attributes.authorizedOperations;
    }

    public Either<Map<String, ConfigEntry>, JsonApiError> configs() {
        return attributes.configs;
    }

    public JsonApiRelationshipToMany groups() {
        return relationships.groups;
    }

    public void groups(JsonApiRelationshipToMany groups) {
        relationships.groups = groups;
    }

    public boolean partitionsOnline() {
        return attributes.partitions.getOptionalPrimary()
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .map(PartitionInfo::online)
            .allMatch(Boolean.TRUE::equals);
    }

    ConfigEntry configEntry(String key) {
        return Optional.ofNullable(attributes.configs)
            .flatMap(Either::getOptionalPrimary)
            .map(c -> c.get(key))
            .orElse(null);
    }

}
