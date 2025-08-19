package com.github.streamshub.console.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import jakarta.inject.Inject;
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

import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.github.streamshub.console.api.model.FetchParams;
import com.github.streamshub.console.api.model.ListFetchParams;
import com.github.streamshub.console.api.model.connect.ConnectCluster;
import com.github.streamshub.console.api.model.connect.Connector;
import com.github.streamshub.console.api.model.connect.ConnectorFilterParams;
import com.github.streamshub.console.api.model.connect.ConnectorTask;
import com.github.streamshub.console.api.service.KafkaConnectService;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.FieldFilter;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.api.support.StringEnumeration;

import io.xlate.validation.constraints.Expression;

@Path("/api/connectors")
@Tag(name = "Kafka Connect Resources")
public class KafkaConnectorsResource {

    @Inject
    UriInfo uriInfo;

    @Inject
    FieldFilter fieldFilter;

    @Inject
    KafkaConnectService connectService;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(Connector.DataList.class)
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public CompletionStage<Response> listConnectors(
            @QueryParam(Connector.FIELDS_PARAM)
            @DefaultValue(Connector.Fields.LIST_DEFAULT)
            @StringEnumeration(
                    source = Connector.FIELDS_PARAM,
                    enumeration = Connector.Fields.class,
                    message = "list contains a value that is not valid or not available for the operation",
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(ref = Connector.FIELDS_PARAM)
            List<String> connectorFields,

            @QueryParam(ConnectCluster.FIELDS_PARAM)
            @DefaultValue(ConnectCluster.Fields.LIST_DEFAULT)
            @StringEnumeration(
                    source = ConnectCluster.FIELDS_PARAM,
                    enumeration = ConnectCluster.Fields.class,
                    message = "list contains a value that is not valid or not available for the operation",
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(ref = ConnectCluster.FIELDS_PARAM)
            List<String> connectFields,

            @QueryParam(ConnectorTask.FIELDS_PARAM)
            @DefaultValue(ConnectorTask.Fields.LIST_DEFAULT)
            @StringEnumeration(
                    source = ConnectorTask.FIELDS_PARAM,
                    enumeration = ConnectorTask.Fields.class,
                    message = "list contains a value that is not valid or not available for the operation",
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(ref = ConnectorTask.FIELDS_PARAM)
            List<String> taskFields,

            @Valid
            @BeanParam
            @Expression(
                    when = "self.rawInclude != null",
                    staticImports = "java.util.Arrays.asList",
                    value = "self.includeEntries.stream().allMatch(i -> asList('connectCluster','tasks').contains(i))",
                    message = "invalid include, supported values: [ 'connectCluster', 'tasks' ]",
                    payload = ErrorCategory.InvalidQueryParameter.class,
                    node = FetchParams.INCLUDE_PARAM)
            ListFetchParams listParams,

            @Valid
            @BeanParam
            ConnectorFilterParams filters) {

        fieldFilter.setTypedFields(Map.of(
                Connector.FIELDS_PARAM, connectorFields,
                ConnectCluster.FIELDS_PARAM, connectFields,
                ConnectorTask.FIELDS_PARAM, taskFields
        ));

        ListRequestContext<Connector> listSupport = new ListRequestContext<>(
            filters,
            Connector.Fields.COMPARATOR_BUILDER,
            uriInfo.getRequestUri(),
            listParams,
            Connector::fromCursor
        );

        return connectService.listConnectors(fieldFilter, listSupport)
            .thenApply(clusterList -> new Connector.DataList(clusterList, listSupport))
            .thenApply(Response::ok)
            .thenApply(ResponseBuilder::build);
    }

    @GET
    @Path("{connectorId}")
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(Connector.Data.class)
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public CompletionStage<Response> describeConnector(
            @Parameter(description = "Connector identifier")
            @PathParam("connectorId")
            String connectorId,

            @QueryParam(Connector.FIELDS_PARAM)
            @DefaultValue(Connector.Fields.LIST_DEFAULT)
            @StringEnumeration(
                    source = Connector.FIELDS_PARAM,
                    enumeration = Connector.Fields.class,
                    message = "list contains a value that is not valid or not available for the operation",
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(ref = Connector.FIELDS_PARAM)
            List<String> connectorFields,

            @QueryParam(ConnectCluster.FIELDS_PARAM)
            @DefaultValue(ConnectCluster.Fields.LIST_DEFAULT)
            @StringEnumeration(
                    source = ConnectCluster.FIELDS_PARAM,
                    enumeration = ConnectCluster.Fields.class,
                    message = "list contains a value that is not valid or not available for the operation",
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(ref = ConnectCluster.FIELDS_PARAM)
            List<String> connectFields,

            @QueryParam(ConnectorTask.FIELDS_PARAM)
            @DefaultValue(ConnectorTask.Fields.LIST_DEFAULT)
            @StringEnumeration(
                    source = ConnectorTask.FIELDS_PARAM,
                    enumeration = ConnectorTask.Fields.class,
                    message = "list contains a value that is not valid or not available for the operation",
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(ref = ConnectorTask.FIELDS_PARAM)
            List<String> taskFields,

            @Valid
            @BeanParam
            @Expression(
                    when = "self.rawInclude != null",
                    staticImports = "java.util.Arrays.asList",
                    value = "self.includeEntries.stream().allMatch(i -> asList('connectCluster','tasks').contains(i))",
                    message = "invalid include, supported values: [ 'connectCluster', 'tasks' ]",
                    payload = ErrorCategory.InvalidQueryParameter.class,
                    node = FetchParams.INCLUDE_PARAM)
            FetchParams describeParams) {

        fieldFilter.setTypedFields(Map.of(
                Connector.FIELDS_PARAM, connectorFields,
                ConnectCluster.FIELDS_PARAM, connectFields,
                ConnectorTask.FIELDS_PARAM, taskFields
        ));

        return connectService.describeConnector(connectorId, fieldFilter, describeParams)
            .thenApply(cluster -> new Connector.Data(cluster, describeParams))
            .thenApply(Response::ok)
            .thenApply(ResponseBuilder::build);
    }
}
