package io.strimzi.kafka.instance.model;

public class MemberDescription {

    private String memberId;
    private String groupInstanceId;
    private String clientId;
    private String host;
    private MemberAssignment assignment;

    public static MemberDescription fromKafkaModel(org.apache.kafka.clients.admin.MemberDescription description) {
        MemberDescription result = new MemberDescription();
        result.memberId = description.consumerId();
        result.groupInstanceId = description.groupInstanceId().orElse(null);
        result.clientId = description.clientId();
        result.host = description.host();
        result.assignment = MemberAssignment.fromKafkaModel(description.assignment());

        return result;
    }

    public String getMemberId() {
        return memberId;
    }

    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }

    public String getGroupInstanceId() {
        return groupInstanceId;
    }

    public void setGroupInstanceId(String groupInstanceId) {
        this.groupInstanceId = groupInstanceId;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public MemberAssignment getAssignment() {
        return assignment;
    }

    public void setAssignment(MemberAssignment assignment) {
        this.assignment = assignment;
    }

}
