package com.github.streamshub.console.api;

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
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
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

import com.github.streamshub.console.api.model.Group;
import com.github.streamshub.console.api.model.ListFetchParams;
import com.github.streamshub.console.api.model.NewTopic;
import com.github.streamshub.console.api.model.Topic;
import com.github.streamshub.console.api.model.TopicFilterParams;
import com.github.streamshub.console.api.model.TopicMetrics;
import com.github.streamshub.console.api.model.TopicPatch;
import com.github.streamshub.console.api.security.Authorized;
import com.github.streamshub.console.api.security.ResourcePrivilege;
import com.github.streamshub.console.api.service.GroupService;
import com.github.streamshub.console.api.service.TopicService;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.FieldFilter;
import com.github.streamshub.console.api.support.KafkaOffsetSpec;
import com.github.streamshub.console.api.support.KafkaUuid;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.api.support.StringEnumeration;
import com.github.streamshub.console.config.security.Privilege;

import io.xlate.validation.constraints.Expression;

@Path("/api/kafkas/{clusterId}/topics")
@Tag(name = "Kafka Cluster Resources")
public class TopicsResource {

    @Inject
    UriInfo uriInfo;

    @Inject
    TopicService topicService;

    @Inject
    GroupService consumerGroupService;

    /**
     * Allows the value of {@link FieldFilter#requestedFields} to be set for
     * the request.
     */
    @Inject
    @Named("requestedFields")
    Consumer<List<String>> requestedFields;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200",
        description = "New topic successfully validated, nothing was created",
        content = @Content(schema = @Schema(implementation = NewTopic.NewTopicDocument.class)))
    @APIResponse(responseCode = "201",
        description = "New topic successfully created",
        content = @Content(schema = @Schema(implementation = NewTopic.NewTopicDocument.class)))
    // authorization checked by TopicService
    public CompletionStage<Response> createTopic(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = NewTopic.NewTopicDocument.class),
                    examples = {
                        @ExampleObject(
                                name = "createTopic-simple",
                                externalValue = "/openapi/examples/createTopic-simple.json"),
                        @ExampleObject(
                                name = "createTopic-configs",
                                externalValue = "/openapi/examples/createTopic-configs.json"),
                        @ExampleObject(
                            name = "createTopic-validateOnly",
                            externalValue = "/openapi/examples/createTopic-validateOnly.json")
                    })
            )
            NewTopic.NewTopicDocument topic) {

        final UriBuilder location = uriInfo.getRequestUriBuilder();
        final boolean validateOnly = Boolean.TRUE.equals(topic.meta("validateOnly"));

        return topicService.createTopic(topic.getData().getAttributes(), validateOnly)
            .thenApply(NewTopic.NewTopicDocument::new)
            .thenApply(entity -> Response.status(validateOnly ? Status.OK : Status.CREATED)
                    .entity(entity)
                    .location(location.path(entity.getData().getId()).build())
                    .build());
    }

    @Path("{topicId}")
    @DELETE
    @APIResponseSchema(responseCode = "204", value = Void.class)
    @Authorized
    @ResourcePrivilege(Privilege.DELETE)
    public CompletionStage<Response> deleteTopic(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @PathParam("topicId")
            @KafkaUuid(payload = ErrorCategory.ResourceNotFound.class, message = "No such topic")
            @Parameter(description = "Topic identifier")
            String topicId) {
        return topicService.deleteTopic(topicId)
                .thenApply(nothing -> Response.noContent())
                .thenApply(Response.ResponseBuilder::build);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(Topic.ListResponse.class)
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    @Authorized
    @ResourcePrivilege(Privilege.LIST)
    public CompletionStage<Response> listTopics(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @QueryParam(Topic.FIELDS_PARAM)
            @DefaultValue(Topic.Fields.LIST_DEFAULT)
            @StringEnumeration(
                    source = Topic.FIELDS_PARAM,
                    allowedValues = {
                        Topic.Fields.NAME,
                        Topic.Fields.VISIBILITY,
                        Topic.Fields.PARTITIONS,
                        Topic.Fields.NUM_PARTITIONS,
                        Topic.Fields.AUTHORIZED_OPERATIONS,
                        Topic.Fields.CONFIGS,
                        Topic.Fields.TOTAL_LEADER_LOG_BYTES,
                        Topic.Fields.GROUPS,
                        Topic.Fields.STATUS,
                    },
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(
                    description = FieldFilter.FIELDS_DESCR,
                    explode = Explode.FALSE,
                    schema = @Schema(
                            type = SchemaType.ARRAY,
                            implementation = String.class,
                            enumeration = {
                                Topic.Fields.NAME,
                                Topic.Fields.VISIBILITY,
                                Topic.Fields.PARTITIONS,
                                Topic.Fields.NUM_PARTITIONS,
                                Topic.Fields.AUTHORIZED_OPERATIONS,
                                Topic.Fields.CONFIGS,
                                Topic.Fields.TOTAL_LEADER_LOG_BYTES,
                                Topic.Fields.GROUPS,
                                Topic.Fields.STATUS,
                            }))
            List<String> fields,

            @QueryParam("offsetSpec")
            @DefaultValue("latest")
            @KafkaOffsetSpec(payload = ErrorCategory.InvalidQueryParameter.class)
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
            @Valid
            ListFetchParams listParams,

            @BeanParam
            @Valid
            TopicFilterParams filters) {

        requestedFields.accept(fields);

        ListRequestContext<Topic> listSupport = new ListRequestContext<>(
                filters,
                Topic.Fields.COMPARATOR_BUILDER,
                uriInfo.getRequestUri(),
                listParams,
                Topic::fromCursor);

        return topicService.listTopics(fields, offsetSpec, listSupport)
                .thenApply(topics -> new Topic.ListResponse(topics, listSupport))
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
    @Authorized
    @ResourcePrivilege(Privilege.GET)
    public CompletionStage<Response> describeTopic(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @PathParam("topicId")
            @KafkaUuid(payload = ErrorCategory.ResourceNotFound.class, message = "No such topic")
            @Parameter(description = "Topic identifier")
            String topicId,

            @QueryParam(Topic.FIELDS_PARAM)
            @DefaultValue(Topic.Fields.DESCRIBE_DEFAULT)
            @StringEnumeration(
                    source = Topic.FIELDS_PARAM,
                    allowedValues = {
                        Topic.Fields.NAME,
                        Topic.Fields.VISIBILITY,
                        Topic.Fields.PARTITIONS,
                        Topic.Fields.NUM_PARTITIONS,
                        Topic.Fields.AUTHORIZED_OPERATIONS,
                        Topic.Fields.CONFIGS,
                        Topic.Fields.TOTAL_LEADER_LOG_BYTES,
                        Topic.Fields.GROUPS,
                        Topic.Fields.STATUS,
                    },
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(
                    description = FieldFilter.FIELDS_DESCR,
                    explode = Explode.FALSE,
                    schema = @Schema(
                            type = SchemaType.ARRAY,
                            implementation = String.class,
                            enumeration = {
                                Topic.Fields.NAME,
                                Topic.Fields.VISIBILITY,
                                Topic.Fields.PARTITIONS,
                                Topic.Fields.NUM_PARTITIONS,
                                Topic.Fields.AUTHORIZED_OPERATIONS,
                                Topic.Fields.CONFIGS,
                                Topic.Fields.TOTAL_LEADER_LOG_BYTES,
                                Topic.Fields.GROUPS,
                                Topic.Fields.STATUS,
                            }))
            List<String> fields,

            @QueryParam("offsetSpec")
            @DefaultValue(KafkaOffsetSpec.LATEST)
            @KafkaOffsetSpec(payload = ErrorCategory.InvalidQueryParameter.class)
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

    @Path("{topicId}/groups")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(Group.DataList.class)
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    // authorization checked by ConsumerGroupService
    public CompletionStage<Response> listTopicConsumerGroups(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @PathParam("topicId")
            @KafkaUuid(payload = ErrorCategory.ResourceNotFound.class, message = "No such topic")
            @Parameter(description = "Topic identifier")
            String topicId,

            @QueryParam(Group.FIELDS_PARAM)
            @DefaultValue(Group.Fields.LIST_DEFAULT)
            @StringEnumeration(
                    source = Group.FIELDS_PARAM,
                    allowedValues = {
                        Group.Fields.GROUP_ID,
                        Group.Fields.TYPE,
                        Group.Fields.PROTOCOL,
                        Group.Fields.STATE,
                        Group.Fields.SIMPLE_CONSUMER_GROUP,
                        Group.Fields.MEMBERS,
                        Group.Fields.OFFSETS,
                        Group.Fields.AUTHORIZED_OPERATIONS,
                        Group.Fields.COORDINATOR,
                        Group.Fields.PARTITION_ASSIGNOR
                    },
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(
                    description = FieldFilter.FIELDS_DESCR,
                    explode = Explode.FALSE,
                    schema = @Schema(
                            type = SchemaType.ARRAY,
                            implementation = String.class,
                            enumeration = {
                                Group.Fields.GROUP_ID,
                                Group.Fields.TYPE,
                                Group.Fields.PROTOCOL,
                                Group.Fields.STATE,
                                Group.Fields.SIMPLE_CONSUMER_GROUP,
                                Group.Fields.MEMBERS,
                                Group.Fields.OFFSETS,
                                Group.Fields.AUTHORIZED_OPERATIONS,
                                Group.Fields.COORDINATOR,
                                Group.Fields.PARTITION_ASSIGNOR
                            }))
            List<String> fields,

            @BeanParam
            @Valid
            ListFetchParams listParams) {

        requestedFields.accept(fields);
        ListRequestContext<Group> listSupport = new ListRequestContext<>(Group.Fields.COMPARATOR_BUILDER, uriInfo.getRequestUri(), listParams, Group::fromCursor);

        return consumerGroupService.listGroups(topicId, fields, listSupport)
                .thenApply(groups -> new Group.DataList(groups, listSupport))
                .thenApply(Response::ok)
                .thenApply(Response.ResponseBuilder::build);
    }

    @Path("{topicId}")
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
    @Authorized
    @ResourcePrivilege(Privilege.UPDATE)
    public CompletionStage<Response> patchTopic(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @PathParam("topicId")
            @KafkaUuid(payload = ErrorCategory.ResourceNotFound.class, message = "No such topic")
            @Parameter(description = "Topic identifier")
            String topicId,

            @Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = TopicPatch.TopicPatchDocument.class),
                    examples = {
                        @ExampleObject(
                            name = "patchTopic-simple",
                            externalValue = "/openapi/examples/patchTopic-simple.json"),
                        @ExampleObject(
                            name = "patchTopic-validateOnly",
                            externalValue = "/openapi/examples/patchTopic-validateOnly.json")
                    })
            )
            TopicPatch.TopicPatchDocument topic) {

        final boolean validateOnly = Boolean.TRUE.equals(topic.meta("validateOnly"));

        return topicService.patchTopic(topicId, topic.getData().getAttributes(), validateOnly)
                .thenApply(nothing -> Response.noContent())
                .thenApply(Response.ResponseBuilder::build);
    }

    @Path("{topicId}/metrics")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", description = "Topic metrics")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @Authorized
    @ResourcePrivilege(Privilege.GET)
    public CompletionStage<Response> getTopicMetrics(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @PathParam("topicId")
            @KafkaUuid(payload = ErrorCategory.ResourceNotFound.class, message = "No such topic")
            @Parameter(description = "Topic identifier")
            String topicId,

            @QueryParam("duration[metrics]") @DefaultValue("5") int durationMinutes) {

        return topicService.getTopicMetrics(topicId, durationMinutes)
                .thenApply(metrics -> new TopicMetrics.MetricsResponse(topicId, metrics))
                .thenApply(Response::ok)
                .thenApply(Response.ResponseBuilder::build);
    }
}
