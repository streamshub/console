package com.github.streamshub.console.config;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.sundr.builder.annotations.Buildable;
import io.xlate.validation.constraints.Expression;

@Expression(
    message = "Either value or valueFrom must be specified, but not both",
    value = """
        (not empty self.value || not empty self.valueFrom)
        &&
        (self.value == null || self.valueFrom == null)""")
@Buildable
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Value {

    /**
     * Literal string to be used for this value
     */
    private String value; // NOSONAR

    /**
     * Reference to an external source file to use for this value
     */
    private String valueFrom;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getValueFrom() {
        return valueFrom;
    }

    public void setValueFrom(String valueFrom) {
        this.valueFrom = valueFrom;
    }

}
