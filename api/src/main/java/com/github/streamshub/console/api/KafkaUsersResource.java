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

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponseSchema;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import com.github.streamshub.console.api.model.KafkaUser;
import com.github.streamshub.console.api.model.KafkaUserFilterParams;
import com.github.streamshub.console.api.model.ListFetchParams;
import com.github.streamshub.console.api.security.Authorized;
import com.github.streamshub.console.api.security.ResourcePrivilege;
import com.github.streamshub.console.api.service.KafkaUserService;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.FieldFilter;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.api.support.StringEnumeration;
import com.github.streamshub.console.config.security.Privilege;

@Path("/api/kafkas/{clusterId}/users")
@Tag(name = "Kafka Cluster Resources")
public class KafkaUsersResource {

    @Inject
    UriInfo uriInfo;

    @Inject
    KafkaUserService userService;

    @Inject
    FieldFilter fieldFilter;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(KafkaUser.DataList.class)
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    @Authorized
    @ResourcePrivilege(Privilege.LIST)
    public CompletionStage<Response> listUsers(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @QueryParam(KafkaUser.FIELDS_PARAM)
            @DefaultValue(KafkaUser.Fields.LIST_DEFAULT)
            @StringEnumeration(
                    source = KafkaUser.FIELDS_PARAM,
                    allowedValues = {
                        KafkaUser.Fields.NAME,
                        KafkaUser.Fields.NAMESPACE,
                        KafkaUser.Fields.CREATION_TIMESTAMP,
                    },
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(
                    description = FieldFilter.FIELDS_DESCR,
                    explode = Explode.FALSE,
                    schema = @Schema(
                            type = SchemaType.ARRAY,
                            implementation = String.class,
                            enumeration = {
                                KafkaUser.Fields.NAME,
                                KafkaUser.Fields.NAMESPACE,
                                KafkaUser.Fields.CREATION_TIMESTAMP,
                            }))
            List<String> fields,

            @BeanParam
            @Valid
            ListFetchParams listParams,

            @BeanParam
            @Valid
            KafkaUserFilterParams filters) {

        fieldFilter.setTypedFields(Map.of(KafkaUser.FIELDS_PARAM, fields));

        ListRequestContext<KafkaUser> listSupport = new ListRequestContext<>(
                filters,
                KafkaUser.Fields.COMPARATOR_BUILDER,
                uriInfo.getRequestUri(),
                listParams,
                KafkaUser::fromCursor);

        return userService.listUsers(listSupport)
            .thenApply(userList -> new KafkaUser.DataList(userList, listSupport))
            .thenApply(Response::ok)
            .thenApply(ResponseBuilder::build);
    }

    @Path("{userId}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponseSchema(KafkaUser.Data.class)
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    @Authorized
    @ResourcePrivilege(Privilege.GET)
    public CompletionStage<Response> describeUser(
            @Parameter(description = "Cluster identifier")
            @PathParam("clusterId")
            String clusterId,

            @PathParam("userId")
            @Parameter(description = "User identifier")
            String userId,

            @QueryParam(KafkaUser.FIELDS_PARAM)
            @DefaultValue(KafkaUser.Fields.DESCRIBE_DEFAULT)
            @StringEnumeration(
                    source = KafkaUser.FIELDS_PARAM,
                    allowedValues = {
                        KafkaUser.Fields.NAME,
                        KafkaUser.Fields.NAMESPACE,
                        KafkaUser.Fields.CREATION_TIMESTAMP,
                    },
                    payload = ErrorCategory.InvalidQueryParameter.class)
            @Parameter(
                    description = FieldFilter.FIELDS_DESCR,
                    explode = Explode.FALSE,
                    schema = @Schema(
                            type = SchemaType.ARRAY,
                            implementation = String.class,
                            enumeration = {
                                KafkaUser.Fields.NAME,
                                KafkaUser.Fields.NAMESPACE,
                                KafkaUser.Fields.CREATION_TIMESTAMP,
                            }))
            List<String> fields) {

        fieldFilter.setTypedFields(Map.of(KafkaUser.FIELDS_PARAM, fields));

        return userService.describeUser(userId)
            .thenApply(KafkaUser.Data::new)
            .thenApply(Response::ok)
            .thenApply(ResponseBuilder::build);
    }
}
