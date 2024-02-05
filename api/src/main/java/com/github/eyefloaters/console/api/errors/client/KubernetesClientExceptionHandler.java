package com.github.eyefloaters.console.api.errors.client;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.support.ErrorCategory;

import io.fabric8.kubernetes.client.KubernetesClientException;

@Provider
@ApplicationScoped
public class KubernetesClientExceptionHandler extends AbstractClientExceptionHandler<KubernetesClientException> {

    private static final Logger LOGGER = Logger.getLogger(KubernetesClientExceptionHandler.class);

    public KubernetesClientExceptionHandler() {
        super(ErrorCategory.ServerError.class, "Unexpected Kubernetes error", (String) null);
    }

    @Override
    public boolean handlesException(Throwable thrown) {
        return thrown instanceof KubernetesClientException;
    }

    @Override
    public List<Error> buildErrors(KubernetesClientException exception) {
        switch (exception.getCode()) {
            case 400:
                return buildErrors(ErrorCategory.InvalidResource.class, exception);
            case 401:
                return buildErrors(ErrorCategory.NotAuthenticated.class, exception);
            case 403:
                return buildErrors(ErrorCategory.NotAuthorized.class, exception);
            default:
                return super.buildErrors(exception);
        }
    }

    private List<Error> buildErrors(Class<? extends ErrorCategory> categoryType, KubernetesClientException exception) {
        ErrorCategory category = ErrorCategory.get(categoryType);
        Error error = category.createError(exception.getMessage(), exception, null);
        LOGGER.debugf("error=%s", error);
        return List.of(error);
    }
}
