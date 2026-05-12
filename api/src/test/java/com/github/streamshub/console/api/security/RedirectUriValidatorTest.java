package com.github.streamshub.console.api.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RedirectUriValidatorTest {

    private RedirectUriValidator validator;

    @BeforeEach
    void setUp() {
        validator = new RedirectUriValidator();
    }

    @Test
    void testNullRedirectUri() {
        String result = validator.validateAndSanitize(null);
        assertEquals("/", result, "Null redirect URI should default to /");
    }

    @Test
    void testBlankRedirectUri() {
        String result = validator.validateAndSanitize("");
        assertEquals("/", result, "Blank redirect URI should default to /");

        result = validator.validateAndSanitize("   ");
        assertEquals("/", result, "Whitespace redirect URI should default to /");
    }

    @Test
    void testRelativePathAllowed() {
        String result = validator.validateAndSanitize("/");
        assertEquals("/", result, "Root path should be allowed");

        result = validator.validateAndSanitize("/topics");
        assertEquals("/topics", result, "Relative path /topics should be allowed");

        result = validator.validateAndSanitize("/overview/cluster");
        assertEquals("/overview/cluster", result, "Nested relative path should be allowed");

        result = validator.validateAndSanitize("/topics?filter=test");
        assertEquals("/topics?filter=test", result, "Relative path with query params should be allowed");
    }

    @Test
    void testProtocolRelativeUrlBlocked() {
        String result = validator.validateAndSanitize("//evil.com");
        assertEquals("/", result, "Protocol-relative URL should be blocked");

        result = validator.validateAndSanitize("//evil.com/phishing");
        assertEquals("/", result, "Protocol-relative URL with path should be blocked");
    }

    @Test
    void testRelativePathWithoutLeadingSlash() {
        String result = validator.validateAndSanitize("topics");
        assertEquals("/", result, "Relative path without leading slash should be blocked");
    }

    @Test
    void testAbsoluteUrlBlocked() {
        String result = validator.validateAndSanitize("https://console.example.com:8443/topics");
        assertEquals("/", result, "Absolute URL should be blocked (even if same-origin)");

        result = validator.validateAndSanitize("https://console.example.com:8443/");
        assertEquals("/", result, "Absolute root URL should be blocked");

        result = validator.validateAndSanitize("http://localhost:8080/topics");
        assertEquals("/", result, "Localhost absolute URL should be blocked");
    }

    @Test
    void testCrossOriginUrlBlocked() {
        String result = validator.validateAndSanitize("https://evil.com/phishing");
        assertEquals("/", result, "Cross-origin URL should be blocked");

        result = validator.validateAndSanitize("https://attacker.com");
        assertEquals("/", result, "Different domain should be blocked");
    }

    @Test
    void testInvalidSchemeBlocked() {
        String result = validator.validateAndSanitize("javascript:alert(1)");
        assertEquals("/", result, "JavaScript URL should be blocked");

        result = validator.validateAndSanitize("data:text/html,<script>alert(1)</script>");
        assertEquals("/", result, "Data URL should be blocked");

        result = validator.validateAndSanitize("file:///etc/passwd");
        assertEquals("/", result, "File URL should be blocked");

        result = validator.validateAndSanitize("ftp://evil.com");
        assertEquals("/", result, "FTP URL should be blocked");
    }

    @Test
    void testInvalidUriSyntax() {
        String result = validator.validateAndSanitize("ht!tp://invalid");
        assertEquals("/", result, "Invalid URI syntax should default to /");

        result = validator.validateAndSanitize("https://[invalid");
        assertEquals("/", result, "Malformed URI should default to /");
    }

    @Test
    void testUrlWithFragment() {
        String result = validator.validateAndSanitize("/topics#section");
        assertEquals("/topics#section", result, "Relative URL with fragment should be allowed");
    }

    @Test
    void testUrlWithComplexQueryParams() {
        String result = validator.validateAndSanitize("/topics?filter=test&sort=name&page=2");
        assertEquals("/topics?filter=test&sort=name&page=2", result, "Relative URL with multiple query params should be allowed");
    }
}
