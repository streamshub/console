package com.github.streamshub.console.api;

import java.util.List;
import java.util.function.Consumer;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.validation.ConstraintTarget;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
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
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.github.streamshub.console.api.model.KafkaRebalance;
import com.github.streamshub.console.api.model.KafkaRebalanceFilterParams;
import com.github.streamshub.console.api.model.ListFetchParams;
import com.github.streamshub.console.api.service.KafkaRebalanceService;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.FieldFilter;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.api.support.StringEnumeration;

import io.xlate.validation.constraints.Expression;

@Path("/api/kafkas/{clusterId}/rebalances")
@Tag(name = "Kafka Cluster Resources")
public class KafkaRebalancesResource {

    @Inject
    UriInfo uriInfo;

    @Inject
    KafkaRebalanceService rebalanceService;

    /**
     * Allows the value of {@link FieldFilter#requestedFields} to be set for
     * the request.
     */
    @Inject
    @Named("requestedFields")
    Consumer<List<String>> requestedFields;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(KafkaRebalance.ListResponse.class)
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public Response listRebalances(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @QueryParam(KafkaRebalance.FIELDS_PARAM)
            @DefaultValue(KafkaRebalance.Fields.LIST_DEFAULT)
            @StringEnumeration(
                    source = KafkaRebalance.FIELDS_PARAM,
                    allowedValues = {
                        KafkaRebalance.Fields.NAME,
                        KafkaRebalance.Fields.NAMESPACE,
                        KafkaRebalance.Fields.CREATION_TIMESTAMP,
                        KafkaRebalance.Fields.STATUS,
                        KafkaRebalance.Fields.MODE,
                        KafkaRebalance.Fields.BROKERS,
                        KafkaRebalance.Fields.GOALS,
                        KafkaRebalance.Fields.SKIP_HARD_GOAL_CHECK,
                        KafkaRebalance.Fields.REBALANCE_DISK,
                        KafkaRebalance.Fields.EXCLUDED_TOPICS,
                        KafkaRebalance.Fields.CONCURRENT_PARTITION_MOVEMENTS_PER_BROKER,
                        KafkaRebalance.Fields.CONCURRENT_INTRABROKER_PARTITION_MOVEMENTS,
                        KafkaRebalance.Fields.CONCURRENT_LEADER_MOVEMENTS,
                        KafkaRebalance.Fields.REPLICATION_THROTTLE,
                        KafkaRebalance.Fields.REPLICA_MOVEMENT_STRATEGIES,
                        KafkaRebalance.Fields.OPTIMIZATION_RESULT,
                    },
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(
                    description = FieldFilter.FIELDS_DESCR,
                    explode = Explode.FALSE,
                    schema = @Schema(
                            type = SchemaType.ARRAY,
                            implementation = String.class,
                            enumeration = {
                                KafkaRebalance.Fields.NAME,
                                KafkaRebalance.Fields.NAMESPACE,
                                KafkaRebalance.Fields.CREATION_TIMESTAMP,
                                KafkaRebalance.Fields.STATUS,
                                KafkaRebalance.Fields.MODE,
                                KafkaRebalance.Fields.BROKERS,
                                KafkaRebalance.Fields.GOALS,
                                KafkaRebalance.Fields.SKIP_HARD_GOAL_CHECK,
                                KafkaRebalance.Fields.REBALANCE_DISK,
                                KafkaRebalance.Fields.EXCLUDED_TOPICS,
                                KafkaRebalance.Fields.CONCURRENT_PARTITION_MOVEMENTS_PER_BROKER,
                                KafkaRebalance.Fields.CONCURRENT_INTRABROKER_PARTITION_MOVEMENTS,
                                KafkaRebalance.Fields.CONCURRENT_LEADER_MOVEMENTS,
                                KafkaRebalance.Fields.REPLICATION_THROTTLE,
                                KafkaRebalance.Fields.REPLICA_MOVEMENT_STRATEGIES,
                                KafkaRebalance.Fields.OPTIMIZATION_RESULT,
                            }))
            List<String> fields,

            @BeanParam
            @Valid
            ListFetchParams listParams,

            @BeanParam
            @Valid
            KafkaRebalanceFilterParams filters) {

        requestedFields.accept(fields);

        ListRequestContext<KafkaRebalance> listSupport = new ListRequestContext<>(
                filters.buildPredicates(),
                KafkaRebalance.Fields.COMPARATOR_BUILDER,
                uriInfo.getRequestUri(),
                listParams,
                KafkaRebalance::fromCursor);

        var rebalanceList = rebalanceService.listRebalances(listSupport);
        var responseEntity = new KafkaRebalance.ListResponse(rebalanceList, listSupport);

        return Response.ok(responseEntity).build();
    }

    @Path("{rebalanceId}")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(responseCode = "200", value = KafkaRebalance.Singleton.class)
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
    public Response patchRebalance(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @PathParam("rebalanceId")
            @Parameter(description = "Rebalance identifier")
            String rebalanceId,

            @Valid
            @RequestBody(content = @Content(
                    schema = @Schema(implementation = KafkaRebalance.Singleton.class))
            )
            KafkaRebalance.Singleton rebalance) {

        requestedFields.accept(List.of(
                KafkaRebalance.Fields.NAME,
                KafkaRebalance.Fields.NAMESPACE,
                KafkaRebalance.Fields.CREATION_TIMESTAMP,
                KafkaRebalance.Fields.STATUS,
                KafkaRebalance.Fields.MODE,
                KafkaRebalance.Fields.BROKERS,
                KafkaRebalance.Fields.GOALS,
                KafkaRebalance.Fields.SKIP_HARD_GOAL_CHECK,
                KafkaRebalance.Fields.REBALANCE_DISK,
                KafkaRebalance.Fields.EXCLUDED_TOPICS,
                KafkaRebalance.Fields.CONCURRENT_PARTITION_MOVEMENTS_PER_BROKER,
                KafkaRebalance.Fields.CONCURRENT_INTRABROKER_PARTITION_MOVEMENTS,
                KafkaRebalance.Fields.CONCURRENT_LEADER_MOVEMENTS,
                KafkaRebalance.Fields.REPLICATION_THROTTLE,
                KafkaRebalance.Fields.REPLICA_MOVEMENT_STRATEGIES,
                KafkaRebalance.Fields.OPTIMIZATION_RESULT));

        var result = rebalanceService.patchRebalance(rebalanceId, rebalance.getData());
        var responseEntity = new KafkaRebalance.Singleton(result);

        return Response.ok(responseEntity).build();
    }

}
