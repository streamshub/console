package com.github.eyefloaters.console.api.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.github.eyefloaters.console.api.model.ErrorSource;
import com.github.eyefloaters.console.api.support.ErrorCategory.Source;

import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceTest {

    @ParameterizedTest
    @CsvSource({
        "PARAMETER, parameter[value1]",
        "POINTER, pointer[value1]",
        "HEADER, header[value1]"
    })
    void testErrorSource(Source source, String toStringValue) {
        ErrorSource errorSource = source.errorSource("value1");
        assertEquals(toStringValue, errorSource.toString());
    }

    @Test
    void testErrorSourceNull() {
        assertNull(Source.NONE.errorSource("value1"));
    }

}
