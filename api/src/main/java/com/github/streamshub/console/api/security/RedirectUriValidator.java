package com.github.streamshub.console.api.security;

import java.net.URI;
import java.net.URISyntaxException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;

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
     * Validates and sanitizes {@code redirectUri}, then combines the resulting safe
     * path with the scheme, host, and port taken from {@code uriInfo} to produce a
     * fully-qualified redirect {@link URI} that stays within the application.
     *
     * @param redirectUri the caller-supplied redirect path to validate; may be
     *                    {@code null} or blank, in which case {@code "/"} is used
     * @param uriInfo     the JAX-RS {@link UriInfo} of the current request, used to
     *                    inherit scheme, host, and port for the returned URI
     * @return a safe, fully-qualified {@link URI} suitable for use in a redirect
     *         response
     */
    public URI safeRedirectUri(String redirectUri, UriInfo uriInfo) {
        String safePath = validateAndSanitize(redirectUri);

        return UriBuilder.fromUri(uriInfo.getRequestUri())
            .replacePath(safePath)
            .replaceQuery(null)
            .fragment(null)
            .build();
    }

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
