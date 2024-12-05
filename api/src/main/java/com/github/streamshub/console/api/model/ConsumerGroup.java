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

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.streamshub.console.api.support.ComparatorBuilder;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.ListRequestContext;

import io.xlate.validation.constraints.Expression;

import static java.util.Comparator.comparing;
import static java.util.Comparator.nullsLast;

@Schema(name = "ConsumerGroupAttributes")
@JsonFilter("fieldFilter")
public class ConsumerGroup {

    public static final String API_TYPE = "consumerGroups";
    public static final String FIELDS_PARAM = "fields[" + API_TYPE + "]";

    public static final class Fields {
        public static final String STATE = "state";
        public static final String MEMBERS = "members";
        public static final String COORDINATOR = "coordinator";
        public static final String AUTHORIZED_OPERATIONS = "authorizedOperations";
        public static final String PARTITION_ASSIGNOR = "partitionAssignor";
        public static final String OFFSETS = "offsets";
        public static final String SIMPLE_CONSUMER_GROUP = "simpleConsumerGroup";

        static final Comparator<ConsumerGroup> ID_COMPARATOR =
                comparing(ConsumerGroup::getGroupId);

        static final Map<String, Map<Boolean, Comparator<ConsumerGroup>>> COMPARATORS = ComparatorBuilder.bidirectional(
                Map.of("id", ID_COMPARATOR,
                        STATE, nullsLast(comparing(ConsumerGroup::getState)),
                        SIMPLE_CONSUMER_GROUP, comparing(ConsumerGroup::isSimpleConsumerGroup)));

        public static final ComparatorBuilder<ConsumerGroup> COMPARATOR_BUILDER =
                new ComparatorBuilder<>(ConsumerGroup.Fields::comparator, ConsumerGroup.Fields.defaultComparator());

        public static final String LIST_DEFAULT = STATE + ", " + SIMPLE_CONSUMER_GROUP;
        public static final String DESCRIBE_DEFAULT = STATE + ", " + MEMBERS + ", " + COORDINATOR + ", " + OFFSETS
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

    @Schema(name = "ConsumerGroupListDocument")
    public static final class ListResponse extends DataList<ConsumerGroupResource> {
        public ListResponse(List<ConsumerGroup> data, ListRequestContext<ConsumerGroup> listSupport) {
            super(data.stream()
                    .map(entry -> {
                        var rsrc = new ConsumerGroupResource(entry);
                        rsrc.addMeta("page", listSupport.buildPageMeta(entry::toCursor));
                        return rsrc;
                    })
                    .toList());
            addMeta("page", listSupport.buildPageMeta());
            listSupport.buildPageLinks(ConsumerGroup::toCursor).forEach(this::addLink);
        }
    }

    @Schema(name = "ConsumerGroupDocument")
    public static final class ConsumerGroupDocument extends DataSingleton<ConsumerGroupResource> {
        /**
         * Used by patch
         */
        @JsonCreator
        public ConsumerGroupDocument(@JsonProperty("data") ConsumerGroupResource data) {
            super(data);
        }

        /**
         * Used by list and describe
         */
        public ConsumerGroupDocument(ConsumerGroup attributes) {
            super(new ConsumerGroupResource(attributes));
        }
    }

    @Schema(name = "ConsumerGroup")
    @Expression(
        value = "self.id != null",
        message = "resource ID is required",
        node = "id",
        payload = ErrorCategory.InvalidResource.class)
    @Expression(
        when = "self.type != null",
        value = "self.type == '" + API_TYPE + "'",
        message = "resource type conflicts with operation",
        node = "type",
        payload = ErrorCategory.ResourceConflict.class)
    public static final class ConsumerGroupResource extends Resource<ConsumerGroup> {
        /**
         * Used by patch
         */
        @JsonCreator
        public ConsumerGroupResource(String id, String type, ConsumerGroup attributes) {
            super(id, type, new ConsumerGroup(id, attributes));
        }

        /**
         * Used by list and describe
         */
        public ConsumerGroupResource(ConsumerGroup attributes) {
            super(attributes.groupId, API_TYPE, attributes);

            if (attributes.errors != null) {
                addMeta("errors", attributes.errors);
            }
        }
    }

    // Available via list or describe operations
    @JsonIgnore
    private final String groupId;
    private final boolean simpleConsumerGroup;
    private final String state;

    // Available via describe operation only

    private Collection<MemberDescription> members = Collections.emptyList();
    private String partitionAssignor;
    private Node coordinator;
    private List<String> authorizedOperations;

    // Available via list offsets operation only

    private List<@Valid OffsetAndMetadata> offsets = Collections.emptyList();

    // When a describe error occurs
    private List<Error> errors;

    private ConsumerGroup(String id, ConsumerGroup other) {
        this(id,
             other != null && other.simpleConsumerGroup,
             other != null ? other.state : null);

        Optional.ofNullable(other)
            .map(ConsumerGroup::getOffsets)
            .ifPresent(this::setOffsets);
    }

    @JsonCreator
    public ConsumerGroup(String groupId, boolean simpleConsumerGroup, String state) {
        this.groupId = groupId;
        this.simpleConsumerGroup = simpleConsumerGroup;
        this.state = state;
    }

    public static ConsumerGroup fromKafkaModel(org.apache.kafka.clients.admin.ConsumerGroupListing listing) {
        return new ConsumerGroup(listing.groupId(), listing.isSimpleConsumerGroup(), listing.state().map(Enum::name).orElse(null));
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
                Optional.ofNullable(description.state()).map(Enum::name).orElse(null));

        group.setPartitionAssignor(description.partitionAssignor());
        group.setCoordinator(Node.fromKafkaModel(description.coordinator()));
        group.setMembers(description.members()
                .stream()
                .map(member -> MemberDescription.fromKafkaModel(member, topicIds))
                .toList());

        group.setAuthorizedOperations(Optional.ofNullable(description.authorizedOperations())
                .map(Collection::stream)
                .map(ops -> ops.map(Enum::name).toList())
                .orElse(null));

        return group;
    }

    public void addError(Error error) {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        errors.add(error);
    }

    public String getGroupId() {
        return groupId;
    }

    public boolean isSimpleConsumerGroup() {
        return simpleConsumerGroup;
    }

    public String getState() {
        return state;
    }

    public Collection<MemberDescription> getMembers() {
        return members;
    }

    public void setMembers(Collection<MemberDescription> members) {
        this.members = members;
    }

    public String getPartitionAssignor() {
        return partitionAssignor;
    }

    public void setPartitionAssignor(String partitionAssignor) {
        this.partitionAssignor = partitionAssignor;
    }

    public Node getCoordinator() {
        return coordinator;
    }

    public void setCoordinator(Node coordinator) {
        this.coordinator = coordinator;
    }

    public List<String> getAuthorizedOperations() {
        return authorizedOperations;
    }

    public void setAuthorizedOperations(List<String> authorizedOperations) {
        this.authorizedOperations = authorizedOperations;
    }

    public List<OffsetAndMetadata> getOffsets() {
        return offsets;
    }

    public void setOffsets(List<OffsetAndMetadata> offsets) {
        this.offsets = offsets;
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

        return new ConsumerGroup(cursor.getString("id"),
                attr.getBoolean(Fields.SIMPLE_CONSUMER_GROUP, false),
                attr.getString(Fields.STATE, null));
    }

    public String toCursor(List<String> sortFields) {
        JsonObjectBuilder cursor = Json.createObjectBuilder()
                .add("id", groupId);
        JsonObjectBuilder attrBuilder = Json.createObjectBuilder();

        if (sortFields.contains(Fields.SIMPLE_CONSUMER_GROUP)) {
            attrBuilder.add(Fields.SIMPLE_CONSUMER_GROUP, simpleConsumerGroup);
        }

        if (sortFields.contains(Fields.STATE)) {
            attrBuilder.add(Fields.STATE, state);
        }

        cursor.add("attributes", attrBuilder.build());

        return Base64.getUrlEncoder().encodeToString(cursor.build().toString().getBytes(StandardCharsets.UTF_8));
    }
}
