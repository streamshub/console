package com.github.streamshub.console.api;

import java.util.List;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.Valid;
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
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Consume records from a topic",
        description = "Consume a limited number of records from a topic, optionally specifying a partition and an absolute offset or timestamp as the starting point for message retrieval.")
    @APIResponseSchema(
        value = KafkaRecord.KafkaRecordDataList.class,
        responseDescription = "List of records matching the request query parameters.")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    @Authorized
    @ResourcePrivilege(Privilege.LIST)
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
        var records = recordService.consumeRecords(
                topicId,
                fields,
                params);

        return Response.ok(new KafkaRecord.KafkaRecordDataList(records))
                .cacheControl(noStore)
                .build();
    }

    @GET
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @Operation(
        summary = "Stream records from a topic",
        description = "Stream records from a topic as Server-Sent Events. Each SSE data event "
            + "contains a JSON array of matching record objects. After each batch of records "
            + "polled from Kafka, an event named 'heartbeat' with data `{\"scanned\":<n>,\"progress\":<p>}` "
            + "reports the total number of records scanned so far, whether or not they matched, and the "
            + "estimated percentage (0-100) of the requested offset range that has been scanned. A terminal "
            + "event named 'done' with data `{\"reason\":\"<reason>\"}` signals that scanning has ended. The reason "
            + "is one of 'complete' (all requested records were read), 'limit' (the requested number of matches "
            + "was found), 'timeout' (time ran out before all records were read), or 'cancelled'. "
            + "The same filter and field parameters as the "
            + "JSON endpoint are supported, including server-side filter[key], filter[value], "
            + "and filter[headers] search using the 'like' operator.")
    @APIResponse(
        responseCode = "200",
        description = "Stream of record batches as SSE data events, terminated by an event named 'done'.")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    @Authorized
    @ResourcePrivilege(Privilege.LIST)
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
                        .queryParam("filter[partition]", entity.getData().partition())
                        .queryParam("filter[offset]", entity.getData().offset())
                        .build())
                .build();
    }
}
