package io.strimzi.kafka.instance;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.parameters.RequestBodySchema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;

import io.strimzi.kafka.instance.model.ConfigEntry;
import io.strimzi.kafka.instance.model.NewPartitions;
import io.strimzi.kafka.instance.model.NewTopic;
import io.strimzi.kafka.instance.model.Topic;
import io.strimzi.kafka.instance.service.TopicService;

@Path("/api/v2/topics")
public class TopicsResource {

    @Inject
    TopicService topicService;

    @POST
    public CompletionStage<Response> createTopic(NewTopic topic) {
        return topicService.createTopic(topic)
                .thenApply(createdTopic -> Response.status(Status.CREATED).entity(createdTopic))
                .exceptionally(error -> Response.serverError().entity(error.getMessage()))
                .thenApply(Response.ResponseBuilder::build);
    }

    @Path("{topicName}")
    @DELETE
    public CompletionStage<Response> deleteTopic(@PathParam("topicName") String topicName) {
        return topicService.deleteTopics(topicName)
                .thenApply(Response::ok)
                .exceptionally(error -> Response.serverError().entity(error.getMessage()))
                .thenApply(Response.ResponseBuilder::build);
    }

    @GET
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
    @APIResponseSchema(responseCode = "200", value = ConfigEntry.ConfigEntryMap.class)
    public CompletionStage<Response> describeTopicConfigs(@PathParam("topicName") String topicName) {
        return topicService.describeConfigs(topicName)
                .thenApply(Response::ok)
                .exceptionally(error -> Response.serverError().entity(error.getMessage()))
                .thenApply(Response.ResponseBuilder::build);
    }

    @Path("{topicName}/configs")
    @PATCH
    @RequestBodySchema(ConfigEntry.ConfigEntryMap.class)
    @APIResponseSchema(responseCode = "200", value = ConfigEntry.ConfigEntryMap.class)
    public CompletionStage<Response> alterTopicConfigs(@PathParam("topicName") String topicName, Map<String, ConfigEntry> configs) {
        return topicService.alterConfigs(topicName, configs)
                .thenApply(Response::ok)
                .exceptionally(error -> Response.serverError().entity(error.getMessage()))
                .thenApply(Response.ResponseBuilder::build);
    }

    @Path("{topicName}/partitions")
    @PATCH
    @RequestBodySchema(NewPartitions.class)
    @APIResponse(responseCode = "204", description = "Partitions successfully created")
    public CompletionStage<Response> createPartitions(@PathParam("topicName") String topicName, NewPartitions partitions) {
        return topicService.createPartitions(topicName, partitions)
                .thenApply(nothing -> Response.noContent())
                .exceptionally(error -> Response.serverError().entity(error.getMessage()))
                .thenApply(Response.ResponseBuilder::build);
    }
}
