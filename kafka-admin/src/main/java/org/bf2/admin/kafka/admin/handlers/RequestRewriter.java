package org.bf2.admin.kafka.admin.handlers;

import io.quarkus.vertx.web.RouteFilter;
import io.vertx.ext.web.RoutingContext;
import org.bf2.admin.kafka.admin.HttpMetrics;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class RequestRewriter {

    private static final Logger LOG = Logger.getLogger(RequestRewriter.class);
    private static final String REST = "/rest";
    private static final String OPENAPI = "/openapi";

    @Inject
    HttpMetrics httpMetrics;

    @RouteFilter(400)
    void filterRequest(RoutingContext context) {
        String requestUri = context.request().uri();

        if (requestUri.startsWith(REST)) {
            httpMetrics.getDeprecatedRequestCounter(requestUri).increment();

            String remainingPath = requestUri.substring(REST.length());
            String target = remainingPath.startsWith(OPENAPI) ? remainingPath : "/api/v1" + remainingPath;
            LOG.infof("Rerouting deprecated request: %s -> %s", requestUri, target);

            context.reroute(target);
            return;
        }

        context.next();
    }
}
