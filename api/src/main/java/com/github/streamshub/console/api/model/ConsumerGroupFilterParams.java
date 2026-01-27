package com.github.streamshub.console.api.model;

import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.github.streamshub.console.api.support.ErrorCategory;

import io.xlate.validation.constraints.Expression;
import io.xlate.validation.constraints.Expression.ExceptionalValue;

public class ConsumerGroupFilterParams extends FilterParams {

    @QueryParam("filter[id]")
    @Parameter(
        description = "Retrieve only consumer groups with an ID matching this parameter",
        schema = @Schema(implementation = String[].class, minItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "self.operator == 'eq' || self.operator == 'in' || self.operator == 'like'",
        message = "unsupported filter operator, supported values: [ 'eq', 'in', 'like' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[id]")
    @Expression(
        when = "self != null",
        value = "self.operands.size() >= 1",
        message = "at least 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[id]")
    FetchFilter idFilter;

    @QueryParam("filter[state]")
    @Parameter(
        description = "Retrieve only consumer groups matching the state identified by this parameter",
        schema = @Schema(implementation = String[].class, minItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "self.operator == 'eq' || self.operator == 'in'",
        message = "unsupported filter operator, supported values: [ 'eq', 'in' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[state]")
    @Expression(
        when = "self != null",
        value = "self.operands.size() >= 1",
        message = "at least 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[state]")
    @Expression(
        when = "self != null && self.operands.size() >= 1",
        classImports = "org.apache.kafka.common.GroupState",
        value = """
                self.operands.stream()
                    .map(state -> GroupState.parse(state))
                    .noneMatch(state -> state == GroupState.UNKNOWN)
                """,
        exceptionalValue = ExceptionalValue.UNSET,
        message = "operands list contains an invalid consumer group state",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[state]")
    FetchFilter stateFilter;

    @Override
    protected void buildPredicates() {
        maybeAddPredicate(idFilter, ConsumerGroup.class, ConsumerGroup::groupId);
        maybeAddPredicate("filter[state]", stateFilter, ConsumerGroup.class, ConsumerGroup::state);
    }
}
