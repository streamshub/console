package com.github.eyefloaters.console.api.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UuidValidatorTest {

    KafkaUuid.Validator target;

    @BeforeEach
    void setUp() throws Exception {
        target = new KafkaUuid.Validator();
    }

    @ParameterizedTest
    @CsvSource({
        "                        , true",
        "''                      , false",
        "'dfGTCUxRSQWxpl--2Ditng', true",
        "'invalid#$%^'           , false"
    })
    void testIsValid(String value, boolean expectedResult) {
        boolean actualResult = target.isValid(value, null);
        assertEquals(expectedResult, actualResult);
    }

}
