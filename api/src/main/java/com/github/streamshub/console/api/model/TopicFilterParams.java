package com.github.streamshub.console.api.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.FetchFilterPredicate;

import io.xlate.validation.constraints.Expression;

public class TopicFilterParams {

    @QueryParam("filter[id]")
    @Parameter(
        description = "Retrieve only topics with an ID matching this parameter",
        schema = @Schema(implementation = String[].class, minItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "self.operator == 'eq' || self.operator == 'in'",
        message = "unsupported filter operator, supported values: [ 'eq', 'in' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[id]")
    @Expression(
        when = "self != null",
        value = "self.operands.size() >= 1",
        message = "at least 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[id]")
    FetchFilter idFilter;

    @QueryParam("filter[name]")
    @Parameter(
        description = "Retrieve only topics with a name matching this parameter",
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

    @QueryParam("filter[visibility]")
    @DefaultValue("eq,external")
    @Parameter(
        description = "Retrieve only topics matching the visibility identified by this parameter",
        schema = @Schema(implementation = String[].class, minItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "self.operator == 'eq' || self.operator == 'in'",
        message = "unsupported filter operator, supported values: [ 'eq', 'in' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[visibility]")
    @Expression(
        when = "self != null",
        value = "self.operands.size() >= 1",
        message = "at least 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[visibility]")
    FetchFilter visibilityFilter;

    @QueryParam("filter[status]")
    @Parameter(
        description = "Retrieve only topics matching the status identified by this parameter",
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

    public List<Predicate<Topic>> buildPredicates() {
        List<Predicate<Topic>> predicates = new ArrayList<>(3);

        predicates.add(new FetchFilterPredicate<>(visibilityFilter, Topic::visibility));

        if (nameFilter != null) {
            predicates.add(new FetchFilterPredicate<>(nameFilter, Topic::name));
        }

        if (idFilter != null) {
            predicates.add(new FetchFilterPredicate<>(idFilter, Topic::getId));
        }

        if (statusFilter != null) {
            predicates.add(new FetchFilterPredicate<>(statusFilter, Topic::status));
        }

        return predicates;
    }

}
