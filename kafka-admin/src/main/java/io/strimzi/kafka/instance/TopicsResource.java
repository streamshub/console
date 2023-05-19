package io.strimzi.kafka.instance;

import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;

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
                .thenApply(nothing -> Response.noContent())
                .exceptionally(error -> Response.serverError().entity(error.getMessage()))
                .thenApply(Response.ResponseBuilder::build);
    }

    @GET
    @APIResponseSchema(responseCode = "200", value = Topic[].class)
    public CompletionStage<Response> listTopics(
                    @QueryParam("include") @DefaultValue("") String include,
                    @QueryParam("listInternal") @DefaultValue("false") boolean listInternal) {

        Set<String> includes = Arrays.stream(include.split(","))
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.toSet());

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
                    @QueryParam("include") @DefaultValue("") String include) {

        Set<String> includes = Arrays.stream(include.split(","))
                .filter(Predicate.not(String::isBlank))
                .collect(Collectors.toSet());

        return topicService.describeTopic(topicName, includes)
                .thenApply(Response::ok)
                .exceptionally(error -> Response.serverError().entity(error.getMessage()))
                .thenApply(Response.ResponseBuilder::build);
    }

    @Path("{topicName}/configs")
    @GET
    @APIResponse(responseCode = "200", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(
                type = SchemaType.OBJECT
            )))
    public CompletionStage<Response> describeTopicConfigs(@PathParam("topicName") String topicName) {
        return topicService.describeConfigs(topicName)
                .thenApply(Response::ok)
                .exceptionally(error -> Response.serverError().entity(error.getMessage()))
                .thenApply(Response.ResponseBuilder::build);
    }

}
