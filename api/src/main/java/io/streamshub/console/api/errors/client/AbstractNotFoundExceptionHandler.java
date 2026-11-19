package io.streamshub.console.api.errors.client;

import io.streamshub.console.api.support.ErrorCategory;

abstract class AbstractNotFoundExceptionHandler<T extends Throwable> extends AbstractClientExceptionHandler<T> {

    AbstractNotFoundExceptionHandler() {
        super(ErrorCategory.ResourceNotFound.class, null, (String) null);
    }

}
