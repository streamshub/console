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

import com.github.streamshub.console.api.model.ListFetchParams;
import com.github.streamshub.console.api.model.connect.ConnectCluster;
import com.github.streamshub.console.api.model.connect.ConnectClusterFilterParams;
import com.github.streamshub.console.api.security.Authorized;
import com.github.streamshub.console.api.security.ResourcePrivilege;
import com.github.streamshub.console.api.service.KafkaConnectService;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.FieldFilter;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.api.support.StringEnumeration;
import com.github.streamshub.console.config.security.Privilege;

@Path("/api/connects")
@Tag(name = "Kafka Connect Resources")
public class KafkaConnectsResource {

    @Inject
    UriInfo uriInfo;

    @Inject
    KafkaConnectService connectService;

    @Inject
    FieldFilter fieldFilter;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(ConnectCluster.DataList.class)
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    @Authorized
    @ResourcePrivilege(Privilege.LIST)
    public CompletionStage<Response> listConnectClusters(
            @QueryParam(ConnectCluster.FIELDS_PARAM)
            @DefaultValue(ConnectCluster.Fields.LIST_DEFAULT)
            @StringEnumeration(
                    source = ConnectCluster.FIELDS_PARAM,
                    enumeration = ConnectCluster.Fields.class,
                    message = "list contains a value that is not valid or not available for the operation",
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(ref = ConnectCluster.FIELDS_PARAM)
            List<String> connectFields,

            @Valid
            @BeanParam
            ListFetchParams listParams,

            @Valid
            @BeanParam
            ConnectClusterFilterParams filters) {

        fieldFilter.setTypedFields(Map.of(ConnectCluster.FIELDS_PARAM, connectFields));

        ListRequestContext<ConnectCluster> listSupport = new ListRequestContext<>(
            filters.buildPredicates(),
            ConnectCluster.Fields.COMPARATOR_BUILDER,
            uriInfo.getRequestUri(),
            listParams,
            ConnectCluster::fromCursor
        );

        return connectService.listClusters(fieldFilter, listSupport)
            .thenApply(clusterList -> new ConnectCluster.DataList(clusterList, listSupport))
            .thenApply(Response::ok)
            .thenApply(ResponseBuilder::build);
    }
}
