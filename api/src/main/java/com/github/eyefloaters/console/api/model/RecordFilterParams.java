package com.github.eyefloaters.console.api.model;

import java.time.Instant;
import java.util.function.Function;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.github.eyefloaters.console.api.support.ErrorCategory;

import io.xlate.validation.constraints.Expression;
import io.xlate.validation.constraints.Expression.ExceptionalValue;

@Expression(
    when = "self.rawTimestamp != null",
    value = "self.rawOffset == null",
    node = "filter[offset]",
    message = "Parameter `filter[offset]` must not be used when `filter[timestamp]` is present.",
    payload = ErrorCategory.InvalidQueryParameter.class)
public class RecordFilterParams {

    @QueryParam("filter[partition]")
    @Parameter(
        description = """
                Retrieve messages only from the partition identified by this parameter.

                Clients may optionally provide a two-item array where the first entry
                is the operator `eq`, and the second item is the partition identifier.
                """,
        schema = @Schema(implementation = String[].class, minItems = 1, maxItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "self.operator == 'eq'",
        message = "unsupported filter operator, supported values: [ 'eq' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[partition]")
    @Expression(
        when = "self != null",
        value = "self.operands.size() == 1",
        message = "exactly 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[partition]")
    @Expression(
        when = "self != null && self.operator == 'eq' && self.operands.size() == 1",
        value = "val = Integer.parseInt(self.firstOperand); val >= 0 && val <= Integer.MAX_VALUE",
        exceptionalValue = ExceptionalValue.FALSE,
        message = "operand must be an integer between 0 and " + Integer.MAX_VALUE + ", inclusive",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[partition]")
    FetchFilter partition;

    @QueryParam("filter[offset]")
    @Parameter(
        description = """
        Retrieve messages with an offset greater than or equal to the filter
        offset. The format of this parameter's value is `[ <operator>,<operand> ]`
        where the only supported operator is `gte` and the operand must be an
        integer between 0 and 2<sup>64</sup>-1.

        This parameter and `filter[timestamp]` are mutually exclusive and may
        not be used in the same request.
        """,
        schema = @Schema(implementation = String[].class, minItems = 2, maxItems = 2),
        explode = Explode.FALSE,
        examples = {
            @ExampleObject(
                name = "As of offset 1001",
                summary = "Messages as of offset 1001",
                value = "[ \"gte\",\"1001\" ]")
        })
    @Expression(
        when = "self != null",
        value = "self.operator == 'gte'",
        exceptionalValue = ExceptionalValue.FALSE,
        message = "unsupported filter operator, supported values: [ 'gte' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[offset]")
    @Expression(
        when = "self != null",
        value = "self.operands.size() == 1",
        message = "exactly 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[offset]")
    @Expression(
        when = "self != null && self.operator == 'gte'",
        value = "val = Long.parseLong(self.firstOperand); val >= 0 && val <= Long.MAX_VALUE",
        exceptionalValue = ExceptionalValue.FALSE,
        message = "operand must be an integer between 0 and " + Long.MAX_VALUE + ", inclusive",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[offset]")
    FetchFilter offset;

    @QueryParam("filter[timestamp]")
    @Parameter(
        description = """
            Retrieve messages with a timestamp greater than or equal to the filter
            timestamp. The format of this parameter's value is `[ <operator>,<operand> ]`
            where the only supported operator is `gte` and the operand must be a
            valid RFC 3339 date-time not earlier than `1970-01-01T00:00:00Z`.

            This parameter and `filter[offset]` are mutually exclusive and may not be
            used in the same request.
            """,
        schema = @Schema(implementation = String[].class, minItems = 2, maxItems = 2),
        explode = Explode.FALSE,
        examples = {
            @ExampleObject(
                    name = "As of 2023-01-01",
                    summary = "Messages as of January 1, 2023",
                    value = "[ \"gte\",\"2023-01-01T00:00:00Z\" ]")
        })
    @Expression(
        when = "self != null",
        value = "self.operator == 'gte'",
        message = "unsupported filter operator, supported values: [ 'gte' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[timestamp]")
    @Expression(
        when = "self != null",
        value = "self.operands.size() == 1",
        message = "exactly 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[timestamp]")
    @Expression(
        when = "self != null && self.operator == 'gte'",
        classImports = "java.time.Instant",
        value = "Instant.parse(self.firstOperand) >= Instant.EPOCH",
        exceptionalValue = ExceptionalValue.FALSE,
        message = "operand must be a valid RFC 3339 date-time no earlier than `1970-01-01T00:00:00Z`",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "filter[timestamp]")
    FetchFilter timestamp;

    @QueryParam("page[size]")
    @DefaultValue("20")
    @Parameter(
        description = "Limit the number of records fetched and returned",
        schema = @Schema(implementation = Integer.class, minimum = "1"))
    @Expression(
        when = "self != null",
        value = "val = Integer.parseInt(self); val >= 1 && val <= Integer.MAX_VALUE",
        exceptionalValue = ExceptionalValue.FALSE,
        message = "must be an integer between 1 and " + Integer.MAX_VALUE + ", inclusive",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "page[size]")
    String pageSize;

    @QueryParam("maxValueLength")
    @Parameter(
        description = """
        Maximum length of string values returned in the response.
        Values with a length that exceeds this parameter will be truncated. When this parameter is not
        included in the request, the full string values will be returned.
        """,
        schema = @Schema(implementation = Integer.class, minimum = "1"))
    @Expression(
        when = "self != null",
        value = "val = Integer.parseInt(self); val >= 1 && val <= Integer.MAX_VALUE",
        exceptionalValue = ExceptionalValue.FALSE,
        message = "must be an integer between 1 and " + Integer.MAX_VALUE + ", inclusive",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "maxValueLength")
    String maxValueLength;

    public String getRawOffset() {
        return FetchFilter.rawFilter(offset);
    }

    public String getRawTimestamp() {
        return FetchFilter.rawFilter(timestamp);
    }

    public Integer getPartition() {
        return parse(partition, val -> Integer.parseInt(val.getFirstOperand()));
    }

    public Long getOffset() {
        return parse(offset, val -> Long.parseLong(val.getFirstOperand()));
    }

    public Instant getTimestamp() {
        return parse(timestamp, val -> Instant.parse(val.getFirstOperand()));
    }

    public Integer getLimit() {
        return parse(pageSize, Integer::parseInt);
    }

    public Integer getMaxValueLength() {
        return parse(maxValueLength, Integer::parseInt);
    }

    static <R, T> T parse(R value, Function<R, T> parser) {
        return value != null ? parser.apply(value) : null;
    }
}
