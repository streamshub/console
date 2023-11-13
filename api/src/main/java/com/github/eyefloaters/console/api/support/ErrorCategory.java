package com.github.eyefloaters.console.api.support;

import java.util.Map;
import java.util.UUID;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Singleton;
import jakarta.validation.Payload;
import jakarta.ws.rs.core.Response.Status;

import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.model.ErrorSource;
import com.github.eyefloaters.console.api.model.ListFetchParams;

public abstract class ErrorCategory implements Payload {

    @SuppressWarnings("unchecked")
    public static <T extends ErrorCategory> T get(Class<? extends Payload> type) {
        return (T) CDI.current().select(type).get();
    }

    @Singleton
    public static class InvalidQueryParameter extends ErrorCategory {
        public InvalidQueryParameter() {
            super("4001", "Invalid query parameter", Status.BAD_REQUEST, Source.PARAMETER);
        }
    }

    /**
     * Special case HTTP 400 error for when the `page[size]` parameter exceeds the
     * server-defined limit.
     *
     * @see <a href=
     *      "https://jsonapi.org/profiles/ethanresnick/cursor-pagination/#errors-max-page-size-exceeded">Max
     *      Page Size Exceeded Error</a>
     */
    @Singleton
    public static class MaxPageSizeExceededError extends ErrorCategory {
        public static final String TYPE_LINK = "https://jsonapi.org/profiles/ethanresnick/cursor-pagination/max-size-exceeded";

        public MaxPageSizeExceededError() {
            super("4002", "Requested page size exceeds limit", Status.BAD_REQUEST, Source.PARAMETER);
        }

        @Override
        public Error createError(String message, Throwable cause, String property) {
            Error error = super.createError(message, cause, property);
            error.addMeta("page", Map.of("maxSize", ListFetchParams.PAGE_SIZE_MAX));
            error.addLink("type", TYPE_LINK);
            return error;
        }
    }

    @Singleton
    public static class InvalidResource extends ErrorCategory {
        public InvalidResource() {
            super("4003", "Invalid resource", Status.BAD_REQUEST, Source.POINTER);
        }
    }

    @Singleton
    public static class UncategorizedBadRequest extends ErrorCategory {
        public UncategorizedBadRequest() {
            super("4004", "Bad Request", Status.BAD_REQUEST);
        }
    }

    @Singleton
    public static class NotAuthenticated extends ErrorCategory {
        public NotAuthenticated() {
            super("4011", "Not Authenticated", Status.UNAUTHORIZED);
        }
    }

    @Singleton
    public static class NotAuthorized extends ErrorCategory {
        public NotAuthorized() {
            super("4031", "Not Authorized", Status.FORBIDDEN);
        }
    }

    @Singleton
    public static class ResourceNotFound extends ErrorCategory {
        public ResourceNotFound() {
            super("4041", "Resource not found", Status.NOT_FOUND);
        }
    }

    @Singleton
    public static class MethodNotAllowed extends ErrorCategory {
        public MethodNotAllowed() {
            super("4051", "Method not allowed", Status.METHOD_NOT_ALLOWED);
        }
    }

    @Singleton
    public static class ResourceConflict extends ErrorCategory {
        public ResourceConflict() {
            super("4091", "Resource conflict", Status.CONFLICT, Source.POINTER);
        }
    }

    @Singleton
    public static class UnsupportedMediaType extends ErrorCategory {
        public UnsupportedMediaType() {
            super("4151", "Unsupported payload format", Status.UNSUPPORTED_MEDIA_TYPE, Source.HEADER);
        }
    }

    @Singleton
    public static class ServerError extends ErrorCategory {
        public ServerError() {
            super("5001", "Unexpected error", Status.INTERNAL_SERVER_ERROR);
        }
    }

    @Singleton
    public static class BackendTimeout extends ErrorCategory {
        public BackendTimeout() {
            super("5041", "Timed out waiting for backend service", Status.GATEWAY_TIMEOUT);
        }
    }

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
    private Status httpStatus;
    private String code;
    private Source source;

    private ErrorCategory(String code, String title, Status httpStatus, Source source) {
        this.code = code;
        this.title = title;
        this.httpStatus = httpStatus;
        this.source = source;
    }

    private ErrorCategory(String code, String title, Status httpStatus) {
        this(code, title, httpStatus, Source.NONE);
    }

    public String getCode() {
        return code;
    }

    public String getTitle() {
        return title;
    }

    public Status getHttpStatus() {
        return httpStatus;
    }

    public Source getSource() {
        return source;
    }

    public Error createError(String message, Throwable cause, String property) {
        Error error = new Error(getTitle(), message, cause);
        error.setStatus(String.valueOf(getHttpStatus().getStatusCode()));
        error.setCode(getCode());
        if (property != null) {
            error.setSource(getSource().errorSource(property));
        }
        error.setId(UUID.randomUUID().toString());

        return error;
    }
}
