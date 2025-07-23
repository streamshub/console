package com.github.streamshub.console.api.model.connect;

import jakarta.inject.Inject;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.github.streamshub.console.api.model.FetchFilter;
import com.github.streamshub.console.api.model.FilterParams;
import com.github.streamshub.console.api.service.KafkaConnectService;
import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.config.KafkaConnectConfig;

import io.xlate.validation.constraints.Expression;

public class ConnectClusterFilterParams extends FilterParams {

    @Inject
    private KafkaConnectService connectService;

    @QueryParam("filter[name]")
    @Parameter(
        description = "Retrieve only Kafka Connect clusters with a name matching this parameter",
        schema = @Schema(implementation = String[].class, minItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "self.operator == 'eq' || self.operator == 'in' || self.operator == 'like'",
        message = "unsupported filter operator, supported values: [ 'eq', 'in', 'like' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[name]")
    @Expression(
        when = "self != null",
        value = "self.operands.size() >= 1",
        message = "at least 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[name]")
    FetchFilter nameFilter;

    @QueryParam("filter[kafkaClusters]")
    @Parameter(
        description = "Retrieve only connect clusters associated with the identified Kafka clusters",
        schema = @Schema(implementation = String[].class, minItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "self.operator == 'contains' || self.operator == 'in'",
        message = "unsupported filter operator, supported values: [ 'contains', 'in' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[kafkaClusters]")
    @Expression(
        when = "self != null",
        value = "self.operands.size() >= 1",
        message = "at least 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[kafkaClusters]")
    FetchFilter kafkaClusterFilter;

    @Override
    protected void buildPredicates() {
        maybeAddPredicate(nameFilter, ConnectCluster.class, ConnectCluster::name);
        maybeAddPredicate(kafkaClusterFilter, KafkaConnectConfig.class, connectService::mapKafkaIdentifiers);
    }
}
