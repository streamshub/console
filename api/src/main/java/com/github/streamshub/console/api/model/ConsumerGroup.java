package com.github.streamshub.console.api.model;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.validation.Valid;

import org.apache.kafka.common.GroupType;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.streamshub.console.api.model.jsonapi.JsonApiError;
import com.github.streamshub.console.api.model.jsonapi.JsonApiMeta;
import com.github.streamshub.console.api.model.jsonapi.JsonApiResource;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRootData;
import com.github.streamshub.console.api.model.jsonapi.JsonApiRootDataList;
import com.github.streamshub.console.api.model.jsonapi.None;
import com.github.streamshub.console.api.support.ComparatorBuilder;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.support.Identifiers;

import io.xlate.validation.constraints.Expression;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;

@Schema(name = "ConsumerGroup")
@Expression(
    value = "self.id != null",
    message = "resource ID is required",
    node = "id",
    payload = ErrorCategory.InvalidResource.class)
@Expression(
    when = "self.type != null",
    value = "self.type == '" + ConsumerGroup.API_TYPE + "'",
    message = "resource type conflicts with operation",
    node = "type",
    payload = ErrorCategory.ResourceConflict.class)
public class ConsumerGroup extends JsonApiResource<ConsumerGroup.Attributes, None> {

    public static final String API_TYPE = "consumerGroups";
    public static final String FIELDS_PARAM = "fields[" + API_TYPE + "]";

    public static final class Fields {
        public static final String GROUP_ID = "groupId";
        public static final String TYPE = "type";
        public static final String PROTOCOL = "protocol";
        public static final String STATE = "state";
        public static final String MEMBERS = "members";
        public static final String COORDINATOR = "coordinator";
        public static final String AUTHORIZED_OPERATIONS = "authorizedOperations";
        public static final String PARTITION_ASSIGNOR = "partitionAssignor";
        public static final String OFFSETS = "offsets";
        public static final String SIMPLE_CONSUMER_GROUP = "simpleConsumerGroup";

        static final Comparator<ConsumerGroup> ID_COMPARATOR =
                comparing(ConsumerGroup::groupId);

        static final Map<String, Map<Boolean, Comparator<ConsumerGroup>>> COMPARATORS = ComparatorBuilder.bidirectional(
                Map.of("id", ID_COMPARATOR,
                        GROUP_ID, nullsLast(comparing(ConsumerGroup::groupId)),
                        TYPE, nullsLast(comparing(ConsumerGroup::type)),
                        PROTOCOL, nullsLast(comparing(ConsumerGroup::protocol)),
                        STATE, nullsLast(comparing(ConsumerGroup::state)),
                        SIMPLE_CONSUMER_GROUP, comparing(ConsumerGroup::simpleConsumerGroup)));

        public static final ComparatorBuilder<ConsumerGroup> COMPARATOR_BUILDER =
                new ComparatorBuilder<>(ConsumerGroup.Fields::comparator, ConsumerGroup.Fields.defaultComparator());

        public static final String LIST_DEFAULT = GROUP_ID +
                ", " + TYPE +
                ", " + PROTOCOL +
                ", " + STATE +
                ", " + SIMPLE_CONSUMER_GROUP;
        public static final String DESCRIBE_DEFAULT = GROUP_ID +
                ", " + TYPE +
                ", " + PROTOCOL +
                ", " + STATE +
                ", " + MEMBERS +
                ", " + COORDINATOR +
                ", " + OFFSETS
                + ", " + SIMPLE_CONSUMER_GROUP;

        private Fields() {
            // Prevent instances
        }

        public static Comparator<ConsumerGroup> defaultComparator() {
            return ID_COMPARATOR;
        }

        public static Comparator<ConsumerGroup> comparator(String fieldName, boolean descending) {
            if (COMPARATORS.containsKey(fieldName)) {
                return COMPARATORS.get(fieldName).get(descending);
            }

            return null;
        }
    }

    @Schema(name = "ConsumerGroupDataList")
    public static final class DataList extends JsonApiRootDataList<ConsumerGroup> {
        public DataList(List<ConsumerGroup> data, ListRequestContext<ConsumerGroup> listSupport) {
            super(data.stream()
                    .map(entry -> {
                        entry.addMeta("page", listSupport.buildPageMeta(entry::toCursor));
                        return entry;
                    })
                    .toList());
            addMeta("page", listSupport.buildPageMeta());
            listSupport.meta().forEach(this::addMeta);
            listSupport.buildPageLinks(ConsumerGroup::toCursor).forEach(this::addLink);
        }
    }

    @Schema(name = "ConsumerGroupData")
    public static final class Data extends JsonApiRootData<ConsumerGroup> {
        @JsonCreator
        public Data(@JsonProperty("data") ConsumerGroup data) {
            super(data);
        }
    }

    public static class Meta extends JsonApiMeta {
        // When a describe error occurs
        @JsonProperty
        List<JsonApiError> errors;
    }

    @JsonFilter("fieldFilter")
    public static class Attributes {
        // Available via list or describe operations
        @JsonProperty
        final String groupId;

        @JsonProperty
        String type;

        @JsonProperty
        String protocol;

        @JsonProperty
        final boolean simpleConsumerGroup;

        @JsonProperty
        final String state;

        // Available via describe operation only

        @JsonProperty
        Collection<MemberDescription> members = Collections.emptyList();

        @JsonProperty
        String partitionAssignor;

        @JsonProperty
        Node coordinator;

        @JsonProperty
        List<String> authorizedOperations;

        // Available via list offsets operation only
        @JsonProperty
        List<@Valid OffsetAndMetadata> offsets = Collections.emptyList();

        @JsonCreator
        private Attributes(String groupId, boolean simpleConsumerGroup, String state) {
            this.groupId = groupId;
            this.simpleConsumerGroup = simpleConsumerGroup;
            this.state = state;
        }

        private Attributes(Attributes other) {
            if (other != null) {
                this.groupId = other.groupId;
                this.simpleConsumerGroup = other.simpleConsumerGroup;
                this.state = other.state;
                this.offsets = other.offsets;
            } else {
                this.groupId = null;
                this.simpleConsumerGroup = false;
                this.state = null;
            }
        }

    }

    @JsonCreator
    public ConsumerGroup(String id, String type, Attributes attributes) {
        super(id, type, new Attributes(attributes));
    }

    private ConsumerGroup(String groupId, boolean simpleConsumerGroup, String state) {
        super(encodeGroupId(groupId), API_TYPE, new Attributes(groupId, simpleConsumerGroup, state));
    }

    public static String encodeGroupId(String groupId) {
        return Identifiers.encode("".equals(groupId) ? "+" : groupId);
    }

    public static String decodeGroupId(String encodedGroupId) {
        String[] decoded = Identifiers.decode(encodedGroupId);

        if (decoded.length == 1) {
            return "+".equals(decoded[0]) ? "" : decoded[0];
        }

        throw new GroupIdNotFoundException("Invalid groupId");
    }

    public static ConsumerGroup fromKafkaModel(org.apache.kafka.clients.admin.GroupListing listing) {
        var group = new ConsumerGroup(
            listing.groupId(),
            listing.isSimpleConsumerGroup(),
            listing.groupState().map(Enum::name).orElse(null)
        );
        group.type(listing.type().map(GroupType::toString).orElse(null));
        group.protocol(listing.protocol());
        return group;
    }

    /**
     * Construct an instance from a Kafka consumer group description and the map of
     * topic Ids, used to set the topic ID of the assignments field elements for
     * each member of the consumber group.
     *
     * @param description Kafka consumer group description model
     * @param topicIds    map of topic names to Ids
     * @return a new {@linkplain ConsumerGroup}
     */
    public static ConsumerGroup fromKafkaModel(
            org.apache.kafka.clients.admin.ConsumerGroupDescription description,
            Map<String, String> topicIds) {

        var group = new ConsumerGroup(
                description.groupId(),
                description.isSimpleConsumerGroup(),
                Optional.ofNullable(description.groupState()).map(Enum::name).orElse(null));

        group.type(description.type().toString());
        group.partitionAssignor(description.partitionAssignor());
        group.coordinator(Node.fromKafkaModel(description.coordinator()));
        group.members(description.members()
                .stream()
                .map(member -> MemberDescription.fromKafkaModel(member, topicIds))
                .toList());

        group.authorizedOperations(Optional.ofNullable(description.authorizedOperations())
                .map(Collection::stream)
                .map(ops -> ops.map(Enum::name).toList())
                .orElse(null));

        return group;
    }

    @Override
    public JsonApiMeta metaFactory() {
        return new Meta();
    }

    public void addError(JsonApiError error) {
        var meta = (Meta) getOrCreateMeta();

        if (meta.errors == null) {
            meta.errors = new ArrayList<>();
        }
        meta.errors.add(error);
    }

    public String groupId() {
        return attributes.groupId;
    }

    public boolean simpleConsumerGroup() {
        return attributes.simpleConsumerGroup;
    }

    public String state() {
        return attributes.state;
    }

    public Collection<MemberDescription> members() {
        return attributes.members;
    }

    public void members(Collection<MemberDescription> members) {
        attributes.members = members;
    }

    public void partitionAssignor(String partitionAssignor) {
        attributes.partitionAssignor = partitionAssignor;
    }

    public Node coordinator() {
        return attributes.coordinator;
    }

    public void coordinator(Node coordinator) {
        attributes.coordinator = coordinator;
    }

    public List<String> authorizedOperations() {
        return attributes.authorizedOperations;
    }

    public void authorizedOperations(List<String> authorizedOperations) {
        attributes.authorizedOperations = authorizedOperations;
    }

    public List<OffsetAndMetadata> offsets() {
        return attributes.offsets;
    }

    public void offsets(List<OffsetAndMetadata> offsets) {
        attributes.offsets = offsets;
    }

    public String type() {
        return attributes.type;
    }

    public void type(String type) {
        attributes.type = type;
    }

    public String protocol() {
        return attributes.protocol;
    }

    public void protocol(String protocol) {
        attributes.protocol = protocol;
    }

    /**
     * Constructs a "cursor" ConsumerGroup from the encoded string representation of the subset
     * of Topic fields used to compare entities for pagination/sorting.
     */
    public static ConsumerGroup fromCursor(JsonObject cursor) {
        if (cursor == null) {
            return null;
        }

        JsonObject attr = cursor.getJsonObject("attributes");
        var group = new ConsumerGroup(attr.getString(Fields.GROUP_ID, null),
                attr.getBoolean(Fields.SIMPLE_CONSUMER_GROUP, false),
                attr.getString(Fields.STATE, null));

        group.type(attr.getString(Fields.TYPE, null));
        group.protocol(attr.getString(Fields.PROTOCOL, null));

        return group;
    }

    public String toCursor(List<String> sortFields) {
        JsonObjectBuilder cursor = Json.createObjectBuilder()
                .add("id", attributes.groupId);
        JsonObjectBuilder attrBuilder = Json.createObjectBuilder();

        if (sortFields.contains(Fields.SIMPLE_CONSUMER_GROUP)) {
            attrBuilder.add(Fields.SIMPLE_CONSUMER_GROUP, attributes.simpleConsumerGroup);
        }

        if (sortFields.contains(Fields.GROUP_ID)) {
            attrBuilder.add(Fields.GROUP_ID, attributes.groupId);
        }

        if (sortFields.contains(Fields.TYPE)) {
            attrBuilder.add(Fields.TYPE, attributes.type);
        }

        if (sortFields.contains(Fields.PROTOCOL)) {
            attrBuilder.add(Fields.PROTOCOL, attributes.protocol);
        }

        if (sortFields.contains(Fields.STATE)) {
            attrBuilder.add(Fields.STATE, attributes.state);
        }

        cursor.add("attributes", attrBuilder.build());

        return Base64.getUrlEncoder().encodeToString(cursor.build().toString().getBytes(StandardCharsets.UTF_8));
    }
}
