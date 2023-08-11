package com.github.eyefloaters.console.api;

import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.github.eyefloaters.console.api.model.KafkaCluster;
import com.github.eyefloaters.console.api.service.KafkaClusterService;

@Path("/api/kafkas")
@Tag(name = "Kafka Cluster Resources")
public class KafkaClustersResource {

    @Inject
    KafkaClusterService clusterService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(KafkaCluster.ListResponse.class)
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public Response listClusters() {
        var responseEntity = new KafkaCluster.ListResponse(clusterService.listClusters());
        return Response.ok(responseEntity).build();
    }

    @GET
    @Path("{clusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(KafkaCluster.SingleResponse.class)
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public CompletionStage<Response> describeCluster(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId) {

        return clusterService.describeCluster()
            .thenApply(KafkaCluster.SingleResponse::new)
            .thenApply(Response::ok)
            .thenApply(Response.ResponseBuilder::build);
    }

}
