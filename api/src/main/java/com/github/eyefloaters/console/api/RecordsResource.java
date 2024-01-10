package com.github.eyefloaters.console.api;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.RuntimeDelegate;
import jakarta.ws.rs.core.Response.Status;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.github.eyefloaters.console.api.model.KafkaRecord;
import com.github.eyefloaters.console.api.model.RecordFilterParams;
import com.github.eyefloaters.console.api.service.RecordService;
import com.github.eyefloaters.console.api.support.ErrorCategory;
import com.github.eyefloaters.console.api.support.FieldFilter;
import com.github.eyefloaters.console.api.support.KafkaUuid;
import com.github.eyefloaters.console.api.support.StringEnumeration;

@Path("/api/kafkas/{clusterId}/topics/{topicId}/records")
@Tag(name = "Kafka Cluster Resources")
public class RecordsResource {

    static final String FIELDS_PARAM = "fields[records]";

    @Inject
    UriInfo uriInfo;

    @Inject
    RecordService recordService;

    /**
     * Allows the value of {@link FieldFilter#requestedFields} to be set for
     * the request.
     */
    @Inject
    @Named("requestedFields")
    Consumer<List<String>> requestedFields;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Consume records from a topic",
        description = "Consume a limited number of records from a topic, optionally specifying a partition and an absolute offset or timestamp as the starting point for message retrieval.")
    @APIResponseSchema(
        value = KafkaRecord.ListResponse.class,
        responseDescription = "List of records matching the request query parameters.")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public Response consumeRecords(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @PathParam("topicId")
            @KafkaUuid(payload = ErrorCategory.ResourceNotFound.class, message = "No such topic")
            @Parameter(description = "Topic identifier")
            String topicId,

            @Valid
            @BeanParam
            RecordFilterParams params,

            @QueryParam(FIELDS_PARAM)
            @DefaultValue(KafkaRecord.Fields.DEFAULT)
            @StringEnumeration(
                    source = FIELDS_PARAM,
                    allowedValues = {
                        KafkaRecord.Fields.PARTITION,
                        KafkaRecord.Fields.OFFSET,
                        KafkaRecord.Fields.TIMESTAMP,
                        KafkaRecord.Fields.TIMESTAMP_TYPE,
                        KafkaRecord.Fields.HEADERS,
                        KafkaRecord.Fields.KEY,
                        KafkaRecord.Fields.VALUE,
                        KafkaRecord.Fields.SIZE
                    },
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(
                    description = FieldFilter.FIELDS_DESCR,
                    explode = Explode.FALSE,
                    allowEmptyValue = true,
                    schema = @Schema(
                            type = SchemaType.ARRAY,
                            implementation = String.class,
                            enumeration = {
                                KafkaRecord.Fields.PARTITION,
                                KafkaRecord.Fields.OFFSET,
                                KafkaRecord.Fields.TIMESTAMP,
                                KafkaRecord.Fields.TIMESTAMP_TYPE,
                                KafkaRecord.Fields.HEADERS,
                                KafkaRecord.Fields.KEY,
                                KafkaRecord.Fields.VALUE,
                                KafkaRecord.Fields.SIZE
                            }))
            List<String> fields) {

        requestedFields.accept(fields);
        var result = recordService.consumeRecords(topicId, params.getPartition(), params.getOffset(), params.getTimestamp(), params.getLimit(), fields, params.getMaxValueLength());

        CacheControl noStore = RuntimeDelegate.getInstance().createHeaderDelegate(CacheControl.class).fromString("no-store");
        return Response.ok(new KafkaRecord.ListResponse(result)).cacheControl(noStore).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Send a record to a topic",
        description = "Produce (write) a single record to a topic")
    @APIResponseSchema(
        responseCode = "201",
        value = KafkaRecord.KafkaRecordDocument.class,
        responseDescription = "Record was successfully sent to the topic")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public CompletionStage<Response> produceRecord(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @PathParam("topicId")
            @KafkaUuid(payload = ErrorCategory.ResourceNotFound.class, message = "No such topic")
            @Parameter(description = "Topic identifier")
            String topicId,

            @Valid
            KafkaRecord.KafkaRecordDocument message) {

        final UriBuilder location = uriInfo.getRequestUriBuilder();
        requestedFields.accept(KafkaRecord.Fields.ALL);

        return recordService.produceRecord(topicId, message.getData().getAttributes())
            .thenApply(KafkaRecord.KafkaRecordDocument::new)
            .thenApply(entity -> Response.status(Status.CREATED)
                    .entity(entity)
                    .location(location
                            .queryParam("filter[partition]", entity.getData().getAttributes().getPartition())
                            .queryParam("filter[offset]", entity.getData().getAttributes().getOffset())
                            .build()))
            .thenApply(Response.ResponseBuilder::build);

    }
}
