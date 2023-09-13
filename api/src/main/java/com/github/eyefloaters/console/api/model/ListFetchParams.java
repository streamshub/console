package com.github.eyefloaters.console.api.model;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.support.ErrorCategory;

import io.xlate.validation.constraints.Expression;
import io.xlate.validation.constraints.Expression.ExceptionalValue;

@Expression(
    when = "self.rawPageAfter != null",
    value = "self.isValidCursor(self.pageAfter)",
    message = "Parameter value missing or invalid",
    payload = ErrorCategory.InvalidQueryParameter.class,
    node = "page[after]")
@Expression(
    when = "self.rawPageBefore != null",
    value = "self.isValidCursor(self.pageBefore)",
    message = "Parameter value missing or invalid",
    payload = ErrorCategory.InvalidQueryParameter.class,
    node = "page[before]")
public class ListFetchParams {

    public static final int PAGE_SIZE_DEFAULT = 10;
    public static final int PAGE_SIZE_MAX = 1000;

    private static final Logger LOGGER = Logger.getLogger(ListFetchParams.class);
    /**
     * Pattern to split dot-separated sort keys. This pattern allows for
     * segments to be quoted for cases when a segment itself includes the dot (.)
     * character.
     *
     * E.g. {@code configs."retention.ms"} would be split to {@code configs} and
     * {@code "retention.ms"} (quotes removed separately).
     */
    private static final Pattern PATH_PATTERN = Pattern.compile("\\.(?=(?:[^\"]*+\"[^\"]*+\")*+[^\"]*$)");

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
    @Parameter(
        description = """
            Limit the number of records fetched and returned. When omitted,
            a default page size will be used by the server unless the client
            has included both `page[after]` and `page[before]` parameters
            (a range pagination request).

            In the case of a range pagination request, up to the maximum number
            of records will be returned in the page. If the number of matching
            records exceeds the page size, the server response will include a meta
            entry `/meta/page/rangeTruncated` with a value of `true`.
            """,
        schema = @Schema(
            implementation = Integer.class,
            minimum = "1",
            maximum = PAGE_SIZE_MAX + "",
            defaultValue = PAGE_SIZE_DEFAULT + ""))
    @Expression(
        when = "self != null",
        value = "Integer.parseInt(self) >= 1",
        exceptionalValue = ExceptionalValue.FALSE,
        message = "must be a positive integer",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = "page[size]")
    @Expression(
        when = "self != null",
        value = "Integer.parseInt(self) <= " + PAGE_SIZE_MAX,
        exceptionalValue = ExceptionalValue.FALSE,
        message = "requested page[size] exceeds maximum of " + PAGE_SIZE_MAX,
        payload = ErrorCategory.MaxPageSizeExceededError.class,
        node = "page[size]")
    String pageSize;

    @QueryParam("page[after]")
    @Parameter(name = "page[after]",
        in = ParameterIn.QUERY,
        description = """
            Cursor used to request a page where the first item returned in the paginated
            data is the item that is closest to, but still after, the cursor if
            it were included in the un-paginated results list.

            If there are no items in the results list that fall after the cursor, the
            returned paginated data will be an empty array.

            May be used together with `page[before]` to form a range pagination request.
            """)
    String pageAfter;

    @QueryParam("page[before]")
    @Parameter(name = "page[before]",
        in = ParameterIn.QUERY,
        description = """
            Cursor used to request a page where the last item returned in the paginated
            data is the item that is closest to, but still before, the cursor if
            it were included in the un-paginated results list.

            If there are no items in the results list that fall before the cursor, the
            returned paginated data will be an empty array.

            May be used together with `page[after]` to form a range pagination request.
            """)
    String pageBefore;

    List<String> sortEntries;
    List<String> sortNames;
    JsonObject pageAfterParsed;
    JsonObject pageBeforeParsed;

    public String getRawPageAfter() {
        return pageAfter;
    }

    public String getRawPageBefore() {
        return pageBefore;
    }

    public List<String> getSortEntries() {
        if (sortEntries == null) {
            sortEntries = Optional.ofNullable(sort)
                .map(s -> s.split(","))
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(String::trim)
                .toList();
        }

        return sortEntries;
    }

    public List<String> getSortNames() {
        if (sortNames == null) {
            sortNames = getSortEntries()
                .stream()
                .map(name -> name.startsWith("-") ? name.substring(1) : name)
                .toList();
        }

        return sortNames;
    }

    public Integer getPageSize() {
        Integer size = parse(pageSize, Integer::parseInt);

        if (size == null) {
            if (getPageAfter() != null && getPageBefore() != null) {
                size = PAGE_SIZE_MAX;
            } else {
                size = PAGE_SIZE_DEFAULT;
            }
        }

        return size;
    }

    public JsonObject getPageAfter() {
        if (pageAfterParsed == null) {
            pageAfterParsed = parseJson(pageAfter);
        }

        return pageAfterParsed;
    }

    public JsonObject getPageBefore() {
        if (pageBeforeParsed == null) {
            pageBeforeParsed = parseJson(pageBefore);
        }

        return pageBeforeParsed;
    }

    /**
     * Verify that the given cursor is valid:
     * <ul>
     * <li>The cursor was successfully parsed (not null)
     * <li>The attributes present in the cursor exactly match the fields requested
     * for the sort operation, both implicitly and explicitly (i.e. the ID is always
     * the final sort field).
     * </ul>
     *
     * This method called once for each `page[after]` and `page[before]` query
     * parameter present in the request.
     *
     * @param cursor one of the request cursors.
     * @return true if the cursor was successfully parsed and is valid vis-à-vis the
     *         `sort` query parameter parameter, else false.
     */
    public boolean isValidCursor(JsonObject cursor) {
        if (cursor == null) {
            return false;
        }

        /*
         * Build a list that includes both the implicit (i.e. `id`) and
         * explicit sort fields for the request. Each field is mapped to
         * JSON Pointer format (RFC 6901) for comparison with a similar
         * list of pointers for all leaf entries in the cursor passed to
         * this method.
         */
        List<String> sortPointers = Stream.concat(Stream.of("/id"), getSortNames()
                .stream()
                .filter(Predicate.not("id"::equals))
                .map(name -> String.join("/", pathElements(name)))
                .map("/attributes/"::concat))
                .sorted()
                .distinct()
                .toList();

        List<String> cursorPointers = new ArrayList<>();
        appendPointers(cursorPointers, "", cursor);
        Collections.sort(cursorPointers);

        return sortPointers.equals(cursorPointers);
    }

    static JsonObject parseJson(String value) {
        return parse(value, val -> {
            byte[] decoded;

            try {
                decoded = Base64.getUrlDecoder().decode(val);
            } catch (IllegalArgumentException e) {
                LOGGER.debugf(e, "Failed to decode base64 value: '%s'", value);
                return null;
            }

            try (var reader = Json.createReader(new ByteArrayInputStream(decoded))) {
                return reader.readObject();
            } catch (JsonException e) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debugf(e, "Failed to parse JSON: '%s'", new String(decoded));
                }
            }

            return null;
        });
    }

    static <R, T> T parse(R value, Function<R, T> parser) {
        return value != null ? parser.apply(value) : null;
    }

    static List<String> pathElements(String key) {
        return Arrays.stream(PATH_PATTERN.split(key, 20))
            .map(ListFetchParams::stripQuotes)
            .map(Json::encodePointer)
            .toList();
    }

    static String stripQuotes(String value) {
        if (value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    static void appendPointers(List<String> pointers, String pointer, JsonValue value) {
        switch (value.getValueType()) {
            case OBJECT:
                value.asJsonObject().forEach((k, v) ->
                    appendPointers(pointers, pointer + "/" + Json.encodePointer(k), v));
                break;

            case ARRAY:
                int index = 0;

                for (var entry : value.asJsonArray()) {
                    appendPointers(pointers, pointer + "/" + (index++), entry);
                }
                break;

            default:
                // save the pointer of any terminal node, value does not matter
                pointers.add(pointer);
                break;
        }
    }
}
