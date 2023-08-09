package com.github.eyefloaters.console.api.support;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import org.apache.kafka.common.errors.UnknownTopicOrPartitionException;

@Provider
public class UnknownTopicOrPartitionExceptionHandler extends AbstractNotFoundExceptionHandler<UnknownTopicOrPartitionException> {

    @Override
    public Response toResponse(UnknownTopicOrPartitionException exception) {
        return super.toResponse(exception);
    }

}
