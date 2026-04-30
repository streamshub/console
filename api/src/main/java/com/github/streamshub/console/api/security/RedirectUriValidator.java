package com.github.streamshub.console.api.security;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.enterprise.context.ApplicationScoped;

import org.jboss.logging.Logger;

/**
 * Validates redirect URIs to prevent open redirect vulnerabilities.
 * Only allows relative paths within the application to ensure redirects
 * cannot be exploited for phishing attacks.
 */
@ApplicationScoped
public class RedirectUriValidator {

    private static final Logger LOGGER = Logger.getLogger(RedirectUriValidator.class);
    private static final String DEFAULT_REDIRECT = "/";

    /**
     * Validates that a redirect URI is safe to use.
     * Only allows relative paths (e.g., "/", "/topics", "/overview").
     * Absolute URLs are rejected to prevent open redirect vulnerabilities,
     * even if they appear to be same-origin (since the request origin itself
     * could be attacker-controlled).
     *
     * @param redirectUri The URI to validate
     * @return A safe redirect URI (defaults to "/" if invalid)
     */
    public String validateAndSanitize(String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            LOGGER.debugf("Redirect URI is null or blank, using default: %s", DEFAULT_REDIRECT);
            return DEFAULT_REDIRECT;
        }

        try {
            URI uri = new URI(redirectUri);

            // Only allow relative paths (no scheme/host)
            if (!uri.isAbsolute()) {
                // Ensure it starts with / to prevent protocol-relative URLs like "//evil.com"
                if (redirectUri.startsWith("/") && !redirectUri.startsWith("//")) {
                    LOGGER.debugf("Allowing relative path redirect: %s", redirectUri);
                    return redirectUri;
                }
                LOGGER.warnf("Rejecting invalid relative path (protocol-relative or missing /): %s", redirectUri);
                return DEFAULT_REDIRECT;
            }

            // Reject all absolute URIs to prevent open redirect vulnerabilities
            LOGGER.warnf("Rejecting absolute URI redirect attempt: %s", redirectUri);
            return DEFAULT_REDIRECT;
        } catch (URISyntaxException e) {
            LOGGER.warnf("Invalid URI syntax for redirect: %s - %s", redirectUri, e.getMessage());
            return DEFAULT_REDIRECT;
        }
    }
}
