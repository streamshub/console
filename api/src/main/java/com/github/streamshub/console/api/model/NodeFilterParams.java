package com.github.streamshub.console.api.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.FetchFilterPredicate;

import io.xlate.validation.constraints.Expression;

public class NodeFilterParams {

    @QueryParam("filter[nodePool]")
    @Parameter(
        description = "Retrieve only nodes in a matching node pool",
        schema = @Schema(implementation = String[].class, minItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "self.operator == 'in'",
        message = "unsupported filter operator, supported values: [ 'in' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[nodePool]")
    @Expression(
        when = "self != null",
        value = "self.operands.size() >= 1",
        message = "at least 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[nodePool]")
    FetchFilter nodePoolFilter;

    @QueryParam("filter[roles]")
    @Parameter(
        description = "Retrieve only nodes that have one of the provided roles",
        schema = @Schema(implementation = String[].class, minItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "self.operator == 'in'",
        message = "unsupported filter operator, supported values: [ 'in' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[roles]")
    @Expression(
        when = "self != null",
        value = "self.operands.size() >= 1",
        message = "at least 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[roles]")
    FetchFilter roleFilter;

    @QueryParam("filter[status]")
    @Parameter(
        description = "Retrieve only nodes matching the status identified by this parameter",
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

    public List<Predicate<Node>> buildPredicates() {
        List<Predicate<Node>> predicates = new ArrayList<>(3);

        if (nodePoolFilter != null) {
            predicates.add(new FetchFilterPredicate<>(nodePoolFilter, Node::nodePool));
        }

        if (roleFilter != null) {
            predicates.add(new FetchFilterPredicate<>(roleFilter, Node.Role::fromValue, Node::roles));
        }

        if (statusFilter != null) {
            predicates.add(new FetchFilterPredicate<>(statusFilter, Node::status));
        }

        return predicates;
    }

}
