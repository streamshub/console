package com.github.eyefloaters.console.api;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.github.eyefloaters.console.api.model.ListFetchParams;
import com.github.eyefloaters.console.api.model.Topic;
import com.github.eyefloaters.console.api.service.TopicService;
import com.github.eyefloaters.console.api.support.ErrorCategory;
import com.github.eyefloaters.console.api.support.FieldFilter;
import com.github.eyefloaters.console.api.support.KafkaOffsetSpec;
import com.github.eyefloaters.console.api.support.KafkaUuid;
import com.github.eyefloaters.console.api.support.ListRequestContext;
import com.github.eyefloaters.console.api.support.StringEnumeration;

@Path("/api/kafkas/{clusterId}/topics")
@Tag(name = "Kafka Cluster Resources")
public class TopicsResource {

    static final String FIELDS_PARAM = "fields[topics]";

    @Inject
    UriInfo uriInfo;

    @Inject
    TopicService topicService;

    /**
     * Allows the value of {@link FieldFilter#requestedFields} to be set for
     * the request.
     */
    @Inject
    @Named("requestedFields")
    Consumer<List<String>> requestedFields;

//    @POST
//    @Consumes(MediaType.APPLICATION_JSON)
//    @Produces(MediaType.APPLICATION_JSON)
//    @APIResponseSchema(responseCode = "204", value = NewTopic.class)
//    public CompletionStage<Response> createTopic(NewTopic topic) {
//        return topicService.createTopic(topic)
//                .thenApply(createdTopic -> Response.status(Status.CREATED).entity(createdTopic))
//                .thenApply(Response.ResponseBuilder::build);
//    }

//    @Path("{topicName}")
//    @DELETE
//    @Produces(MediaType.APPLICATION_JSON)
//    @APIResponseSchema(responseCode = "200", value = Map.class)
//    public CompletionStage<Response> deleteTopic(@PathParam("topicName") String topicName) {
//        return topicService.deleteTopics(topicName)
//                .thenApply(Response::ok)
//                .thenApply(Response.ResponseBuilder::build);
//    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(Topic.ListResponse.class)
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public CompletionStage<Response> listTopics(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @QueryParam(FIELDS_PARAM)
            @DefaultValue(Topic.Fields.LIST_DEFAULT)
            @StringEnumeration(
                    source = FIELDS_PARAM,
                    allowedValues = {
                        Topic.Fields.NAME,
                        Topic.Fields.INTERNAL,
                        Topic.Fields.PARTITIONS,
                        Topic.Fields.AUTHORIZED_OPERATIONS,
                        Topic.Fields.CONFIGS
                    },
                    category = ErrorCategory.INVALID_QUERY_PARAMETER)
            @Parameter(
                    description = FieldFilter.FIELDS_DESCR,
                    explode = Explode.FALSE,
                    allowEmptyValue = true,
                    schema = @Schema(
                            type = SchemaType.ARRAY,
                            implementation = String.class,
                            enumeration = {
                                Topic.Fields.NAME,
                                Topic.Fields.INTERNAL,
                                Topic.Fields.PARTITIONS,
                                Topic.Fields.AUTHORIZED_OPERATIONS,
                                Topic.Fields.CONFIGS
                            }))
            List<String> fields,

            @QueryParam("offsetSpec")
            @DefaultValue("latest")
            @KafkaOffsetSpec(category = ErrorCategory.INVALID_QUERY_PARAMETER)
            @Parameter(
                    schema = @Schema(ref = "OffsetSpec"),
                    examples = {
                        @ExampleObject(ref = "EarliestOffset"),
                        @ExampleObject(ref = "LatestOffset"),
                        @ExampleObject(ref = "MaxTimestamp"),
                        @ExampleObject(ref = "LiteralTimestamp")
                    })
            String offsetSpec,

            @BeanParam
            ListFetchParams listParams) {

        requestedFields.accept(fields);
        ListRequestContext listSupport = new ListRequestContext(uriInfo.getRequestUri(), listParams);

        return topicService.listTopics(fields, offsetSpec, listSupport)
                .thenApply(Topic.ListResponse::new)
                .thenApply(Response::ok)
                .thenApply(Response.ResponseBuilder::build);
    }

    @Path("{topicId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(Topic.SingleResponse.class)
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public CompletionStage<Response> describeTopic(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @PathParam("topicId")
            @KafkaUuid(category = ErrorCategory.RESOURCE_NOT_FOUND, message = "No such topic")
            @Parameter(description = "Topic identifier")
            String topicId,

            @QueryParam(FIELDS_PARAM)
            @DefaultValue(Topic.Fields.DESCRIBE_DEFAULT)
            @StringEnumeration(
                    source = FIELDS_PARAM,
                    allowedValues = {
                        Topic.Fields.NAME,
                        Topic.Fields.INTERNAL,
                        Topic.Fields.PARTITIONS,
                        Topic.Fields.AUTHORIZED_OPERATIONS,
                        Topic.Fields.CONFIGS
                    },
                    category = ErrorCategory.INVALID_QUERY_PARAMETER)
            @Parameter(
                    description = FieldFilter.FIELDS_DESCR,
                    explode = Explode.FALSE,
                    allowEmptyValue = true,
                    schema = @Schema(
                            type = SchemaType.ARRAY,
                            implementation = String.class,
                            enumeration = {
                                Topic.Fields.NAME,
                                Topic.Fields.INTERNAL,
                                Topic.Fields.PARTITIONS,
                                Topic.Fields.AUTHORIZED_OPERATIONS,
                                Topic.Fields.CONFIGS
                            }))
            List<String> fields,

            @QueryParam("offsetSpec")
            @DefaultValue(KafkaOffsetSpec.LATEST)
            @KafkaOffsetSpec(category = ErrorCategory.INVALID_QUERY_PARAMETER)
            @Parameter(
                    schema = @Schema(ref = "OffsetSpec"),
                    examples = {
                        @ExampleObject(ref = "EarliestOffset"),
                        @ExampleObject(ref = "LatestOffset"),
                        @ExampleObject(ref = "MaxTimestamp"),
                        @ExampleObject(ref = "LiteralTimestamp")
                    })
            String offsetSpec) {

        requestedFields.accept(fields);

        return topicService.describeTopic(topicId, fields, offsetSpec)
                .thenApply(Topic.SingleResponse::new)
                .thenApply(Response::ok)
                .thenApply(Response.ResponseBuilder::build);
    }

//    @Path("{topicName}/configs")
//    @PATCH
//    @Consumes(MediaType.APPLICATION_JSON)
//    @RequestBodySchema(ConfigEntry.ConfigEntryMap.class)
//    @Produces(MediaType.APPLICATION_JSON)
//    @APIResponseSchema(responseCode = "200", value = ConfigEntry.ConfigEntryMap.class)
//    public CompletionStage<Response> alterTopicConfigs(@PathParam("topicName") String topicName, Map<String, ConfigEntry> configs) {
//        return topicService.alterConfigs(topicName, configs)
//                .thenApply(Response::ok)
//                .thenApply(Response.ResponseBuilder::build);
//    }

//    @Path("{topicName}/partitions")
//    @PATCH
//    @Consumes(MediaType.APPLICATION_JSON)
//    @RequestBodySchema(NewPartitions.class)
//    @Produces(MediaType.APPLICATION_JSON)
//    @APIResponse(responseCode = "204", description = "Partitions successfully created")
//    public CompletionStage<Response> createPartitions(@PathParam("topicName") String topicName, NewPartitions partitions) {
//        return topicService.createPartitions(topicName, partitions)
//                .thenApply(nothing -> Response.noContent())
//                .thenApply(Response.ResponseBuilder::build);
//    }
}
