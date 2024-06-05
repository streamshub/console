package com.github.streamshub.console.api.errors.client;

import java.util.List;

public class InvalidPageCursorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final List<String> sources;

    public InvalidPageCursorException(String message, List<String> sources) {
        super(message);
        this.sources = sources;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

    public List<String> getSources() {
        return sources;
    }
}
