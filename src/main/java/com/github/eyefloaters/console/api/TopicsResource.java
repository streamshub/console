package com.github.eyefloaters.console.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBodySchema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;

import com.github.eyefloaters.console.api.model.ConfigEntry;
import com.github.eyefloaters.console.api.model.NewPartitions;
import com.github.eyefloaters.console.api.model.NewTopic;
import com.github.eyefloaters.console.api.model.Topic;
import com.github.eyefloaters.console.api.service.TopicService;

@Path("/api/clusters/{clusterId}/topics")
public class TopicsResource {

    @Inject
    TopicService topicService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(responseCode = "204", value = NewTopic.class)
    public CompletionStage<Response> createTopic(NewTopic topic) {
        return topicService.createTopic(topic)
                .thenApply(createdTopic -> Response.status(Status.CREATED).entity(createdTopic))
                .exceptionally(error -> Response.serverError().entity(error.getMessage()))
                .thenApply(Response.ResponseBuilder::build);
    }

    @Path("{topicName}")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(responseCode = "200", value = Map.class)
    public CompletionStage<Response> deleteTopic(@PathParam("topicName") String topicName) {
        return topicService.deleteTopics(topicName)
                .thenApply(Response::ok)
                .exceptionally(error -> Response.serverError().entity(error.getMessage()))
                .thenApply(Response.ResponseBuilder::build);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(responseCode = "200", value = Topic[].class)
    public CompletionStage<Response> listTopics(
                    @QueryParam("include") @DefaultValue("") List<String> includes,
                    @QueryParam("listInternal") @DefaultValue("false") boolean listInternal) {

        return topicService.listTopics(listInternal, includes)
                .thenApply(Response::ok)
                .exceptionally(error -> Response.serverError().entity(error.getMessage()))
                .thenApply(Response.ResponseBuilder::build);
    }

    @Path("{topicName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(responseCode = "200", value = Topic[].class)
    public CompletionStage<Response> describeTopic(
                    @PathParam("topicName") String topicName,
                    @QueryParam("include") @DefaultValue("") List<String> includes) {

        return topicService.describeTopic(topicName, includes)
                .thenApply(Response::ok)
                .exceptionally(error -> Response.serverError().entity(error.getMessage()))
                .thenApply(Response.ResponseBuilder::build);
    }

    @Path("{topicName}/configs")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(responseCode = "200", value = ConfigEntry.ConfigEntryMap.class)
    public CompletionStage<Response> describeTopicConfigs(@PathParam("topicName") String topicName) {
        return topicService.describeConfigs(topicName)
                .thenApply(Response::ok)
                .exceptionally(error -> Response.serverError().entity(error.getMessage()))
                .thenApply(Response.ResponseBuilder::build);
    }

    @Path("{topicName}/configs")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @RequestBodySchema(ConfigEntry.ConfigEntryMap.class)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(responseCode = "200", value = ConfigEntry.ConfigEntryMap.class)
    public CompletionStage<Response> alterTopicConfigs(@PathParam("topicName") String topicName, Map<String, ConfigEntry> configs) {
        return topicService.alterConfigs(topicName, configs)
                .thenApply(Response::ok)
                .exceptionally(error -> Response.serverError().entity(error.getMessage()))
                .thenApply(Response.ResponseBuilder::build);
    }

    @Path("{topicName}/partitions")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @RequestBodySchema(NewPartitions.class)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "204", description = "Partitions successfully created")
    public CompletionStage<Response> createPartitions(@PathParam("topicName") String topicName, NewPartitions partitions) {
        return topicService.createPartitions(topicName, partitions)
                .thenApply(nothing -> Response.noContent())
                .exceptionally(error -> Response.serverError().entity(error.getMessage()))
                .thenApply(Response.ResponseBuilder::build);
    }
}
