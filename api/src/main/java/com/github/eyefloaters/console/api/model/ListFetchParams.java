package com.github.eyefloaters.console.api.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.github.eyefloaters.console.api.support.ErrorCategory;

import io.xlate.validation.constraints.Expression;
import io.xlate.validation.constraints.Expression.ExceptionalValue;

public class ListFetchParams {

    @QueryParam("sort")
    @Parameter(name = "sort",
        in = ParameterIn.QUERY,
        explode = Explode.FALSE,
        schema = @Schema(implementation = String[].class),
        description = """
            Comma-separated list of fields by which the result set will be ordered.
            The sort order for each sort field will be ascending unless it is prefixed
            with a minus (U+002D HYPHEN-MINUS, "-"), in which case it will be descending.

            Unrecognized field names or fields of type `object` or `array` will be
            ignored.
            """)
    String sort;

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

    @QueryParam("page[before]")
    String pageBefore;

    @QueryParam("page[after]")
    String pageAfter;

    public String getSort() {
        return sort;
    }

    public List<String> getSortNames() {
        return Optional.ofNullable(sort)
            .map(s -> s.split(","))
            .map(Arrays::stream)
            .orElseGet(Stream::empty)
            .map(name -> name.startsWith("-") ? name.substring(1) : name)
            .toList();
    }

    public String getRawPageAfter() {
        return pageAfter;
    }

    public String getRawPageBefore() {
        return pageAfter;
    }

    public Integer getPageSize() {
        return parse(pageSize, Integer::parseInt);
    }

    public JsonObject getPageAfter() {
        return parseJson(pageAfter);
    }

    public JsonObject getPageBefore() {
        return parseJson(pageBefore);
    }

    static JsonObject parseJson(String value) {
        return parse(value, val -> {
            byte[] decoded = Base64.getUrlDecoder().decode(val);

            try (var stream = new ByteArrayInputStream(decoded);
                 var parser = Json.createParser(stream)) {
                parser.next();
                return parser.getObject();
            } catch (IOException e) {
                // XXX throw custom exception and return 400 Bad Request
                throw new UncheckedIOException(e);
            }
        });
    }

    static <R, T> T parse(R value, Function<R, T> parser) {
        return value != null ? parser.apply(value) : null;
    }
}
