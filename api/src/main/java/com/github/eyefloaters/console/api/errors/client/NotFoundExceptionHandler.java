package com.github.eyefloaters.console.api.errors.client;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionHandler extends AbstractNotFoundExceptionHandler<NotFoundException> {
}
