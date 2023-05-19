package io.strimzi.kafka.instance;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;

import io.strimzi.kafka.instance.service.ClusterService;

@Path("/api/v2/cluster")
public class ClusterResource {

    @Inject
    ClusterService clusterService;

    @GET
    public CompletionStage<Response> describeCluster() {
        return clusterService.describeCluster()
            .thenApply(Response::ok)
            .exceptionally(error -> Response.serverError().entity(error.getMessage()))
            .thenApply(Response.ResponseBuilder::build);
    }

}
