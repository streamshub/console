package com.github.streamshub.console.api.errors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class WebApplicationExceptionHandler extends UnwrappingExceptionHandler<WebApplicationException> {
}
