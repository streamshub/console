package com.github.streamshub.console.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringSupportTest {

    @Test
    void testReplaceNonAlphanumericStringChar() {
        String actual = StringSupport.replaceNonAlphanumeric("hyphenated-segment", '.');
        assertEquals("hyphenated.segment", actual);
    }

    @ParameterizedTest
    @CsvSource({
        "console.config.\"Hyphenated-Segment-1\".value, CONSOLE_CONFIG__HYPHENATED_SEGMENT_1__VALUE",
        "console.config.\"Hyphenated-Segment-1\", CONSOLE_CONFIG__HYPHENATED_SEGMENT_1__",
        "console.config.{Braced-Segment}, CONSOLE_CONFIG__BRACED_SEGMENT_",
    })
    void testToEnv(String input, String expected) {
        String actual = StringSupport.toEnv(input);
        assertEquals(expected, actual);
    }

}
