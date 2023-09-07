package com.github.eyefloaters.console.legacy.handlers;

import io.quarkus.runtime.annotations.RegisterForReflection;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.github.eyefloaters.console.legacy.Operations;
import com.github.eyefloaters.console.legacy.model.Types;

import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Optional;
import java.util.concurrent.CompletionStage;

@RegisterForReflection
public interface OperationsHandler {

    @POST
    @Path("topics")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // OpenAPI
    @Tag(name = "topics")
    @Operation(
        operationId = Operations.CREATE_TOPIC,
        summary = "Creates a new topic",
        description = "Creates a new topic for Kafka.")
    @RequestBody(
        description = "Topic to create.",
        required = true,
        content = @Content(examples = @ExampleObject(ref = "NewTopicExample")))
    @APIResponseSchema(
        responseCode = "201",
        value = Types.Topic.class,
        responseDescription = "Topic created successfully.")
    @APIResponse(responseCode = "400", ref = "BadRequest")
    @APIResponse(responseCode = "401", ref = "NotAuthorized")
    @APIResponse(responseCode = "403", ref = "Forbidden")
    @APIResponse(responseCode = "409", ref = "Conflict")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "503", ref = "ServiceUnavailable")
    CompletionStage<Response> createTopic(@Valid Types.NewTopic newTopic);

    @GET
    @Path("topics/{topicName}")
    @Produces(MediaType.APPLICATION_JSON)
    // OpenAPI
    @Tag(name = "topics")
    @Operation(
        operationId = Operations.GET_TOPIC,
        summary = "Retrieves a single topic",
        description = "Topic")
    @Parameter(
        name = "topicName",
        description = "Name of the topic to describe")
    @APIResponseSchema(
        value = Types.Topic.class,
        responseDescription = "Kafka topic details")
    @APIResponse(responseCode = "401", ref = "NotAuthorized")
    @APIResponse(responseCode = "403", ref = "Forbidden")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "503", ref = "ServiceUnavailable")
    CompletionStage<Response> describeTopic(@PathParam("topicName") String topicName);

    @PATCH
    @Path("topics/{topicName}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // OpenAPI
    @Tag(name = "topics")
    @Operation(
        operationId = Operations.UPDATE_TOPIC,
        summary = "Updates a single topic",
        description = "Update the configuration settings for a topic.")
    @Parameter(
        name = "topicName",
        description = "Name of the topic to update")
    @RequestBody(required = true)
    @APIResponseSchema(
        responseCode = "200",
        value = Types.Topic.class,
        responseDescription = "Topic updated successfully.")
    @APIResponse(responseCode = "400", ref = "BadRequest")
    @APIResponse(responseCode = "401", ref = "NotAuthorized")
    @APIResponse(responseCode = "403", ref = "Forbidden")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "503", ref = "ServiceUnavailable")
    CompletionStage<Response> updateTopic(@PathParam("topicName") String topicName,
                                          @Valid Types.TopicSettings updatedTopic);

    @DELETE
    @Path("topics/{topicName}")
    @Produces(MediaType.APPLICATION_JSON)
    // OpenAPI
    @Tag(name = "topics")
    @Operation(
        operationId = Operations.DELETE_TOPIC,
        summary = "Deletes a topic",
        description = "Deletes the topic with the specified name.")
    @Parameter(
        name = "topicName",
        description = "Name of the topic to delete")
    @APIResponse(responseCode = "204", description = "Topic deleted successfully.")
    @APIResponse(responseCode = "401", ref = "NotAuthorized")
    @APIResponse(responseCode = "403", ref = "Forbidden")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "503", ref = "ServiceUnavailable")
    CompletionStage<Response> deleteTopic(@PathParam("topicName") String topicName);

    @GET
    @Path("topics")
    @Produces(MediaType.APPLICATION_JSON)
    // OpenAPI
    @Tag(name = "topics")
    @Operation(
        operationId = Operations.GET_TOPICS_LIST,
        summary = "Retrieves a list of topics",
        description = "Returns a list of all of the available topics, or the list of topics that meet the request query parameters. The topics returned are limited to those records the requestor is authorized to view.")
    @Parameter(
        name = "filter",
        description = "Filter to apply when returning the list of topics")
    @APIResponseSchema(
        value = Types.TopicList.class,
        responseDescription = "List of topics matching the request query parameters. The topics returned are limited to those records the requestor is authorized to view.")
    @APIResponse(responseCode = "400", ref = "BadRequest")
    @APIResponse(responseCode = "401", ref = "NotAuthorized")
    @APIResponse(responseCode = "403", ref = "Forbidden")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "503", ref = "ServiceUnavailable")
    CompletionStage<Response> listTopics(@QueryParam("filter") String filter,
                                         @Valid @BeanParam Types.DeprecatedPageRequest pageParams,
                                         @Valid @BeanParam Types.TopicSortParams orderParams);

    @GET
    @Path("topics/{topicName}/records")
    @Produces(MediaType.APPLICATION_JSON)
    // OpenAPI
    @Tag(name = "records")
    @Operation(
        operationId = Operations.CONSUME_RECORDS,
        summary = "Consume records from a topic",
        description = "Consume a limited number of records from a topic, optionally specifying a partition and an absolute offset or timestamp as the starting point for message retrieval.")
    @Parameter(
        name = "topicName",
        description = "Topic name")
    @APIResponseSchema(
        value = Types.RecordList.class,
        responseDescription = "List of records matching the request query parameters.")
    @APIResponse(responseCode = "400", ref = "BadRequest")
    @APIResponse(responseCode = "401", ref = "NotAuthorized")
    @APIResponse(responseCode = "403", ref = "Forbidden")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "503", ref = "ServiceUnavailable")
    Response consumeRecords(@PathParam("topicName") String topicName,
                            @Valid @BeanParam Types.RecordFilterParams filterParams);

    @POST
    @Path("topics/{topicName}/records")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // OpenAPI
    @Tag(name = "records", description = "Send and receive records interactively")
    @Operation(
        operationId = Operations.PRODUCE_RECORD,
        summary = "Send a record to a topic",
        description = "Produce (write) a single record to a topic.")
    @Parameter(
        name = "topicName",
        description = "Topic name")
    @RequestBody(
        required = true,
        content = @Content(
            schema = @Schema(implementation = Types.Record.class),
            examples = @ExampleObject(ref = "RecordProduceExample")))
    @APIResponseSchema(
        responseCode = "201",
        value = Types.Record.class,
        responseDescription = "Record was successfully sent to the topic")
    @APIResponse(responseCode = "400", ref = "BadRequest")
    @APIResponse(responseCode = "401", ref = "NotAuthorized")
    @APIResponse(responseCode = "403", ref = "Forbidden")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "503", ref = "ServiceUnavailable")
    CompletionStage<Response> produceRecord(@PathParam("topicName") String topicName, @Valid Types.Record input);

    @GET
    @Path("consumer-groups")
    @Produces(MediaType.APPLICATION_JSON)
    // OpenAPI
    @Tag(name = "groups")
    @Operation(
        operationId = Operations.GET_CONSUMER_GROUPS_LIST,
        summary = "List of consumer groups in the Kafka instance.",
        description = "Returns a list of all consumer groups for a particular Kafka instance. The consumer groups returned are limited to those records the requestor is authorized to view.")
    @Parameter(
        name = "group-id-filter",
        description = "Return the consumer groups where the ID contains this value")
    @Parameter(
        name = "topic",
        description = "Return consumer groups where the topic name contains this value")
    @APIResponseSchema(
        responseCode = "200",
        value = Types.ConsumerGroupList.class,
        responseDescription = "List of consumer groups matching the request parameters. The consumer groups returned are limited to those records the requestor is authorized to view.")
    @APIResponse(responseCode = "400", ref = "BadRequest")
    @APIResponse(responseCode = "401", ref = "NotAuthorized")
    @APIResponse(responseCode = "403", ref = "Forbidden")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "503", ref = "ServiceUnavailable")
    CompletionStage<Response> listGroups(@QueryParam("group-id-filter") String groupFilter,
                                         @QueryParam("topic") String topicFilter,
                                         @Valid @BeanParam Types.DeprecatedPageRequest pageParams,
                                         @Valid @BeanParam Types.ConsumerGroupSortParams sortParams);

    @GET
    @Path("consumer-groups/{consumerGroupId}")
    @Produces(MediaType.APPLICATION_JSON)
    // OpenAPI
    @Tag(name = "groups")
    @Operation(operationId = Operations.GET_CONSUMER_GROUP, summary = "Get a single consumer group by its unique ID.")
    @Parameter(
        name = "consumerGroupId",
        description = "Consumer group identifier")
    @Parameter(
        name = "partitionFilter",
        description = "Value of partition to include. Value -1 means filter is not active.")
    @Parameter(
        name = "topic",
        description = "Filter consumer groups for a specific topic")
    @APIResponse(
        responseCode = "200",
        description = "Consumer group details.",
        content = @Content(
            schema = @Schema(implementation = Types.ConsumerGroup.class),
            examples = @ExampleObject(ref = "ConsumerGroupExample")))
    @APIResponse(responseCode = "401", ref = "NotAuthorized")
    @APIResponse(responseCode = "403", ref = "Forbidden")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "503", ref = "ServiceUnavailable")
    CompletionStage<Response> describeGroup(@PathParam("consumerGroupId") String consumerGroupId,
                                            @QueryParam("partitionFilter") Optional<Integer> partitionFilter,
                                            @QueryParam("topic") String topicFilter,
                                            @BeanParam Types.ConsumerGroupDescriptionSortParams sortParams);

    @DELETE
    @Path("consumer-groups/{consumerGroupId}")
    // OpenAPI
    @Tag(name = "groups")
    @Operation(
        operationId = Operations.DELETE_CONSUMER_GROUP,
        summary = "Delete a consumer group.",
        description = "Delete a consumer group, along with its consumers.")
    @Parameter(
        name = "consumerGroupId",
        description = "Consumer group identifier")
    @APIResponse(
        responseCode = "204",
        description = "The consumer group was deleted successfully.")
    @APIResponse(responseCode = "401", ref = "NotAuthorized")
    @APIResponse(responseCode = "403", ref = "Forbidden")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(
        responseCode = "423",
        description = "User cannot delete consumer group with active members.",
        content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Types.Error.class)))
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "503", ref = "ServiceUnavailable")
    CompletionStage<Response> deleteGroup(@PathParam("consumerGroupId") String consumerGroupId);

    @POST
    @Path("consumer-groups/{consumerGroupId}/reset-offset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    // OpenAPI
    @Tag(name = "groups")
    @Operation(
        operationId = Operations.RESET_CONSUMER_GROUP_OFFSET,
        summary = "Reset the offset for a consumer group.",
        description = "Reset the offset for a particular consumer group.")
    @Parameter(
        name = "consumerGroupId",
        description = "Consumer group identifier")
    @RequestBody(
        required = true,
        content = @Content(examples = @ExampleObject(ref = "ConsumerGroupOffsetResetExample")))
    @APIResponseSchema(
        responseCode = "200",
        value = Types.ConsumerGroupResetOffsetResult.class,
        responseDescription = "The consumer group offsets have been reset.")
    @APIResponse(responseCode = "400", ref = "BadRequest")
    @APIResponse(responseCode = "401", ref = "NotAuthorized")
    @APIResponse(responseCode = "403", ref = "Forbidden")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "503", ref = "ServiceUnavailable")
    CompletionStage<Response> resetGroupOffset(@PathParam("consumerGroupId") String consumerGroupId,
                                               @Valid Types.ConsumerGroupOffsetResetParameters parameters);

    @GET
    @Path("acls/resource-operations")
    @Produces(MediaType.APPLICATION_JSON)
    // OpenAPI
    @Tag(name = "acls")
    @Operation(
        operationId = Operations.GET_ACL_RESOURCE_OPERATIONS,
        summary = "Retrieve allowed ACL resources and operations",
        description = "Retrieve the resources and associated operations that may have ACLs configured.")
    @APIResponse(responseCode = "200", description = "Map of allowed resources and operations for ACL creation")
    @APIResponse(responseCode = "401", ref = "NotAuthorized")
    @APIResponse(responseCode = "403", ref = "Forbidden")
    @APIResponse(responseCode = "500", ref = "ServerError")
    Response getAclResourceOperations();

    @GET
    @Path("acls")
    @Produces(MediaType.APPLICATION_JSON)
    // OpenAPI
    @Tag(name = "acls")
    @Operation(
        operationId = Operations.GET_ACLS,
        summary = "List ACL bindings",
        description = "Returns a list of all of the available ACL bindings, or the list of bindings that meet the user's URL query parameters. If no parameters are specified, all ACL bindings known to the system will be returned (with paging).")
    @APIResponseSchema(
        responseCode = "200",
        value = Types.AclBindingList.class,
        responseDescription = "List of ACL bindings matching the query parameters.")
    @APIResponse(responseCode = "400", ref = "BadRequest")
    @APIResponse(responseCode = "401", ref = "NotAuthorized")
    @APIResponse(responseCode = "403", ref = "Forbidden")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "503", ref = "ServiceUnavailable")
    CompletionStage<Response> describeAcls(@BeanParam Types.AclBindingFilterParams filterParams,
                                           @Valid @BeanParam Types.PageRequest pageParams,
                                           @Valid @BeanParam Types.AclBindingSortParams sortParams);

    @POST
    @Path("acls")
    @Consumes(MediaType.APPLICATION_JSON)
    // OpenAPI
    @Tag(name = "acls")
    @Operation(
        operationId = Operations.CREATE_ACL,
        summary = "Create ACL binding",
        description = "Creates a new ACL binding for a Kafka instance.")
    @RequestBody(description = "ACL to create.", required = true)
    @APIResponse(responseCode = "201", description = "ACL created successfully.")
    @APIResponse(responseCode = "400", ref = "BadRequest")
    @APIResponse(responseCode = "401", ref = "NotAuthorized")
    @APIResponse(responseCode = "403", ref = "Forbidden")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "503", ref = "ServiceUnavailable")
    CompletionStage<Response> createAcl(@Valid Types.AclBinding binding);

    @DELETE
    @Path("acls")
    @Produces(MediaType.APPLICATION_JSON)
    // OpenAPI
    @Tag(name = "acls")
    @Operation(
        operationId = Operations.DELETE_ACLS,
        summary = "Delete ACL bindings",
        description = "Deletes ACL bindings that match the query parameters.")
    @APIResponseSchema(
        responseCode = "200",
        value = Types.AclBindingList.class,
        responseDescription = "List of all ACL bindings matching the query parameters that were deleted.")
    @APIResponse(responseCode = "400", ref = "BadRequest")
    @APIResponse(responseCode = "401", ref = "NotAuthorized")
    @APIResponse(responseCode = "403", ref = "Forbidden")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "503", ref = "ServiceUnavailable")
    CompletionStage<Response> deleteAcls(@BeanParam Types.AclBindingFilterParams filterParams);

    @GET
    @Path("errors")
    @Produces(MediaType.APPLICATION_JSON)
    // OpenAPI
    @Tag(name = "errors")
    @Operation(operationId = Operations.GET_ERRORS, summary = "Get list of errors")
    @APIResponse(
        responseCode = "200",
        description = "Error listing",
        content = @Content(
            schema = @Schema(implementation = Types.ErrorList.class)))
    @APIResponse(responseCode = "500", ref = "ServerError")
    Response getErrors();

    @GET
    @Path("errors/{errorId}")
    @Produces(MediaType.APPLICATION_JSON)
    // OpenAPI
    @Tag(name = "errors")
    @Operation(operationId = Operations.GET_ERROR, summary = "Get an error by its unique ID")
    @Parameter(
        name = "errorId",
        description = "Error identifier")
    @APIResponse(
        responseCode = "200",
        description = "Error details",
        content = @Content(
            schema = @Schema(implementation = Types.Error.class)))
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    Response getError(@PathParam("errorId") String errorId);
}
