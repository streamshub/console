package com.github.streamshub.console.api;

import java.util.List;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.Context;
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
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.RuntimeDelegate;
import jakarta.ws.rs.sse.Sse;
import jakarta.ws.rs.sse.SseEventSink;

import io.smallrye.common.annotation.Blocking;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.github.streamshub.console.api.model.KafkaRecord;
import com.github.streamshub.console.api.model.RecordFilterParams;
import com.github.streamshub.console.api.security.Authorized;
import com.github.streamshub.console.api.security.ResourcePrivilege;
import com.github.streamshub.console.api.service.RecordService;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.FieldFilter;
import com.github.streamshub.console.api.support.KafkaUuid;
import com.github.streamshub.console.api.support.StringEnumeration;
import com.github.streamshub.console.config.security.Privilege;

@Path("kafkas/{clusterId}/topics/{topicId}/records")
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

    @Context
    Sse sse;

    @GET
    @Path("{partition}/{offset}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Retrieve a single record from a topic",
        description = "Retrieve the record at the given partition and offset of a topic.")
    @APIResponseSchema(
        value = KafkaRecord.KafkaRecordData.class,
        responseDescription = "The record at the requested partition and offset.")
    @APIResponse(
        responseCode = "404",
        ref = "NotFound",
        description = "The topic, partition, or record at the requested offset does not exist.")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    @Authorized
    @ResourcePrivilege(Privilege.GET)
    public Response getRecord(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @PathParam("topicId")
            @KafkaUuid(payload = ErrorCategory.ResourceNotFound.class, message = "No such topic")
            @Parameter(description = "Topic identifier")
            String topicId,

            @PathParam("partition")
            @Min(value = 0, payload = ErrorCategory.ResourceNotFound.class, message = "No such partition")
            @Parameter(description = "Partition of the record within the topic")
            int partition,

            @PathParam("offset")
            @Min(value = 0, payload = ErrorCategory.ResourceNotFound.class, message = "No such record")
            @Parameter(description = "Offset of the record within the topic partition")
            long offset,

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
                        KafkaRecord.Fields.SIZE,
                        KafkaRecord.Fields.KEY_SCHEMA,
                        KafkaRecord.Fields.VALUE_SCHEMA,
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
                                KafkaRecord.Fields.SIZE,
                                KafkaRecord.Fields.KEY_SCHEMA,
                                KafkaRecord.Fields.VALUE_SCHEMA,
                            }))
            List<String> fields) {

        requestedFields.accept(fields);
        CacheControl noStore = RuntimeDelegate.getInstance().createHeaderDelegate(CacheControl.class).fromString("no-store");
        var rec = recordService.getRecord(topicId, partition, offset, fields);

        return Response.ok(new KafkaRecord.KafkaRecordData(rec))
                .cacheControl(noStore)
                .build();
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Operation(
        summary = "Stream records from a topic",
        description = "Stream records from a topic as Server-Sent Events. Each SSE data event "
            + "contains a JSON array of matching record objects. After each batch of records "
            + "polled from Kafka, an event named 'heartbeat' with data "
            + "`{\"scanned\":<n>,\"progress\":<p>,\"elapsed\":<e>,\"remaining\":<r>}` "
            + "reports the total number of records scanned so far, whether or not they matched, the "
            + "estimated percentage (0-100) of the requested offset range that has been scanned, the "
            + "milliseconds the stream has been running, and the milliseconds left before it is ended with "
            + "the 'timeout' reason. A terminal "
            + "event named 'done' with data `{\"reason\":\"<reason>\"}` signals that scanning has ended. The reason "
            + "is one of 'complete' (all requested records were read), 'limit' (the requested number of matches "
            + "was found), 'timeout' (time ran out before all records were read), or 'cancelled'. "
            + "Records may be selected using filter[partition], filter[offset], filter[timestamp], and "
            + "server-side filter[key], filter[value], and filter[headers] search using the 'like' operator.")
    @APIResponse(
        responseCode = "200",
        description = "Stream of record batches as SSE data events, terminated by an event named 'done'.")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(
        responseCode = "429",
        description = "Too many record streams are running, retry later.")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    @Authorized
    @ResourcePrivilege(Privilege.LIST)
    @Blocking // Polls a blocking Kafka consumer for the lifetime of the stream
    public void streamRecords(
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
                        KafkaRecord.Fields.SIZE,
                        KafkaRecord.Fields.KEY_SCHEMA,
                        KafkaRecord.Fields.VALUE_SCHEMA,
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
                                KafkaRecord.Fields.SIZE,
                                KafkaRecord.Fields.KEY_SCHEMA,
                                KafkaRecord.Fields.VALUE_SCHEMA,
                            }))
            List<String> fields,

            SseEventSink sink) {

        requestedFields.accept(fields);
        recordService.consumeRecords(topicId, fields, params, sink, sse);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Send a record to a topic",
        description = "Produce (write) a single record to a topic")
    @APIResponseSchema(
        responseCode = "201",
        value = KafkaRecord.KafkaRecordData.class,
        responseDescription = "Record was successfully sent to the topic")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    @Authorized
    @ResourcePrivilege(Privilege.CREATE)
    public Response produceRecord(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @PathParam("topicId")
            @KafkaUuid(payload = ErrorCategory.ResourceNotFound.class, message = "No such topic")
            @Parameter(description = "Topic identifier")
            String topicId,

            @Valid
            KafkaRecord.KafkaRecordData message) {

        final UriBuilder location = uriInfo.getRequestUriBuilder();
        requestedFields.accept(KafkaRecord.Fields.ALL);
        var entity = new KafkaRecord.KafkaRecordData(recordService.produceRecord(topicId, message.getData()));

        return Response.status(Status.CREATED)
                .entity(entity)
                .location(location
                        .path("{partition}")
                        .path("{offset}")
                        .build(entity.getData().partition(), entity.getData().offset()))
                .build();
    }
}
