package com.github.eyefloaters.console.api.model;

import java.time.Instant;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

import com.github.eyefloaters.console.legacy.model.Types.Record;

public class RecordFilterParams {

    public static final String PROP_LIMIT = "limit";
    public static final String PROP_MAX_VALUE_LENGTH = "maxValueLength";

    @QueryParam(Record.PROP_PARTITION)
    @Parameter(description = "Retrieve messages only from this partition")
    Integer partition;

    @QueryParam(Record.PROP_OFFSET)
    @Parameter(description = "Retrieve messages with an offset equal to or greater than this offset. If both `timestamp` and `offset` are requested, `timestamp` is given preference.")
    @Min(0)
    Integer offset;

    @QueryParam(Record.PROP_TIMESTAMP)
    @Parameter(
        description = "Retrieve messages with a timestamp equal to or later than this timestamp. If both `timestamp` and `offset` are requested, `timestamp` is given preference.",
        schema = @Schema(format = "date-time"))
    String timestamp;

    @QueryParam(PROP_LIMIT)
    @DefaultValue("20")
    @Parameter(description = "Limit the number of records fetched and returned")
    @Positive
    Integer limit;

    @QueryParam(PROP_MAX_VALUE_LENGTH)
    @Parameter(description = "Maximum length of string values returned in the response. "
            + "Values with a length that exceeds this parameter will be truncated. When this parameter is not "
            + "included in the request, the full string values will be returned.")
    @Positive
    Integer maxValueLength;

    @AssertTrue(message = "invalid timestamp")
    public boolean isTimestampValid() {
        if (timestamp == null) {
            return true;
        }

        try {
            return Instant.parse(timestamp).isAfter(Instant.ofEpochMilli(-1));
        } catch (Exception e) {
            return false;
        }
    }

    public Integer getPartition() {
        return partition;
    }

    public void setPartition(Integer partition) {
        this.partition = partition;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(Integer offset) {
        this.offset = offset;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getLimit() {
        return limit;
    }

    public void setLimit(Integer limit) {
        this.limit = limit;
    }

    public Integer getMaxValueLength() {
        return maxValueLength;
    }

    public void setMaxValueLength(Integer maxValueLength) {
        this.maxValueLength = maxValueLength;
    }
}
