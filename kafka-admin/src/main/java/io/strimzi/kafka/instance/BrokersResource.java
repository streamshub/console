package io.strimzi.kafka.instance;

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
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;

import io.strimzi.kafka.instance.model.ConfigEntry;
import io.strimzi.kafka.instance.service.BrokerService;

@Path("/api/v2/brokers")
public class BrokersResource {

    @Inject
    Supplier<Admin> clientSupplier;

    @Inject
    BrokerService brokerService;

    @GET
    @Path("{nodeId}/configs")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(responseCode = "200", value = ConfigEntry.ConfigEntryMap.class)
    public CompletionStage<Response> describeConfigs(@PathParam("nodeId") String nodeId) {
        return brokerService.describeConfigs(nodeId)
            .thenApply(Response::ok)
            .exceptionally(error -> Response.serverError().entity(error.getMessage()))
            .thenApply(Response.ResponseBuilder::build);
    }

}
