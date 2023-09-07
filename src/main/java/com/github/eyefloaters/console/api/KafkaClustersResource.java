package com.github.eyefloaters.console.api;

import java.util.List;
import java.util.concurrent.CompletionStage;
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
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.github.eyefloaters.console.api.model.KafkaCluster;
import com.github.eyefloaters.console.api.model.ListFetchParams;
import com.github.eyefloaters.console.api.service.KafkaClusterService;
import com.github.eyefloaters.console.api.support.ErrorCategory;
import com.github.eyefloaters.console.api.support.FieldFilter;
import com.github.eyefloaters.console.api.support.ListRequestContext;
import com.github.eyefloaters.console.api.support.StringEnumeration;

@Path("/api/kafkas")
@Tag(name = "Kafka Cluster Resources")
public class KafkaClustersResource {

    static final String FIELDS_PARAM = "fields[kafkas]";

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
    @APIResponseSchema(KafkaCluster.ListResponse.class)
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public Response listClusters(
            @QueryParam(FIELDS_PARAM)
            @DefaultValue(KafkaCluster.Fields.LIST_DEFAULT)
            @StringEnumeration(
                    source = FIELDS_PARAM,
                    allowedValues = {
                        KafkaCluster.Fields.NAME,
                        KafkaCluster.Fields.NAMESPACE,
                        KafkaCluster.Fields.CREATION_TIMESTAMP,
                        KafkaCluster.Fields.BOOTSTRAP_SERVERS,
                        KafkaCluster.Fields.AUTH_TYPE
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
                                KafkaCluster.Fields.BOOTSTRAP_SERVERS,
                                KafkaCluster.Fields.AUTH_TYPE
                            }))
            List<String> fields,

            @Valid
            @BeanParam
            ListFetchParams listParams) {

        requestedFields.accept(fields);

        ListRequestContext listSupport = new ListRequestContext(uriInfo.getRequestUri(), listParams);
        var clusterList = clusterService.listClusters(listSupport);
        var responseEntity = new KafkaCluster.ListResponse(clusterList);

        return Response.ok(responseEntity).build();
    }

    @GET
    @Path("{clusterId}")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(KafkaCluster.SingleResponse.class)
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public CompletionStage<Response> describeCluster(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @QueryParam(FIELDS_PARAM)
            @DefaultValue(KafkaCluster.Fields.DESCRIBE_DEFAULT)
            @StringEnumeration(
                    source = FIELDS_PARAM,
                    allowedValues = {
                        KafkaCluster.Fields.NAME,
                        KafkaCluster.Fields.NAMESPACE,
                        KafkaCluster.Fields.CREATION_TIMESTAMP,
                        KafkaCluster.Fields.NODES,
                        KafkaCluster.Fields.CONTROLLER,
                        KafkaCluster.Fields.AUTHORIZED_OPERATIONS,
                        KafkaCluster.Fields.BOOTSTRAP_SERVERS,
                        KafkaCluster.Fields.AUTH_TYPE
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
                                KafkaCluster.Fields.CONTROLLER,
                                KafkaCluster.Fields.AUTHORIZED_OPERATIONS,
                                KafkaCluster.Fields.BOOTSTRAP_SERVERS,
                                KafkaCluster.Fields.AUTH_TYPE
                            }))
            List<String> fields) {

        requestedFields.accept(fields);

        return clusterService.describeCluster(fields)
            .thenApply(KafkaCluster.SingleResponse::new)
            .thenApply(Response::ok)
            .thenApply(Response.ResponseBuilder::build);
    }

}
