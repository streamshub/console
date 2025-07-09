package com.github.streamshub.console.api.model;

import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.github.streamshub.console.api.support.ErrorCategory;

import io.xlate.validation.constraints.Expression;

public class KafkaRebalanceFilterParams extends FilterParams {

    @QueryParam("filter[name]")
    @Parameter(
        description = "Retrieve only rebalances with a name matching this parameter",
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

    @QueryParam("filter[status]")
    @Parameter(
        description = "Retrieve only rebalances matching the status identified by this parameter",
        schema = @Schema(implementation = String[].class, minItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "self.operator == 'eq' || self.operator == 'in'",
        message = "unsupported filter operator, supported values: [ 'eq', 'in' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[status]")
    @Expression(
        when = "self != null",
        value = "self.operands.size() >= 1",
        message = "at least 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[status]")
    FetchFilter statusFilter;

    @QueryParam("filter[mode]")
    @Parameter(
        description = "Retrieve only rebalances matching the mode identified by this parameter",
        schema = @Schema(implementation = String[].class, minItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "self.operator == 'eq' || self.operator == 'in'",
        message = "unsupported filter operator, supported values: [ 'eq', 'in' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[mode]")
    @Expression(
        when = "self != null",
        value = "self.operands.size() >= 1",
        message = "at least 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[mode]")
    FetchFilter modeFilter;

    @Override
    protected void buildPredicates() {
        maybeAddPredicate(nameFilter, KafkaRebalance.class, KafkaRebalance::name);
        maybeAddPredicate(statusFilter, KafkaRebalance.class, KafkaRebalance::status);
        maybeAddPredicate(modeFilter, KafkaRebalance.class, KafkaRebalance::mode);
    }
}
