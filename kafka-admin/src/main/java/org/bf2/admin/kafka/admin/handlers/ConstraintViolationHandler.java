package org.bf2.admin.kafka.admin.handlers;

import org.bf2.admin.kafka.admin.model.ErrorType;
import org.bf2.admin.kafka.admin.model.Types;

import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import javax.validation.Path.Node;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Provider
public class ConstraintViolationHandler implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        final String detail = exception.getConstraintViolations().stream()
                .map(violation ->
                String.format("%s %s", lastNode(violation.getPropertyPath()), violation.getMessage()))
            .collect(Collectors.joining(", "));

        ErrorType errorType = ErrorType.INVALID_REQUEST;
        Types.Error errorEntity = Types.Error.forErrorType(errorType);
        errorEntity.setDetail(detail);
        errorEntity.setCode(errorType.getHttpStatus().getStatusCode());
        errorEntity.setErrorMessage(errorType.getReason());

        return Response.status(errorType.getHttpStatus()).entity(errorEntity).build();
    }

    String lastNode(Path propertyPath) {
        return StreamSupport.stream(propertyPath.spliterator(), false)
            .reduce((first, second) -> second)
            .map(Node::toString)
            .orElse("");
    }
}
