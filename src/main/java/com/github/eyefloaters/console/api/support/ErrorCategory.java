package com.github.eyefloaters.console.api.support;

import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.Response.StatusType;

import com.github.eyefloaters.console.api.model.ErrorSource;

public enum ErrorCategory {

    INVALID_QUERY_PARAMETER("4001", "Invalid query parameter", Status.BAD_REQUEST, Source.PARAMETER),

    RESOURCE_NOT_FOUND("4041", "Resource not found", Status.NOT_FOUND),

    SERVER_ERROR("5001", "Unexpected error", Status.INTERNAL_SERVER_ERROR),

    BACKEND_TIMEOUT("5041", "Timed out waiting for backend service", Status.GATEWAY_TIMEOUT);


    public enum Source {
        PARAMETER {
            @Override
            public ErrorSource errorSource(String reference) {
                return new ErrorSource(null, reference, null);
            }
        },
        POINTER {
            @Override
            public ErrorSource errorSource(String reference) {
                return new ErrorSource(reference, null, null);
            }
        },
        HEADER {
            @Override
            public ErrorSource errorSource(String reference) {
                return new ErrorSource(null, null, reference);
            }
        },
        NONE {
            @Override
            public ErrorSource errorSource(String reference) {
                return null;
            }
        };

        public abstract ErrorSource errorSource(String reference);
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
