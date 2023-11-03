package com.github.eyefloaters.console.api;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.ConstraintTarget;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.github.eyefloaters.console.api.model.ConsumerGroup;
import com.github.eyefloaters.console.api.model.ListFetchParams;
import com.github.eyefloaters.console.api.service.ConsumerGroupService;
import com.github.eyefloaters.console.api.support.ErrorCategory;
import com.github.eyefloaters.console.api.support.FieldFilter;
import com.github.eyefloaters.console.api.support.ListRequestContext;
import com.github.eyefloaters.console.api.support.StringEnumeration;

import io.xlate.validation.constraints.Expression;

@Path("/api/kafkas/{clusterId}/consumerGroups")
@Tag(name = "Kafka Cluster Resources")
public class ConsumerGroupsResource {

    @Inject
    UriInfo uriInfo;

    @Inject
    ConsumerGroupService consumerGroupService;

    /**
     * Allows the value of {@link FieldFilter#requestedFields} to be set for
     * the request.
     */
    @Inject
    @Named("requestedFields")
    Consumer<List<String>> requestedFields;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(ConsumerGroup.ListResponse.class)
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public CompletionStage<Response> listConsumerGroups(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @QueryParam(ConsumerGroup.FIELDS_PARAM)
            @DefaultValue(ConsumerGroup.Fields.LIST_DEFAULT)
            @StringEnumeration(
                    source = ConsumerGroup.FIELDS_PARAM,
                    allowedValues = {
                        ConsumerGroup.Fields.STATE,
                        ConsumerGroup.Fields.SIMPLE_CONSUMER_GROUP,
                        ConsumerGroup.Fields.MEMBERS,
                        ConsumerGroup.Fields.OFFSETS,
                        ConsumerGroup.Fields.AUTHORIZED_OPERATIONS,
                        ConsumerGroup.Fields.COORDINATOR,
                        ConsumerGroup.Fields.PARTITION_ASSIGNOR
                    },
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(
                    description = FieldFilter.FIELDS_DESCR,
                    explode = Explode.FALSE,
                    schema = @Schema(
                            type = SchemaType.ARRAY,
                            implementation = String.class,
                            enumeration = {
                                ConsumerGroup.Fields.STATE,
                                ConsumerGroup.Fields.SIMPLE_CONSUMER_GROUP,
                                ConsumerGroup.Fields.MEMBERS,
                                ConsumerGroup.Fields.OFFSETS,
                                ConsumerGroup.Fields.AUTHORIZED_OPERATIONS,
                                ConsumerGroup.Fields.COORDINATOR,
                                ConsumerGroup.Fields.PARTITION_ASSIGNOR
                            }))
            List<String> fields,

            @BeanParam
            @Valid
            ListFetchParams listParams) {

        requestedFields.accept(fields);
        ListRequestContext<ConsumerGroup> listSupport = new ListRequestContext<>(ConsumerGroup.Fields.COMPARATOR_BUILDER, uriInfo.getRequestUri(), listParams, ConsumerGroup::fromCursor);

        return consumerGroupService.listConsumerGroups(fields, listSupport)
                .thenApply(groups -> new ConsumerGroup.ListResponse(groups, listSupport))
                .thenApply(Response::ok)
                .thenApply(Response.ResponseBuilder::build);
    }

    @Path("{groupId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(ConsumerGroup.ConsumerGroupDocument.class)
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public CompletionStage<Response> describeConsumerGroup(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @PathParam("groupId")
            @Parameter(description = "Consumer group identifier")
            String groupId,

            @QueryParam(ConsumerGroup.FIELDS_PARAM)
            @DefaultValue(ConsumerGroup.Fields.DESCRIBE_DEFAULT)
            @StringEnumeration(
                    source = ConsumerGroup.FIELDS_PARAM,
                    allowedValues = {
                        ConsumerGroup.Fields.STATE,
                        ConsumerGroup.Fields.SIMPLE_CONSUMER_GROUP,
                        ConsumerGroup.Fields.MEMBERS,
                        ConsumerGroup.Fields.OFFSETS,
                        ConsumerGroup.Fields.AUTHORIZED_OPERATIONS,
                        ConsumerGroup.Fields.COORDINATOR,
                        ConsumerGroup.Fields.PARTITION_ASSIGNOR
                    },
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(
                    description = FieldFilter.FIELDS_DESCR,
                    explode = Explode.FALSE,
                    schema = @Schema(
                            type = SchemaType.ARRAY,
                            implementation = String.class,
                            enumeration = {
                                ConsumerGroup.Fields.STATE,
                                ConsumerGroup.Fields.SIMPLE_CONSUMER_GROUP,
                                ConsumerGroup.Fields.MEMBERS,
                                ConsumerGroup.Fields.OFFSETS,
                                ConsumerGroup.Fields.AUTHORIZED_OPERATIONS,
                                ConsumerGroup.Fields.COORDINATOR,
                                ConsumerGroup.Fields.PARTITION_ASSIGNOR
                            }))
            List<String> fields) {

        requestedFields.accept(fields);

        return consumerGroupService.describeConsumerGroup(groupId, fields)
                .thenApply(ConsumerGroup.ConsumerGroupDocument::new)
                .thenApply(Response::ok)
                .thenApply(Response.ResponseBuilder::build);
    }

    @Path("{groupId}")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(responseCode = "204", value = Void.class)
    @Expression(
        targetName = "args",
        // Only check when the request body Id is present (separately checked for @NotNull)
        when = "args[2].data.id != null",
        // Verify the Id in the request body matches the Id in the URL
        value = "args[1].equals(args[2].data.id)",
        message = "resource ID conflicts with operation URL",
        node = { "data", "id" },
        payload = ErrorCategory.InvalidResource.class,
        validationAppliesTo = ConstraintTarget.PARAMETERS)
    public CompletionStage<Response> patchConsumerGroup(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @PathParam("groupId")
            @Parameter(description = "Consumer group identifier")
            String groupId,

            @Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = ConsumerGroup.ConsumerGroupDocument.class),
                    examples = {
                        @ExampleObject(
                            name = "patchConsumerGroup-allPartitions",
                            externalValue = "/openapi/examples/patchConsumerGroup-allPartitions.json"),
                        @ExampleObject(
                            name = "patchConsumerGroup-byPartition",
                            externalValue = "/openapi/examples/patchConsumerGroup-byPartition.json"),
                    })
            )
            ConsumerGroup.ConsumerGroupDocument patch) {

        return consumerGroupService.patchConsumerGroup(patch.getData().getAttributes())
                .thenApply(nothing -> Response.noContent())
                .thenApply(Response.ResponseBuilder::build);
    }

    @Path("{groupId}")
    @DELETE
    @APIResponseSchema(responseCode = "204", value = Void.class)
    public CompletionStage<Response> deleteConsumerGroup(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @PathParam("groupId")
            @Parameter(description = "Consumer group identifier")
            String groupId) {
        return consumerGroupService.deleteConsumerGroup(groupId)
                .thenApply(nothing -> Response.noContent())
                .thenApply(Response.ResponseBuilder::build);
    }
}
