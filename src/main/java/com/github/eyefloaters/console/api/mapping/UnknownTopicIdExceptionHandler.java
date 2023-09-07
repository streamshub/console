package com.github.eyefloaters.console.api.mapping;

import jakarta.ws.rs.ext.Provider;

import org.apache.kafka.common.errors.UnknownTopicIdException;

@Provider
public class UnknownTopicIdExceptionHandler extends AbstractNotFoundExceptionHandler<UnknownTopicIdException> {
}
