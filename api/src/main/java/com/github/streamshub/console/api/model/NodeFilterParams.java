package com.github.streamshub.console.api.model;

import java.util.Optional;
import java.util.function.Predicate;

import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.FetchFilterPredicate;

import io.xlate.validation.constraints.Expression;

public class NodeFilterParams extends FilterParams {

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

    @QueryParam("filter[broker.status]")
    @Parameter(
        description = "Retrieve only brokers matching the status identified by this parameter",
        schema = @Schema(implementation = String[].class, minItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "self.operator == 'eq' || self.operator == 'in'",
        message = "unsupported filter operator, supported values: [ 'eq', 'in' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[broker.status]")
    @Expression(
        when = "self != null",
        value = "self.operands.size() >= 1",
        message = "at least 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[broker.status]")
    FetchFilter brokerStatusFilter;

    @QueryParam("filter[controller.status]")
    @Parameter(
        description = "Retrieve only controllers matching the status identified by this parameter",
        schema = @Schema(implementation = String[].class, minItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "self.operator == 'eq' || self.operator == 'in'",
        message = "unsupported filter operator, supported values: [ 'eq', 'in' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[controller.status]")
    @Expression(
        when = "self != null",
        value = "self.operands.size() >= 1",
        message = "at least 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[controller.status]")
    FetchFilter controllerStatusFilter;

    @Override
    protected void buildPredicates() {
        maybeAddPredicate(nodePoolFilter, Node.class, Node::nodePool);
        maybeAddPredicate(roleFilter, Node.class, Node.Role::fromValue, Node::roles);
        maybeAddStatusFilters();
    }

    private void maybeAddStatusFilters() {
        Predicate<Node> brokerPredicate = brokerStatusFilter != null
                ? new FetchFilterPredicate<>(
                        brokerStatusFilter,
                        Node.BrokerStatus::fromValue,
                        n -> Optional.ofNullable(n.broker())
                            .map(Node.Attributes.Broker::status)
                            .orElse(null))
                : null;
        Predicate<Node> controllerPredicate = controllerStatusFilter != null
                ? new FetchFilterPredicate<>(
                        controllerStatusFilter,
                        Node.ControllerStatus::fromValue,
                        n -> Optional.ofNullable(n.controller())
                            .map(Node.Attributes.Controller::status)
                            .orElse(null))
                : null;

        if (brokerPredicate != null && controllerPredicate != null) {
            addPredicate(Node.class, brokerPredicate.or(controllerPredicate));
        } else if (brokerPredicate != null) {
            addPredicate(Node.class, brokerPredicate);
        } else if (controllerPredicate != null) {
            addPredicate(Node.class, controllerPredicate);
        }
    }
}
