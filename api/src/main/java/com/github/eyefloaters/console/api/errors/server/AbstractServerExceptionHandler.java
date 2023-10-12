package com.github.eyefloaters.console.api.errors.server;

import java.util.List;

import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.errors.AbstractExceptionHandler;
import com.github.eyefloaters.console.api.model.Error;
import com.github.eyefloaters.console.api.support.ErrorCategory;

abstract class AbstractServerExceptionHandler<T extends Throwable> extends AbstractExceptionHandler<T> {

    private static final Logger LOGGER = Logger.getLogger(AbstractServerExceptionHandler.class);

    AbstractServerExceptionHandler(Class<? extends ErrorCategory> categoryType) {
        super(categoryType);
    }

    @Override
    public List<Error> buildErrors(T exception) {
        Error error = category.createError(exception.getMessage(), exception, null);
        LOGGER.warnf(exception, "error=%s", error);
        return List.of(error);
    }

}
