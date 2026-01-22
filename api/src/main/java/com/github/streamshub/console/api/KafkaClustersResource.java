package com.github.streamshub.console.api;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletionStage;
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
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.github.streamshub.console.api.model.KafkaCluster;
import com.github.streamshub.console.api.model.KafkaClusterFilterParams;
import com.github.streamshub.console.api.model.ListFetchParams;
import com.github.streamshub.console.api.security.Authorized;
import com.github.streamshub.console.api.security.ResourcePrivilege;
import com.github.streamshub.console.api.service.KafkaClusterService;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.FieldFilter;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.api.support.StringEnumeration;
import com.github.streamshub.console.config.security.Privilege;

import io.xlate.validation.constraints.Expression;

@Path("/api/kafkas")
@Tag(name = "Kafka Cluster Resources")
public class KafkaClustersResource {

    @Inject
    UriInfo uriInfo;

    @Inject
    KafkaClusterService clusterService;

    /**
     * Allows the value of {@link FieldFilter#requestedFields} to be set for
     * the request.
     */
    @Inject
    @Named("requestedFields")
    Consumer<List<String>> requestedFields;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(KafkaCluster.KafkaClusterDataList.class)
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    @Authorized
    @ResourcePrivilege(Privilege.LIST)
    public Response listClusters(
            @QueryParam(KafkaCluster.FIELDS_PARAM)
            @DefaultValue(KafkaCluster.Fields.LIST_DEFAULT)
            @StringEnumeration(
                    source = KafkaCluster.FIELDS_PARAM,
                    allowedValues = {
                        KafkaCluster.Fields.NAME,
                        KafkaCluster.Fields.NAMESPACE,
                        KafkaCluster.Fields.CREATION_TIMESTAMP,
                        KafkaCluster.Fields.LISTENERS,
                        KafkaCluster.Fields.KAFKA_VERSION,
                        KafkaCluster.Fields.STATUS,
                        KafkaCluster.Fields.CONDITIONS,
                        KafkaCluster.Fields.NODE_POOLS,
                        KafkaCluster.Fields.CRUISE_CONTROL_ENABLED,
                    },
                    message = "list contains a value that is not valid or not available for the operation",
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(
                    description = FieldFilter.FIELDS_DESCR,
                    explode = Explode.FALSE,
                    allowEmptyValue = true,
                    schema = @Schema(
                            type = SchemaType.ARRAY,
                            implementation = String.class,
                            enumeration = {
                                KafkaCluster.Fields.NAME,
                                KafkaCluster.Fields.NAMESPACE,
                                KafkaCluster.Fields.CREATION_TIMESTAMP,
                                KafkaCluster.Fields.LISTENERS,
                                KafkaCluster.Fields.KAFKA_VERSION,
                                KafkaCluster.Fields.STATUS,
                                KafkaCluster.Fields.CONDITIONS,
                                KafkaCluster.Fields.NODE_POOLS,
                                KafkaCluster.Fields.CRUISE_CONTROL_ENABLED,
                            }))
            List<String> fields,

            @Valid
            @BeanParam
            ListFetchParams listParams,

            @Valid
            @BeanParam
            KafkaClusterFilterParams filters) {

        requestedFields.accept(fields);

        ListRequestContext<KafkaCluster> listSupport = new ListRequestContext<>(
                filters,
                KafkaCluster.Fields.COMPARATOR_BUILDER,
                uriInfo.getRequestUri(),
                listParams,
                KafkaCluster::fromCursor);

        var clusterList = clusterService.listClusters(listSupport);
        var responseEntity = new KafkaCluster.KafkaClusterDataList(clusterList, listSupport);

        return Response.ok(responseEntity).build();
    }

    @GET
    @Path("{clusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(KafkaCluster.KafkaClusterData.class)
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    @Authorized
    @ResourcePrivilege(Privilege.GET)
    public CompletionStage<Response> describeCluster(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @QueryParam(KafkaCluster.FIELDS_PARAM)
            @DefaultValue(KafkaCluster.Fields.DESCRIBE_DEFAULT)
            @StringEnumeration(
                    source = KafkaCluster.FIELDS_PARAM,
                    allowedValues = {
                        KafkaCluster.Fields.NAME,
                        KafkaCluster.Fields.NAMESPACE,
                        KafkaCluster.Fields.CREATION_TIMESTAMP,
                        KafkaCluster.Fields.NODES,
                        KafkaCluster.Fields.AUTHORIZED_OPERATIONS,
                        KafkaCluster.Fields.LISTENERS,
                        KafkaCluster.Fields.METRICS,
                        KafkaCluster.Fields.KAFKA_VERSION,
                        KafkaCluster.Fields.STATUS,
                        KafkaCluster.Fields.CONDITIONS,
                        KafkaCluster.Fields.NODE_POOLS,
                        KafkaCluster.Fields.CRUISE_CONTROL_ENABLED,
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
                                KafkaCluster.Fields.NAME,
                                KafkaCluster.Fields.NAMESPACE,
                                KafkaCluster.Fields.CREATION_TIMESTAMP,
                                KafkaCluster.Fields.NODES,
                                KafkaCluster.Fields.AUTHORIZED_OPERATIONS,
                                KafkaCluster.Fields.LISTENERS,
                                KafkaCluster.Fields.METRICS,
                                KafkaCluster.Fields.KAFKA_VERSION,
                                KafkaCluster.Fields.STATUS,
                                KafkaCluster.Fields.CONDITIONS,
                                KafkaCluster.Fields.NODE_POOLS,
                                KafkaCluster.Fields.CRUISE_CONTROL_ENABLED,
                            }))
            List<String> fields,

            @Parameter(description = "Time range for metrics in minutes")
            @QueryParam("duration") 
            Integer durationMinutes) {

        requestedFields.accept(fields);

        return clusterService.describeCluster(fields, durationMinutes)
            .thenApply(KafkaCluster.KafkaClusterData::new)
            .thenApply(Response::ok)
            .thenApply(Response.ResponseBuilder::build);
    }


    @Path("{clusterId}")
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(responseCode = "200", value = KafkaCluster.KafkaClusterData.class)
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
    public Response patchCluster(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @Valid
            KafkaCluster.KafkaClusterData clusterData) {

        // Return all fields
        requestedFields.accept(Arrays.asList(KafkaCluster.Fields.DESCRIBE_DEFAULT.split(",\\s*")));

        var result = clusterService.patchCluster(clusterData.getData());
        var responseEntity = new KafkaCluster.KafkaClusterData(result);

        return Response.ok(responseEntity).build();
    }
}
