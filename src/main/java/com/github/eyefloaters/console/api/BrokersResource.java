package com.github.eyefloaters.console.api;

import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.kafka.clients.admin.Admin;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.github.eyefloaters.console.api.model.ConfigEntry;
import com.github.eyefloaters.console.api.service.BrokerService;

@Path("/api/kafkas/{clusterId}/nodes")
@Tag(name = "Kafka Cluster Resources")
public class BrokersResource {

    @Inject
    Supplier<Admin> clientSupplier;

    @Inject
    BrokerService brokerService;

    @Parameter(description = "Cluster identifier")
    @PathParam("clusterId")
    String clusterId;

    @GET
    @Path("{nodeId}/configs")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", ref = "Configurations", content = @Content())
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public CompletionStage<Response> describeConfigs(
            @PathParam("nodeId")
            @Parameter(description = "Node identifier")
            String nodeId) {

        return brokerService.describeConfigs(nodeId)
            .thenApply(ConfigEntry.ConfigResponse::new)
            .thenApply(Response::ok)
            .thenApply(Response.ResponseBuilder::build);
    }

}
