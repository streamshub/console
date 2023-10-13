package com.github.eyefloaters.console.api.errors.client;

import com.github.eyefloaters.console.api.support.ErrorCategory;

abstract class AbstractNotFoundExceptionHandler<T extends Throwable> extends AbstractClientExceptionHandler<T> {

    AbstractNotFoundExceptionHandler() {
        super(ErrorCategory.ResourceNotFound.class, null, null);
    }

}
