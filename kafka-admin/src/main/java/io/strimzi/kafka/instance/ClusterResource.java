package io.strimzi.kafka.instance;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;

import io.strimzi.kafka.instance.model.Cluster;
import io.strimzi.kafka.instance.service.ClusterService;

@Path("/api/v2/cluster")
public class ClusterResource {

    @Inject
    ClusterService clusterService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(Cluster.class)
    public CompletionStage<Response> describeCluster() {
        return clusterService.describeCluster()
            .thenApply(Response::ok)
            .exceptionally(error -> Response.serverError().entity(error.getMessage()))
            .thenApply(Response.ResponseBuilder::build);
    }

}
