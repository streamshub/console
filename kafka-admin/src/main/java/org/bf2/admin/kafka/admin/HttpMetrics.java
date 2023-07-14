package org.bf2.admin.kafka.admin;

import io.micrometer.core.instrument.Counter;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

@ApplicationScoped
public class HttpMetrics {
    private static final String FAILED_REQUESTS_COUNTER = "failed_requests";
    private static final String HTTP_STATUS_CODE = "status_code";
    private static final String DEPRECATED_REQUESTS_COUNTER = "deprecated_requests";
    private static final String DEPRECATED_REQUESTS_PATH = "path";

    @Inject
    PrometheusMeterRegistry meterRegistry;

    private Counter requestsCounter;
    private Counter openApiCounter;
    private Counter succeededRequestsCounter;

    public void init(@Observes StartupEvent event) {
        requestsCounter = meterRegistry.counter("requests");
        openApiCounter = meterRegistry.counter("requests_openapi");
        succeededRequestsCounter = meterRegistry.counter("succeeded_requests");

        /*
         * Status code 404 is a placeholder for defining the status_code label.
         */
        meterRegistry.counter(FAILED_REQUESTS_COUNTER, HTTP_STATUS_CODE, "404");
        meterRegistry.counter(DEPRECATED_REQUESTS_COUNTER, DEPRECATED_REQUESTS_PATH, "/rest/openapi");

    }

    public PrometheusMeterRegistry getRegistry() {
        return meterRegistry;
    }

    public Counter getFailedRequestsCounter(int httpStatusCode) {
        return getRegistry().counter(FAILED_REQUESTS_COUNTER, HTTP_STATUS_CODE, String.valueOf(httpStatusCode));
    }

    public Counter getRequestsCounter() {
        return requestsCounter;
    }

    public Counter getOpenApiCounter() {
        return openApiCounter;
    }

    public Counter getSucceededRequestsCounter() {
        return succeededRequestsCounter;
    }

    public Counter getDeprecatedRequestCounter(String path) {
        return getRegistry().counter(DEPRECATED_REQUESTS_COUNTER, DEPRECATED_REQUESTS_PATH, path);
    }
}
