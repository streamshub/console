package com.github.streamshub.console.api.support.serdes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import io.apicurio.registry.resolver.ParsedSchema;

public class RecordData {

    public static final String BINARY_DATA_MESSAGE = "Binary or non-UTF-8 encoded data cannot be displayed";
    static final int REPLACEMENT_CHARACTER = '\uFFFD';

    public final Map<String, String> meta = new LinkedHashMap<>(1);
    byte[] data;
    ParsedSchema<?> schema;
    com.github.streamshub.console.api.model.Error error;

    public RecordData(byte[] data, ParsedSchema<?> schema) {
        super();
        this.data = data;
        this.schema = schema;
    }

    public RecordData(byte[] data) {
        super();
        this.data = data;
        this.schema = null;
    }

    public RecordData(String data) {
        this(data != null ? data.getBytes(StandardCharsets.UTF_8) : null);
    }

    public ParsedSchema<?> schema() {
        return schema;
    }

    public com.github.streamshub.console.api.model.Error error() {
        return error;
    }

    public String dataString(Integer maxValueLength) {
        return bytesToString(data, maxValueLength);
    }

    public static String bytesToString(byte[] bytes, Integer maxValueLength) {
        if (bytes == null) {
            return null;
        }

        if (bytes.length == 0) {
            return "";
        }

        int bufferSize = maxValueLength != null ? Math.min(maxValueLength, bytes.length) : bytes.length;
        StringBuilder buffer = new StringBuilder(bufferSize);

        try (Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8)) {
            int input;

            while ((input = reader.read()) > -1) {
                if (input == REPLACEMENT_CHARACTER || !Character.isDefined(input)) {
                    return BINARY_DATA_MESSAGE;
                }

                buffer.append((char) input);

                if (maxValueLength != null && buffer.length() == maxValueLength) {
                    break;
                }
            }

            return buffer.toString();
        } catch (IOException e) {
            return BINARY_DATA_MESSAGE;
        }
    }

}
