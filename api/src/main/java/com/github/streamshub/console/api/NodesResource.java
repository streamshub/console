package com.github.streamshub.console.api;

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
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriInfo;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.github.streamshub.console.api.model.ConfigEntry;
import com.github.streamshub.console.api.model.ListFetchParams;
import com.github.streamshub.console.api.model.Node;
import com.github.streamshub.console.api.model.NodeFilterParams;
import com.github.streamshub.console.api.model.NodeMetrics;
import com.github.streamshub.console.api.security.Authorized;
import com.github.streamshub.console.api.security.ResourcePrivilege;
import com.github.streamshub.console.api.service.NodeService;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.FieldFilter;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.api.support.StringEnumeration;
import com.github.streamshub.console.config.security.Privilege;

@Path("/api/kafkas/{clusterId}/nodes")
@Tag(name = "Kafka Cluster Resources")
public class NodesResource {

    @Inject
    UriInfo uriInfo;

    @Inject
    NodeService nodeService;

    /**
     * Allows the value of {@link FieldFilter#requestedFields} to be set for
     * the request.
     */
    @Inject
    @Named("requestedFields")
    Consumer<List<String>> requestedFields;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(Node.NodeDataList.class)
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    @Authorized
    @ResourcePrivilege(Privilege.LIST)
    public CompletionStage<Response> listNodes(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @QueryParam(Node.FIELDS_PARAM)
            @DefaultValue(Node.Fields.LIST_DEFAULT)
            @StringEnumeration(
                    source = Node.FIELDS_PARAM,
                    allowedValues = {
                        Node.Fields.NODE_POOL,
                        Node.Fields.ROLES,
                        Node.Fields.KAFKA_VERSION,
                        Node.Fields.METADATA_STATE,
                        Node.Fields.BROKER,
                        Node.Fields.CONTROLLER,
                        Node.Fields.HOST,
                        Node.Fields.PORT,
                        Node.Fields.RACK,
                        Node.Fields.STORAGE_USED,
                        Node.Fields.STORAGE_CAPACITY,
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
                                Node.Fields.NODE_POOL,
                                Node.Fields.ROLES,
                                Node.Fields.KAFKA_VERSION,
                                Node.Fields.METADATA_STATE,
                                Node.Fields.BROKER,
                                Node.Fields.CONTROLLER,
                                Node.Fields.HOST,
                                Node.Fields.PORT,
                                Node.Fields.RACK,
                                Node.Fields.STORAGE_USED,
                                Node.Fields.STORAGE_CAPACITY,
                            }))
            List<String> fields,

            @Valid
            @BeanParam
            ListFetchParams listParams,

            @BeanParam
            @Valid
            NodeFilterParams filters) {

        requestedFields.accept(fields);

        ListRequestContext<Node> listSupport = new ListRequestContext<>(
                filters,
                Node.Fields.COMPARATOR_BUILDER,
                uriInfo.getRequestUri(),
                listParams,
                Node::fromCursor);

        return nodeService.listNodes(listSupport)
            .thenApply(nodeList -> new Node.NodeDataList(nodeList, listSupport))
            .thenApply(Response::ok)
            .thenApply(ResponseBuilder::build);
    }

    @GET
    @Path("{nodeId}/configs")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", ref = "Configurations", content = @Content())
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    @Authorized
    @ResourcePrivilege(Privilege.GET)
    public CompletionStage<Response> describeConfigs(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @PathParam("nodeId")
            @Parameter(description = "Node identifier")
            String nodeId) {

        return nodeService.describeConfigs(nodeId)
            .thenApply(ConfigEntry.ConfigResponse::new)
            .thenApply(Response::ok)
            .thenApply(Response.ResponseBuilder::build);
    }

    @GET
    @Path("{nodeId}/metrics")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", description = "Node metrics")
    @APIResponse(responseCode = "404", ref = "NotFound")
    @Authorized
    @ResourcePrivilege(Privilege.GET)
    public CompletionStage<Response> getNodeMetrics(
            @PathParam("clusterId") String clusterId,
            @PathParam("nodeId") String nodeId) {

        return nodeService.getNodeMetrics(nodeId)
            .thenApply(metrics -> new NodeMetrics.MetricsResponse(nodeId, metrics))
            .thenApply(Response::ok)
            .thenApply(Response.ResponseBuilder::build);
    }
}
