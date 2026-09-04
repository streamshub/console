package com.github.streamshub.console.api.support.serdes;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * The multi-format de-/serializer uses this type as both the key and value in
 * order to provide a bi-directional flow of information. Meta information about
 * the associated schema is passed between clients of Producer/Consumer and the
 * serializer/de-serializer, and error information may also be conveyed back to
 * the client without throwing an exception and disrupting the processing of the
 * topic.
 */
public class RecordData {

    public final Map<String, String> meta = LinkedHashMap.newLinkedHashMap(1);
    private byte[] data;
    private String stringData;
    com.github.streamshub.console.api.model.jsonapi.JsonApiError error;

    public RecordData(byte[] data) {
        super();
        this.data = data;
    }

    public RecordData(String data) {
        super();
        this.stringData = data;
    }

    public com.github.streamshub.console.api.model.jsonapi.JsonApiError error() {
        return error;
    }

    /**
     * Returns the raw bytes for this record data. If a byte array was provided at
     * construction, it is returned directly. If a String was provided, it is lazily
     * encoded to UTF-8 bytes. Returns {@code null} when both fields are null.
     */
    public byte[] bytes() {
        if (data != null) {
            return data;
        }
        if (stringData != null) {
            return stringData.getBytes(StandardCharsets.UTF_8);
        }
        return null;
    }

    public void bytes(byte[] data) {
        this.data = Objects.requireNonNull(data);
        this.stringData = null;
    }

    /**
     * Returns the original String value provided at construction, or {@code null}
     * when the instance was constructed from a byte array (or with a null String).
     */
    public String stringValue() {
        return stringData;
    }

}
