package com.github.eyefloaters.console.api.support;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;

public enum ErrorCategory {

    INVALID_QUERY_PARAMETER("1000", "Invalid query parameter", Status.BAD_REQUEST, Source.PARAMETER);

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
