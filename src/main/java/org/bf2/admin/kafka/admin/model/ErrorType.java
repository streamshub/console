package org.bf2.admin.kafka.admin.model;

import org.bf2.admin.kafka.admin.AccessControlOperations;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.Status.Family;
import jakarta.ws.rs.core.Response.StatusType;

import java.util.Arrays;

public enum ErrorType {

    NOT_AUTHENTICATED("1", "Invalid or missing credentials", Status.UNAUTHORIZED),
    NOT_AUTHORIZED("2", "Client is not authorized to perform the requested operation", Status.FORBIDDEN),
    // Resource in URL not found
    TOPIC_NOT_FOUND("3", "No such topic", Status.NOT_FOUND),
    GROUP_NOT_FOUND("4", "No such consumer group", Status.NOT_FOUND),
    // Data in request body invalid
    TOPIC_PARTITION_INVALID("5", "Topic/Partition in request is invalid", Status.BAD_REQUEST),
    TOPIC_PARTITION_NOT_FOUND("6", "Topic/Partition in request does not exist", Status.BAD_REQUEST),
    INVALID_REQUEST_FORMAT("7", "Request body is malformed or invalid", Status.BAD_REQUEST),
    POLICY_VIOLATION("8", "Request violates topic configuration policy", Status.BAD_REQUEST),
    INVALID_CONFIGURATION("9", "Request contains invalid configuration", Status.BAD_REQUEST),
    //
    TOPIC_DUPLICATED("10", "Requested topic already exists", Status.CONFLICT),
    //
    GROUP_NOT_EMPTY("11", "Consumer group has connected clients", new Locked()),
    //
    CLUSTER_NOT_AVAILABLE("12", "Cluster not available", Status.SERVICE_UNAVAILABLE),
    //
    INVALID_ACL_RESOURCE_OP("13", AccessControlOperations.INVALID_ACL_RESOURCE_OPERATION, Status.BAD_REQUEST),
    //
    ERROR_NOT_FOUND("14", "No such error", Status.NOT_FOUND),
    //
    UNKNOWN_MEMBER("15", "Unknown consumer group member", Status.BAD_REQUEST),
    INVALID_REQUEST("16", "Request body or parameters invalid", Status.BAD_REQUEST),
    //
    RESOURCE_NOT_FOUND("17", "No such resource found", Status.NOT_FOUND),
    //
    SERVER_ERROR("99", "Server has encountered an unexpected error", Status.INTERNAL_SERVER_ERROR);

    public static boolean isCausedBy(Throwable error, Class<? extends Throwable> searchCause) {
        Throwable cause = error;

        do {
            if (searchCause.isInstance(cause)) {
                return true;
            }
        } while (cause != cause.getCause() && (cause = cause.getCause()) != null);

        return false;
    }

    public static ErrorType forCode(String code) {
        return Arrays.stream(values())
            .filter(v -> v.id.equals(code))
            .findFirst()
            .orElseThrow(() -> {
                throw new AdminServerException(ERROR_NOT_FOUND);
            });
    }

    private String id;
    private String reason;
    private StatusType httpStatus;

    private ErrorType(String id, String reason, StatusType httpStatus) {
        this.id = id;
        this.reason = reason;
        this.httpStatus = httpStatus;
    }

    public String getId() {
        return id;
    }

    public String getReason() {
        return reason;
    }

    public StatusType getHttpStatus() {
        return httpStatus;
    }

}

/**
 * 423 Locked (WebDAV, RFC4918)
 */
class Locked implements StatusType {
    @Override
    public int getStatusCode() {
        return 423;
    }

    @Override
    public Family getFamily() {
        return Family.CLIENT_ERROR;
    }

    @Override
    public String getReasonPhrase() {
        return "Locked";
    }
}
