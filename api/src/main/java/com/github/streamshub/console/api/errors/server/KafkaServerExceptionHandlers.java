package com.github.streamshub.console.api.errors.server;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ext.Provider;

import org.apache.kafka.common.errors.SslAuthenticationException;

import com.github.streamshub.console.api.support.ErrorCategory;

public class KafkaServerExceptionHandlers {

    private KafkaServerExceptionHandlers() {
    }

    @Provider
    @ApplicationScoped
    public static class AuthenticationExceptionHandler
        extends AbstractServerExceptionHandler<SslAuthenticationException> {

        public AuthenticationExceptionHandler() {
            super(ErrorCategory.ServerError.class);
        }

        @Override
        public boolean handlesException(Throwable thrown) {
            return thrown instanceof SslAuthenticationException;
        }
    }
}
