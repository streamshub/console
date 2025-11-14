package com.github.streamshub.console.api.model;

import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.github.streamshub.console.api.support.ErrorCategory;

import io.xlate.validation.constraints.Expression;

public class KafkaUserFilterParams extends FilterParams {

    @QueryParam("filter[username]")
    @Parameter(
        description = "Retrieve only Kafka users with a username matching this parameter",
        schema = @Schema(implementation = String[].class, minItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "self.operator == 'eq' || self.operator == 'in' || self.operator == 'like'",
        message = "unsupported filter operator, supported values: [ 'eq', 'in', 'like' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[username]")
    @Expression(
        when = "self != null",
        value = "self.operands.size() >= 1",
        message = "at least 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[username]")
    FetchFilter usernameFilter;

    @Override
    protected void buildPredicates() {
        maybeAddPredicate(usernameFilter, KafkaUser.class, KafkaUser::username);
    }
}
