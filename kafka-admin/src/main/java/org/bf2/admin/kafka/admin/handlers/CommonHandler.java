package org.bf2.admin.kafka.admin.handlers;

import io.strimzi.kafka.oauth.validator.TokenExpiredException;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.AuthenticationException;
import org.apache.kafka.common.errors.AuthorizationException;
import org.apache.kafka.common.errors.GroupIdNotFoundException;
import org.apache.kafka.common.errors.GroupNotEmptyException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.apache.kafka.common.errors.InvalidPartitionsException;
import org.apache.kafka.common.errors.InvalidRequestException;
import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.LeaderNotAvailableException;
import org.apache.kafka.common.errors.PolicyViolationException;
import org.apache.kafka.common.errors.SslAuthenticationException;
import org.apache.kafka.common.errors.TimeoutException;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.errors.UnknownMemberIdException;
import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;
import org.bf2.admin.kafka.admin.model.AdminServerException;
import org.bf2.admin.kafka.admin.model.ErrorType;
import org.bf2.admin.kafka.admin.model.Types;
import org.jboss.logging.Logger;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.Response.Status.Family;

import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public class CommonHandler {

    static final Logger log = Logger.getLogger(CommonHandler.class);

    private CommonHandler() {
    }

    static class ErrorHandler<T extends Throwable> {
        Class<T> causeType;
        Function<T, ResponseBuilder> mapper;

        public ErrorHandler(Class<T> causeType, Function<T, ResponseBuilder> mapper) {
            this.causeType = causeType;
            this.mapper = mapper;
        }
    }

    static List<ErrorHandler<?>> errorHandlers = List.of(
            entry(AdminServerException.class, thrown -> errorResponse(thrown, thrown.getError(), thrown.getMessage())),

            // 400 Bad Request
            entry(InvalidTopicException.class, thrown -> errorResponse(thrown, ErrorType.TOPIC_PARTITION_INVALID)),
            entry(PolicyViolationException.class, thrown -> errorResponse(thrown, ErrorType.POLICY_VIOLATION, thrown.getMessage())),
            entry(InvalidRequestException.class, thrown -> errorResponse(thrown, ErrorType.INVALID_CONFIGURATION, thrown.getMessage())),
            entry(InvalidConfigurationException.class, thrown -> errorResponse(thrown, ErrorType.INVALID_CONFIGURATION, thrown.getMessage())),
            entry(InvalidPartitionsException.class, thrown -> errorResponse(thrown, ErrorType.TOPIC_PARTITION_INVALID, thrown.getMessage())),
            entry(com.fasterxml.jackson.core.JsonParseException.class, thrown -> errorResponse(thrown, ErrorType.INVALID_REQUEST_FORMAT)),
            entry(com.fasterxml.jackson.databind.JsonMappingException.class, thrown -> errorResponse(thrown, ErrorType.INVALID_REQUEST_FORMAT)),
            entry(UnknownMemberIdException.class, thrown -> errorResponse(thrown, ErrorType.UNKNOWN_MEMBER)),

            // 500 Internal Server Error
            entry(SslAuthenticationException.class, thrown -> errorResponse(thrown, ErrorType.SERVER_ERROR)),

            // 401 Unauthorized
            entry(AuthenticationException.class, thrown -> errorResponse(thrown, ErrorType.NOT_AUTHENTICATED)),
            entry(TokenExpiredException.class, thrown -> errorResponse(thrown, ErrorType.NOT_AUTHENTICATED)),
            entry(IllegalStateException.class, thrown -> errorResponse(thrown, ErrorType.NOT_AUTHENTICATED)),
            entry(GeneralSecurityException.class, thrown -> errorResponse(thrown, ErrorType.NOT_AUTHENTICATED)),

            // 403 Forbidden
            entry(AuthorizationException.class, thrown -> errorResponse(thrown, ErrorType.NOT_AUTHORIZED)),

            // 404 Not Found
            entry(UnknownTopicOrPartitionException.class, thrown -> errorResponse(thrown, ErrorType.TOPIC_NOT_FOUND)),
            entry(GroupIdNotFoundException.class, thrown -> errorResponse(thrown, ErrorType.GROUP_NOT_FOUND)),
            entry(NotFoundException.class, thrown -> errorResponse(thrown, ErrorType.RESOURCE_NOT_FOUND)),

            // 409 Conflict
            entry(TopicExistsException.class, thrown -> errorResponse(thrown, ErrorType.TOPIC_DUPLICATED)),

            // 423 Locked
            entry(GroupNotEmptyException.class, thrown -> errorResponse(thrown, ErrorType.GROUP_NOT_EMPTY)),

            // 503 Service Unavailable
            entry(TimeoutException.class, thrown -> errorResponse(thrown, ErrorType.CLUSTER_NOT_AVAILABLE)),
            entry(LeaderNotAvailableException.class, thrown -> errorResponse(thrown, ErrorType.CLUSTER_NOT_AVAILABLE)),

            // Other
            entry(KafkaException.class, thrown -> {
                if (thrown.getMessage().contains("Failed to find brokers to send")) {
                    return errorResponse(thrown, ErrorType.CLUSTER_NOT_AVAILABLE);
                } else if (thrown.getMessage().contains("JAAS configuration")) {
                    return errorResponse(thrown, ErrorType.NOT_AUTHORIZED);
                } else {
                    log.error("Unknown exception", thrown);
                    return errorResponse(thrown, ErrorType.SERVER_ERROR);
                }
            }));

    static <T extends Throwable> ErrorHandler<T> entry(Class<T> key, Function<T, ResponseBuilder> value) {
        return new ErrorHandler<>(key, value);
    }

    static ResponseBuilder errorResponse(Throwable cause, ErrorType errorType) {
        return errorResponse(cause, errorType, null);
    }

    static ResponseBuilder errorResponse(Throwable cause, ErrorType errorType, String detail) {
        if (errorType.getHttpStatus().getFamily() == Family.SERVER_ERROR) {
            log.errorf(cause, "%s %s", cause.getClass(), cause.getMessage());
        } else {
            log.warnf("%s %s", cause.getClass(), cause.getMessage());
        }

        Types.Error errorEntity = Types.Error.forErrorType(errorType);
        errorEntity.setCode(errorType.getHttpStatus().getStatusCode());
        errorEntity.setErrorMessage(errorType.getReason());
        errorEntity.setDetail(detail);

        return Response.status(errorType.getHttpStatus()).entity(errorEntity);
    }

    @SuppressWarnings("unchecked")
    public static ResponseBuilder mapCause(Throwable error, Class<? extends Throwable> searchCause, Function<? extends Throwable, ResponseBuilder> mapper) {
        Throwable cause = error;

        do {
            if (searchCause.isInstance(cause)) {
                return ((Function<Throwable, ResponseBuilder>) mapper).apply(cause);
            }
        } while (cause != cause.getCause() && (cause = cause.getCause()) != null);

        return null;
    }

    static ResponseBuilder processFailure(Throwable thrown) {
        return errorHandlers.stream()
            .map(e -> mapCause(thrown, e.causeType, e.mapper))
            .filter(Objects::nonNull)
            .findFirst()
            .orElseGet(() -> {
                log.error("Unknown exception", thrown);
                return errorResponse(thrown, ErrorType.SERVER_ERROR);
            });
    }

}
