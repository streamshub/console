package io.strimzi.kafka.instance.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ConsumerGroup {

    // Available via list or describe operations

    private String groupId;
    private boolean simpleConsumerGroup;
    private String state;

    // Available via describe operation only

    private Collection<MemberDescription> members;
    private String partitionAssignor;
    private Node coordinator;
    private List<String> authorizedOperations;

    // Available via list offsets operation only

    private Map<String, Map<Integer, OffsetAndMetadata>> offsets;

    // When a describe error occurs
    private List<Error> errors;

    public ConsumerGroup() {
    }

    public ConsumerGroup(String groupId, boolean simpleConsumerGroup, String state) {
        super();
        this.groupId = groupId;
        this.simpleConsumerGroup = simpleConsumerGroup;
        this.state = state;
    }

    public static ConsumerGroup fromListing(org.apache.kafka.clients.admin.ConsumerGroupListing listing) {
        return new ConsumerGroup(listing.groupId(), listing.isSimpleConsumerGroup(), listing.state().map(Enum::name).orElse(null));
    }

    public static ConsumerGroup fromDescription(org.apache.kafka.clients.admin.ConsumerGroupDescription description) {
        var group = new ConsumerGroup(
                description.groupId(),
                description.isSimpleConsumerGroup(),
                Optional.ofNullable(description.state()).map(Enum::name).orElse(null));

        group.setPartitionAssignor(description.partitionAssignor());
        group.setCoordinator(Node.fromKafkaModel(description.coordinator()));
        group.setMembers(description.members().stream().map(MemberDescription::fromKafkaModel).toList());

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

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public boolean isSimpleConsumerGroup() {
        return simpleConsumerGroup;
    }

    public void setSimpleConsumerGroup(boolean simpleConsumerGroup) {
        this.simpleConsumerGroup = simpleConsumerGroup;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
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

    public List<Error> getErrors() {
        return errors;
    }

    public void setErrors(List<Error> errors) {
        this.errors = errors;
    }

    public Map<String, Map<Integer, OffsetAndMetadata>> getOffsets() {
        return offsets;
    }

    public void setOffsets(Map<String, Map<Integer, OffsetAndMetadata>> offsets) {
        this.offsets = offsets;
    }
}
