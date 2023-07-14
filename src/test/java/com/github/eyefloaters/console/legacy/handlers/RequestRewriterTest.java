package com.github.eyefloaters.console.legacy.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import com.github.eyefloaters.console.legacy.HttpMetrics;

import io.micrometer.core.instrument.Counter;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestRewriterTest {

    RoutingContext context;
    HttpServerRequest request;
    Counter deprecatedCounter;
    RequestRewriter target;

    @BeforeEach
    void setup() {
        context = Mockito.mock(RoutingContext.class);
        request = Mockito.mock(HttpServerRequest.class);
        when(context.request()).thenReturn(request);

        HttpMetrics httpMetrics = Mockito.mock(HttpMetrics.class);
        deprecatedCounter = mock(Counter.class);
        when(httpMetrics.getDeprecatedRequestCounter(anyString())).thenReturn(deprecatedCounter);

        target = new RequestRewriter();
        target.httpMetrics = httpMetrics;
    }

    @ParameterizedTest
    @CsvSource({
        "/rest,                     /api/v1",
        "/rest/openapi?format=JSON, /openapi?format=JSON",
        "/rest/topics,              /api/v1/topics",
    })
    void testDeprecatedRequestsForwarded(String original, String forwarded) {
        when(request.uri()).thenReturn(original);

        target.filterRequest(context);

        verify(context, times(1)).reroute(forwarded);
        verify(context, never()).next();
        verify(deprecatedCounter, times(1)).increment();
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/api/v1",
        "/openapi?format=JSON",
        "/api/v1/topics",
    })
    void testCurrentRequestsProcessed(String uri) {
        when(request.uri()).thenReturn(uri);

        target.filterRequest(context);

        verify(context, times(1)).next();
        verify(context, never()).reroute(anyString());
        verify(deprecatedCounter, never()).increment();
    }
}
