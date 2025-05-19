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

public class KafkaClusterFilterParams {

    @QueryParam("filter[name]")
    @Parameter(
        description = "Retrieve only kafka clusters with a name matching this parameter",
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

    public List<Predicate<KafkaCluster>> buildPredicates() {
        List<Predicate<KafkaCluster>> predicates = new ArrayList<>();
        
        if (nameFilter != null) {
            predicates.add(new FetchFilterPredicate<>(nameFilter, KafkaCluster::name));
        }

        return predicates;
    }
}
