package org.bf2.admin.kafka.admin.handlers;

import org.bf2.admin.kafka.admin.HttpMetrics;

import javax.inject.Inject;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;

import java.io.IOException;

@Provider
public class RequestMetricsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Inject
    HttpMetrics httpMetrics;

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        httpMetrics.getRequestsCounter().increment();
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        int statusCode = responseContext.getStatus();

        if (statusCode < 300) {
            httpMetrics.getSucceededRequestsCounter().increment();
        } else {
            httpMetrics.getFailedRequestsCounter(statusCode).increment();
        }
    }
}
