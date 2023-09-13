package com.github.eyefloaters.console.api.model;

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
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.eyefloaters.console.api.support.ComparatorBuilder;
import com.github.eyefloaters.console.api.support.ListRequestContext;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;

@Schema(name = "TopicAttributes")
@JsonFilter("fieldFilter")
public class Topic {

    public static final class Fields {
        public static final String NAME = "name";
        public static final String INTERNAL = "internal";
        public static final String PARTITIONS = "partitions";
        public static final String AUTHORIZED_OPERATIONS = "authorizedOperations";
        public static final String CONFIGS = "configs";
        public static final String RECORD_COUNT = "recordCount";
        static final Pattern CONFIG_KEY = Pattern.compile("^configs\\.\"([^\"]+)\"$");

        static final Comparator<Topic> ID_COMPARATOR =
                comparing(Topic::getId);

        static final Comparator<ConfigEntry> CONFIG_COMPARATOR =
                nullsLast(comparing(ConfigEntry::getType, nullsLast(Comparable::compareTo)))
                    .thenComparing(nullsLast(ConfigEntry::compareValues));

        static final Map<String, Map<Boolean, Comparator<Topic>>> COMPARATORS = ComparatorBuilder.bidirectional(
                Map.of("id", ID_COMPARATOR, NAME, comparing(Topic::getName)));

        public static final ComparatorBuilder<Topic> COMPARATOR_BUILDER =
                new ComparatorBuilder<>(Topic.Fields::comparator, Topic.Fields.defaultComparator());

        public static final String LIST_DEFAULT = NAME + ", " + INTERNAL;
        public static final String DESCRIBE_DEFAULT =
                NAME + ", " + INTERNAL + ", " + PARTITIONS + ", " + AUTHORIZED_OPERATIONS + ", " + RECORD_COUNT;

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
                        t -> t.getConfigEntry(configKey),
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
    public static final class ListResponse extends DataListResponse<TopicResource> {
        public ListResponse(List<Topic> data, ListRequestContext<Topic> listSupport) {
            super(data.stream()
                    .map(entry -> {
                        var rsrc = new TopicResource(entry);
                        rsrc.addMeta("page", listSupport.buildPageMeta(entry::toCursor));
                        return rsrc;
                    })
                    .toList());
            addMeta("page", listSupport.buildPageMeta());
        }
    }

    @Schema(name = "TopicResponse")
    public static final class SingleResponse extends DataResponse<TopicResource> {
        public SingleResponse(Topic data) {
            super(new TopicResource(data));
        }
    }

    @Schema(name = "Topic")
    public static final class TopicResource extends Resource<Topic> {
        public TopicResource(Topic data) {
            super(data.id, "topics", data);
        }
    }

    String name;
    boolean internal;
    @JsonIgnore
    String id;

    @Schema(implementation = Object.class, oneOf = { PartitionInfo[].class, Error.class })
    Either<List<PartitionInfo>, Error> partitions;

    @Schema(implementation = Object.class, oneOf = { String[].class, Error.class })
    Either<List<String>, Error> authorizedOperations;

    @Schema(implementation = Object.class, oneOf = { ConfigEntry.ConfigEntryMap.class, Error.class })
    Either<Map<String, ConfigEntry>, Error> configs;

    public Topic(String name, boolean internal, String id) {
        super();
        this.name = name;
        this.internal = internal;
        this.id = id;
    }

    public static Topic fromTopicListing(org.apache.kafka.clients.admin.TopicListing listing) {
        return new Topic(listing.name(), listing.isInternal(), listing.topicId().toString());
    }

    public static Topic fromTopicDescription(org.apache.kafka.clients.admin.TopicDescription description) {
        Topic topic = new Topic(description.name(), description.isInternal(), description.topicId().toString());

        topic.partitions = Either.of(description.partitions()
                .stream()
                .map(PartitionInfo::fromKafkaModel)
                .toList());

        topic.authorizedOperations = Either.of(Optional.ofNullable(description.authorizedOperations())
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

        JsonObject attr = Optional.ofNullable(cursor.getJsonObject("attributes"))
                .orElseGet(() -> Json.createObjectBuilder().build());

        Topic topic = new Topic(attr.getString(Fields.NAME, null), false, cursor.getString("id"));

        if (attr.containsKey(Fields.CONFIGS)) {
            var configs = attr.getJsonObject(Fields.CONFIGS)
                .entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> ConfigEntry.fromCursor(e.getValue())));
            topic.configs = Either.of(configs);
        }

        return topic;
    }

    public String toCursor(List<String> sortFields) {
        JsonObjectBuilder cursor = Json.createObjectBuilder()
                .add("id", id);
        JsonObjectBuilder attrBuilder = Json.createObjectBuilder();

        if (sortFields.contains(Fields.NAME)) {
            attrBuilder.add(Fields.NAME, name);
        }

        JsonObjectBuilder sortedConfigsBuilder = Json.createObjectBuilder();

        sortFields.stream()
            .map(Fields.CONFIG_KEY::matcher)
            .filter(Matcher::matches)
            .forEach(configMatcher -> {
                String configKey = configMatcher.group(1);
                ConfigEntry entry = getConfigEntry(configKey);

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

        JsonObject attr = attrBuilder.build();

        if (!attr.isEmpty()) {
            cursor.add("attributes", attr);
        }

        return Base64.getUrlEncoder().encodeToString(cursor.build().toString().getBytes(StandardCharsets.UTF_8));
    }

    public void addPartitions(Either<Topic, Throwable> description) {
        partitions = description.ifPrimaryOrElse(
                Topic::getPartitions,
                thrown -> Error.forThrowable(thrown, "Unable to describe topic"));
    }

    public void addAuthorizedOperations(Either<Topic, Throwable> description) {
        authorizedOperations = description.ifPrimaryOrElse(
                Topic::getAuthorizedOperations,
                thrown -> Error.forThrowable(thrown, "Unable to describe topic"));
    }

    public void addConfigs(Either<Map<String, ConfigEntry>, Throwable> configs) {
        this.configs = configs.ifPrimaryOrElse(
                Either::of,
                thrown -> Error.forThrowable(thrown, "Unable to describe topic configs"));
    }

    public String getName() {
        return name;
    }

    public boolean isInternal() {
        return internal;
    }

    public Either<List<PartitionInfo>, Error> getPartitions() {
        return partitions;
    }

    public Either<List<String>, Error> getAuthorizedOperations() {
        return authorizedOperations;
    }

    public String getId() {
        return id;
    }

    public Either<Map<String, ConfigEntry>, Error> getConfigs() {
        return configs;
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

    ConfigEntry getConfigEntry(String key) {
        return Optional.ofNullable(configs)
            .flatMap(Either::getOptionalPrimary)
            .map(c -> c.get(key))
            .orElse(null);
    }

}
