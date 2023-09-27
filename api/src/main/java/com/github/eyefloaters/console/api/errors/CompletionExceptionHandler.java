package com.github.eyefloaters.console.api.errors;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.errors.server.UnhandledThrowableHandler;

@Provider
@ApplicationScoped
public class CompletionExceptionHandler implements ExceptionMapper<CompletionException> {

    private static final Logger LOGGER = Logger.getLogger(CompletionExceptionHandler.class);

    @Inject
    UnhandledThrowableHandler fallbackMapper;

    @Inject
    Instance<SelectableExceptionMapper<? extends Throwable>> availableMappers;

    private final Map<Type, ExceptionMapper<Throwable>> mappers = new HashMap<>();

    @PostConstruct
    public void initialize() {
        availableMappers.forEach(this::storeMapper);
    }

    @Override
    public Response toResponse(CompletionException exception) {
        Throwable cause = exception.getCause();
        return mappers.getOrDefault(cause.getClass(), fallbackMapper).toResponse(cause);
    }

    @SuppressWarnings("unchecked")
    private void storeMapper(SelectableExceptionMapper<? extends Throwable> mapper) {
        Type superType = mapper.getClass().getGenericSuperclass();

        /*
         * Climb hierarchy until we find a non-class type (hopefully parameterized)
         * with the exception as the parameter.
         */
        while (superType instanceof Class<?> superClass) {
            superType = superClass.getGenericSuperclass();
        }

        if (superType instanceof ParameterizedType pType) {
            Type exceptionType = pType.getActualTypeArguments()[0];
            LOGGER.debugf("Mapping type %s to error mapper %s", exceptionType.getTypeName(), superType.getTypeName());
            mappers.put(exceptionType, (ExceptionMapper<Throwable>) mapper);
        } else {
            LOGGER.warnf("Mapper %s does not extend a ParameterizedType with Throwable type argument");
        }
    }
}
