package com.github.streamshub.console.api.errors.client;

import java.util.List;
import java.util.function.Function;

import org.jboss.logging.Logger;

import com.github.streamshub.console.api.errors.AbstractExceptionHandler;
import com.github.streamshub.console.api.model.Error;
import com.github.streamshub.console.api.support.ErrorCategory;

import static java.util.Objects.requireNonNullElseGet;

abstract class AbstractClientExceptionHandler<T extends Throwable> extends AbstractExceptionHandler<T> {

    private static final Logger LOGGER = Logger.getLogger(AbstractClientExceptionHandler.class);

    protected final String message;
    protected final Function<T, String> propertyMapper;

    AbstractClientExceptionHandler(Class<? extends ErrorCategory> categoryType, String message, Function<T, String> propertyMapper) {
        super(categoryType);
        this.message = message;
        this.propertyMapper = propertyMapper;
    }

    AbstractClientExceptionHandler(Class<? extends ErrorCategory> categoryType, String message, String property) {
        this(categoryType, message, exception -> property);
    }

    @Override
    public List<Error> buildErrors(T exception) {
        String property = propertyMapper.apply(exception);
        Error error = category.createError(requireNonNullElseGet(message, exception::getMessage), exception, property);
        LOGGER.debugf("error=%s", error);
        return List.of(error);
    }

}
