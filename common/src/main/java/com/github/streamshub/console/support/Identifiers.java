package com.github.streamshub.console.support;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

public final class Identifiers {

    public static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    public static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    private Identifiers() {
    }

    public static String encode(String value) {
        return encode("", value);
    }

    /**
     * id values must be URL-safe, so we encode the unknown/variable values of object
     * names from Kafka Connect and.
     */
    public static String encode(String base, String... values) {
        StringBuilder encoded = new StringBuilder(base);
        for (String v : values) {
            if (!encoded.isEmpty()) {
                encoded.append(',');
            }
            encoded.append(ENCODER.encodeToString(v.getBytes(StandardCharsets.UTF_8)));
        }
        return encoded.toString();
    }

    public static String[] decode(String value) {
        List<String> decoded = new ArrayList<>();
        for (String v : value.split(Pattern.quote(","))) {
            decoded.add(new String(DECODER.decode(v), StandardCharsets.UTF_8));
        }
        return decoded.toArray(String[]::new);
    }
}
