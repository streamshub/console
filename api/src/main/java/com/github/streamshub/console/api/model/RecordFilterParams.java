package com.github.streamshub.console.api.model;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.StreamSupport;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.github.streamshub.console.api.support.ErrorCategory;
import com.github.streamshub.console.api.support.serdes.RecordData;

import io.xlate.validation.constraints.Expression;
import io.xlate.validation.constraints.Expression.ExceptionalValue;

@Expression(
    when = "self.rawTimestamp != null",
    value = "self.rawOffset == null",
    node = RecordFilterParams.FILTER_OFFSET,
    message = "Parameter `filter[offset]` must not be used when `filter[timestamp]` is present.",
    payload = ErrorCategory.InvalidQueryParameter.class)
public class RecordFilterParams extends FilterParams {

    static final String FILTER_PARTITION = "filter[partition]";
    static final String FILTER_OFFSET = "filter[offset]";
    static final String FILTER_TIMESTAMP = "filter[timestamp]";
    static final String FILTER_KEY = "filter[key]";
    static final String FILTER_VALUE = "filter[value]";
    static final String FILTER_HEADERS = "filter[headers]";
    static final String PAGE_SIZE = "page[size]";
    static final String MAX_VALUE_LENGTH = "maxValueLength";

    @QueryParam(FILTER_PARTITION)
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
        node = FILTER_PARTITION)
    @Expression(
        when = "self != null",
        value = "self.operands.size() == 1",
        message = "exactly 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = FILTER_PARTITION)
    @Expression(
        when = "self != null && self.operator == 'eq' && self.operands.size() == 1",
        value = "val = Integer.parseInt(self.firstOperand); val >= 0 && val <= Integer.MAX_VALUE",
        exceptionalValue = ExceptionalValue.FALSE,
        message = "operand must be an integer between 0 and " + Integer.MAX_VALUE + ", inclusive",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = FILTER_PARTITION)
    FetchFilter partition;

    @QueryParam(FILTER_OFFSET)
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
        node = FILTER_OFFSET)
    @Expression(
        when = "self != null",
        value = "self.operands.size() == 1",
        message = "exactly 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = FILTER_OFFSET)
    @Expression(
        when = "self != null && self.operator == 'gte'",
        value = "val = Long.parseLong(self.firstOperand); val >= 0 && val <= Long.MAX_VALUE",
        exceptionalValue = ExceptionalValue.FALSE,
        message = "operand must be an integer between 0 and " + Long.MAX_VALUE + ", inclusive",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = FILTER_OFFSET)
    FetchFilter offset;

    @QueryParam(FILTER_TIMESTAMP)
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
        node = FILTER_TIMESTAMP)
    @Expression(
        when = "self != null",
        value = "self.operands.size() == 1",
        message = "exactly 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = FILTER_TIMESTAMP)
    @Expression(
        when = "self != null && self.operator == 'gte'",
        classImports = "java.time.Instant",
        value = "Instant.parse(self.firstOperand) >= Instant.EPOCH",
        exceptionalValue = ExceptionalValue.FALSE,
        message = "operand must be a valid RFC 3339 date-time no earlier than `1970-01-01T00:00:00Z`",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = FILTER_TIMESTAMP)
    FetchFilter timestamp;

    @QueryParam(FILTER_KEY)
    @Parameter(
        description = """
            Retrieve only messages whose key matches this filter. The format of this
            parameter's value is `like,<pattern>` where `*` matches any sequence of
            characters and `?` matches any single character. Binary keys are never matched.
            """,
        schema = @Schema(implementation = String[].class, minItems = 2, maxItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "[ 'like', 'ilike' ].contains(self.operator)",
        message = "unsupported filter operator, supported values: [ 'like', 'ilike' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = FILTER_KEY)
    @Expression(
        when = "self != null",
        value = "self.operands.size() == 1",
        message = "exactly 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = FILTER_KEY)
    FetchFilter keyFilter;

    @QueryParam(FILTER_VALUE)
    @Parameter(
        description = """
            Retrieve only messages whose value matches this filter. The format of this
            parameter's value is `like,<pattern>` where `*` matches any sequence of
            characters and `?` matches any single character. Binary values are never matched.
            """,
        schema = @Schema(implementation = String[].class, minItems = 2, maxItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "[ 'like', 'ilike' ].contains(self.operator)",
        message = "unsupported filter operator, supported values: [ 'like', 'ilike' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = FILTER_VALUE)
    @Expression(
        when = "self != null",
        value = "self.operands.size() == 1",
        message = "exactly 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = FILTER_VALUE)
    FetchFilter valueFilter;

    @QueryParam(FILTER_HEADERS)
    @Parameter(
        description = """
            Retrieve only messages with at least one header value matching this filter.
            The format of this parameter's value is `like,<pattern>` where `*` matches
            any sequence of characters and `?` matches any single character. Binary header
            values are never matched.
            """,
        schema = @Schema(implementation = String[].class, minItems = 2, maxItems = 2),
        explode = Explode.FALSE)
    @Expression(
        when = "self != null",
        value = "[ 'like', 'ilike' ].contains(self.operator)",
        message = "unsupported filter operator, supported values: [ 'like', 'ilike' ]",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = FILTER_HEADERS)
    @Expression(
        when = "self != null",
        value = "self.operands.size() == 1",
        message = "exactly 1 operand is required",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = FILTER_HEADERS)
    FetchFilter headersFilter;

    @QueryParam(PAGE_SIZE)
    @DefaultValue(ListFetchParams.PAGE_SIZE_DEFAULT + "")
    @Parameter(
        description = "Limit the number of records fetched and returned",
        schema = @Schema(
                implementation = Integer.class,
                minimum = "1",
                maximum = ListFetchParams.PAGE_SIZE_MAX + "",
                defaultValue = ListFetchParams.PAGE_SIZE_DEFAULT + ""))
    @Expression(
        when = "self != null",
        value = "val = Integer.parseInt(self); val >= 1 && val <= " + ListFetchParams.PAGE_SIZE_MAX,
        exceptionalValue = ExceptionalValue.FALSE,
        message = "must be an integer between 1 and " + ListFetchParams.PAGE_SIZE_MAX + ", inclusive",
        payload = ErrorCategory.InvalidQueryParameter.class,
        node = PAGE_SIZE)
    String pageSize;

    @QueryParam(MAX_VALUE_LENGTH)
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
        node = MAX_VALUE_LENGTH)
    String maxValueLength;

    /**
     * Attempts to decode {@code bytes} as UTF-8 text. Returns {@link Optional#empty()}
     * when the bytes are {@code null}, empty, contain a malformed UTF-8 sequence, or
     * contain control characters that indicate binary content.
     */
    public static Optional<String> decodeText(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return Optional.empty();
        }

        StringBuilder sb = new StringBuilder();

        try (Reader reader = new InputStreamReader(
                new ByteArrayInputStream(bytes),
                StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT))) {
            int cp;

            while ((cp = reader.read()) != -1) {
                if (isExcludedUnicodeC0(cp) || isUnicodeC1(cp)) {
                    return Optional.empty();
                }

                sb.appendCodePoint(cp);
            }
        } catch (IOException e) {
            return Optional.empty();
        }

        return Optional.of(sb.toString());
    }

    /** Returns true for C0 control codes except common text whitespace (tab, LF, CR). */
    public static boolean isExcludedUnicodeC0(int cp) {
        return cp <= 0x1F && cp != '\t' && cp != '\n' && cp != '\r';
    }

    /** Returns true for DEL and C1 control codes (0x7F–0x9F). */
    public static boolean isUnicodeC1(int cp) {
        return cp >= 0x7F && cp <= 0x9F;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void buildPredicates() {
        Class<ConsumerRecord<RecordData, RecordData>> recordClass =
            (Class<ConsumerRecord<RecordData, RecordData>>) (Class<?>) ConsumerRecord.class;

        maybeAddPredicate(keyFilter, recordClass,
            rec -> decodeText(rec.key() != null ? rec.key().bytes() : null).orElse(null));
        maybeAddPredicate(valueFilter, recordClass,
            rec -> decodeText(rec.value() != null ? rec.value().bytes() : null).orElse(null));
        maybeAddPredicate(headersFilter, recordClass,
            rec -> StreamSupport.stream(rec.headers().spliterator(), false)
                .map(Header::value)
                .map(RecordFilterParams::decodeText)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .reduce((a, b) -> a + "\u0000" + b)
                .orElse(null));
    }

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
