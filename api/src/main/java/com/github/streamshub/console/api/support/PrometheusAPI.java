package com.github.streamshub.console.api.support;

import java.time.Instant;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@ApplicationScoped
@RegisterRestClient(configKey = "prometheus")
@Path("/api/v1")
public interface PrometheusAPI {

    /**
     * Evaluates an instant query at a single point in time
     *
     * @see <a href="https://prometheus.io/docs/prometheus/latest/querying/api/#instant-queries">Instant queries</a>
     */
    @Path("query")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    JsonObject query(
            @QueryParam("query") String query,
            @QueryParam("time") Instant time);

    /**
     * Evaluates an expression query over a range of time
     *
     * @see <a href="https://prometheus.io/docs/prometheus/latest/querying/api/#range-queries">Range queries</a>
     */
    @Path("query_range")
    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.APPLICATION_JSON)
    JsonObject queryRange(
            @QueryParam("query") String query,
            @QueryParam("start") Instant start,
            @QueryParam("end") Instant end,
            @QueryParam("step") String step);

}
