package com.github.streamshub.console.api.support;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.model.Error;
import com.github.streamshub.console.api.model.ErrorResponse;

@Provider
public class EnabledOperationFilter extends AbstractOperationFilter implements ContainerRequestFilter {

    private static final Logger LOGGER = Logger.getLogger(EnabledOperationFilter.class);
    private static final ErrorCategory CATEGORY = ErrorCategory.get(ErrorCategory.MethodNotAllowed.class);

    @Inject
    ResourceInfo resource;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if (disabled(requestContext.getMethod(), operationId())) {
            rejectRequest(requestContext);
        }
    }

    void rejectRequest(ContainerRequestContext requestContext) {
        Error error = CATEGORY.createError(
                "Method '%s' is not allowed for the requested resource".formatted(requestContext.getMethod()),
                null, null);
        LOGGER.debugf("error=%s", error);

        requestContext.abortWith(Response.status(CATEGORY.getHttpStatus())
                .entity(new ErrorResponse(List.of(error)))
                .build());
    }

    String operationId() {
        Method resourceMethod = resource.getResourceMethod();

        return Optional.ofNullable(resourceMethod.getAnnotation(Operation.class))
                .map(Operation::operationId)
                .orElseGet(resourceMethod::getName);
    }
}
