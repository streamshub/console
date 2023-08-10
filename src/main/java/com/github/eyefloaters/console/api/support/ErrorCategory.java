package com.github.eyefloaters.console.api.support;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;

public enum ErrorCategory {

    INVALID_QUERY_PARAMETER("4001", "Invalid query parameter", Status.BAD_REQUEST, Source.PARAMETER),

    RESOURCE_NOT_FOUND("4041", "Resource not found", Status.NOT_FOUND),

    SERVER_ERROR("5001", "Unexpected error", Status.INTERNAL_SERVER_ERROR),

    BACKEND_TIMEOUT("5041", "Timed out waiting for backend service", Status.GATEWAY_TIMEOUT);


    public enum Source {
        PARAMETER, PAYLOAD, HEADER, NONE
    }

    private String title;
    private StatusType httpStatus;
    private String code;
    private Source source;

    private ErrorCategory(String code, String title, StatusType httpStatus, Source source) {
        this.code = code;
        this.title = title;
        this.httpStatus = httpStatus;
        this.source = source;
    }

    private ErrorCategory(String code, String title, StatusType httpStatus) {
        this(code, title, httpStatus, Source.NONE);
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public StatusType getHttpStatus() {
        return httpStatus;
    }

    public Source getSource() {
        return source;
    }

}
