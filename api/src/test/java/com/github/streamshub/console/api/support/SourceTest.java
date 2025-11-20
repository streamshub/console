package com.github.streamshub.console.api.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.github.streamshub.console.api.model.jsonapi.ErrorSource;
import com.github.streamshub.console.api.support.ErrorCategory.Source;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    void testErrorSourceEmpty() {
        ErrorSource source = new ErrorSource(null, null, null);
        assertEquals("empty", source.toString());
    }

}
