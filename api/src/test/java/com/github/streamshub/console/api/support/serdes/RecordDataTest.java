package com.github.streamshub.console.api.support.serdes;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RecordDataTest {

    // --- byte[] constructor ---

    @Test
    void byteConstructor_bytes_returnsGivenArray() {
        byte[] data = {1, 2, 3};
        assertArrayEquals(data, new RecordData(data).bytes());
    }

    @Test
    void byteConstructor_stringValue_isNull() {
        assertNull(new RecordData(new byte[] {1}).stringValue());
    }

    @Test
    void byteConstructor_nullBytes_bytesReturnsNull() {
        assertNull(new RecordData((byte[]) null).bytes());
    }

    @Test
    void byteConstructor_nullBytes_stringValueIsNull() {
        assertNull(new RecordData((byte[]) null).stringValue());
    }

    // --- String constructor ---

    @Test
    void stringConstructor_stringValue_returnsGivenString() {
        assertEquals("hello", new RecordData("hello").stringValue());
    }

    @Test
    void stringConstructor_bytes_lazilyEncodesToUtf8() {
        String value = "héllo";
        byte[] expected = value.getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, new RecordData(value).bytes());
    }

    @Test
    void stringConstructor_nullString_stringValueIsNull() {
        assertNull(new RecordData((String) null).stringValue());
    }

    @Test
    void stringConstructor_nullString_bytesReturnsNull() {
        assertNull(new RecordData((String) null).bytes());
    }

    @Test
    void stringConstructor_emptyString_bytesIsEmpty() {
        assertArrayEquals(new byte[0], new RecordData("").bytes());
    }
}
