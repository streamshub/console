package com.github.streamshub.console.support;

import java.util.Locale;

public class StringSupport {

    public static boolean isAsciiLetterOrDigit(char c) {
        return 'a' <= c && c <= 'z' ||
                'A' <= c && c <= 'Z' ||
                '0' <= c && c <= '9';
    }

    public static String replaceNonAlphanumeric(final String name, char replacement) {
        return replaceNonAlphanumeric(name, replacement, new StringBuilder(name.length()));
    }

    public static String replaceNonAlphanumeric(final String name, char replacement, final StringBuilder sb) {
        int length = name.length();
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);
            if (isAsciiLetterOrDigit(c)) {
                sb.append(c);
            } else {
                sb.append(replacement);
                if (c == '"' && i + 1 == length) {
                    sb.append(replacement);
                }
            }
        }
        return sb.toString();
    }

    public static String toEnv(String name) {
        return replaceNonAlphanumeric(name, '_').toUpperCase(Locale.ROOT);
    }

}
