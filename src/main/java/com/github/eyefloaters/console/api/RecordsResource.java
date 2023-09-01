package com.github.eyefloaters.console.api;

import java.util.List;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
    @Parameter(
        name = "topicName",
        description = "Topic name")
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
            @KafkaUuid(category = ErrorCategory.RESOURCE_NOT_FOUND, message = "No such topic")
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
                        KafkaRecord.Fields.VALUE
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
                                KafkaRecord.Fields.PARTITION,
                                KafkaRecord.Fields.OFFSET,
                                KafkaRecord.Fields.TIMESTAMP,
                                KafkaRecord.Fields.TIMESTAMP_TYPE,
                                KafkaRecord.Fields.HEADERS,
                                KafkaRecord.Fields.KEY,
                                KafkaRecord.Fields.VALUE
                            }))
            List<String> fields) {

        requestedFields.accept(fields);
        var result = recordService.consumeRecords(topicId, params.getPartition(), params.getOffset(), params.getTimestamp(), params.getLimit(), fields, params.getMaxValueLength());

        return Response.ok(new KafkaRecord.ListResponse(result)).build();

    }
}
