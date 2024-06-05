package com.github.streamshub.console.api.errors;

import java.util.concurrent.CompletionException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class CompletionExceptionHandler extends UnwrappingExceptionHandler<CompletionException> {
}
