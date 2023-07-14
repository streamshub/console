package com.github.eyefloaters.console.legacy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RecordOperationsTest {

    @ParameterizedTest
    @CsvSource(nullValues = "null", value = {
        "null",
        "''",
        "Valid value"
    })
    void testBytesToStringValid(String input) {
        RecordOperations target = new RecordOperations();
        String out = target.bytesToString(input != null ? input.getBytes() : null, null);
        assertEquals(input, out);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "\uFFFE",
        "\uFFFF",
        "Valid until the end\uFFFF"
    })
    void testBytesToStringNoncharacters(String sequence) {
        RecordOperations target = new RecordOperations();
        String out = target.bytesToString(sequence.getBytes(), null);
        assertEquals(RecordOperations.BINARY_DATA_MESSAGE, out);
    }

    @Test
    void testBytesToStringNoncharacterRange() {
        for (char i = '\uFDD0'; i < '\uFDEF' + 1; i++) {
            RecordOperations target = new RecordOperations();
            String out = target.bytesToString(String.valueOf(i).getBytes(), null);
            assertEquals(RecordOperations.BINARY_DATA_MESSAGE, out);
        }
    }

    @Test
    void testBytesToStringUnmappable() throws UnsupportedEncodingException {
        RecordOperations target = new RecordOperations();
        String out = target.bytesToString("¬".getBytes("ISO-8859-1"), null);
        assertEquals(RecordOperations.BINARY_DATA_MESSAGE, out);
    }

    @Test
    void testBytesToStringUnmappableSequence() throws Exception {
        RecordOperations target = new RecordOperations();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (Writer writer = new OutputStreamWriter(bytes, "windows-1252")) {
            writer.write("©");
        }
        String out = target.bytesToString(bytes.toByteArray(), null);
        assertEquals(RecordOperations.BINARY_DATA_MESSAGE, out);
    }

    @Test
    void testBytesToStringInvalid() throws UnsupportedEncodingException {
        RecordOperations target = new RecordOperations();
        String out = target.bytesToString(new byte[] {
            (byte) 0xC1
        }, null);
        assertEquals(RecordOperations.BINARY_DATA_MESSAGE, out);
    }

    static int[] continuationRange() {
        return IntStream.rangeClosed(0x80, 0xbf).toArray();
    }

    @ParameterizedTest
    @MethodSource("continuationRange")
    void testBytesToStringContinuationRange(int continuation) {
        RecordOperations target = new RecordOperations();
        String out = target.bytesToString(new byte[] {
            (byte) continuation
        }, null);
        assertEquals(RecordOperations.BINARY_DATA_MESSAGE, out);
    }
}
