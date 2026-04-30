package com.github.streamshub.console.api.security;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.kafka.systemtest.utils.TokenUtils;
import com.github.streamshub.console.test.TestHelper;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

@QuarkusTest
@TestHTTPEndpoint(SessionResource.class)
@TestProfile(TestPlainProfile.class)
class SessionResourceIT {

    private static final String OIDC_TAG = "OIDC";

    @Inject
    Config config;

    @TestHTTPResource
    URI testUri;

    @Inject
    ConsoleConfig consoleConfig;

    TestHelper utils;
    TokenUtils tokens;

    @BeforeEach
    void setup(TestInfo testInfo) {
        utils = new TestHelper(URI.create("/"), config);
        utils.resetSecurity(consoleConfig, testInfo.getTags().contains(OIDC_TAG));
        tokens = new TokenUtils(config);
    }

    @Test
    @Tag(OIDC_TAG)
    void testLoginUnauthenticatedWithJavaScript() {
        whenRequesting(req -> req
                .header("X-Requested-With", "JavaScript")
                .get("login"))
            .assertThat()
            // Quarkus is configured to use a special status 499 to indicate to
            // a JavaScript client that there is no active session.
            .statusCode(is(499))
            .header("WWW-Authenticate", "OIDC");
    }

    @Test
    @Tag(OIDC_TAG)
    void testLoginUnauthenticatedWithAuthorization() {
        whenRequesting(req -> req
                // The API is configured as a hybrid application. When
                // the Authorization header is given, the server responds
                // with the typical 401/Unauthorized status
                .header("Authorization", UUID.randomUUID().toString())
                .get("login"))
            .assertThat()
            .statusCode(is(Status.UNAUTHORIZED.getStatusCode()))
            .header("WWW-Authenticate", "Bearer");
    }

    @Test
    @Tag(OIDC_TAG)
    void testLoginUnauthenticated() {
        // convert the URL because KC advertises the internal port to Quarkus via
        // the well-known endpoint, which is then used for the redirect.
        var authEndpoint = UriBuilder
                .fromUri(URI.create(config.getValue("console.test.oidc-url", String.class)))
                .port(8443)
                .build()
                .toString();

        whenRequesting(req -> req
                // The API is configured as a hybrid application. When
                // no Authorization header is given, the server responds
                // with a 302/Found redirect to the OIDC provider's login
                // form.
                .redirects().follow(false)
                .get("login"))
            .assertThat()
            .statusCode(is(Status.FOUND.getStatusCode()))
            .header("Location", startsWith(authEndpoint));
    }

    @Test
    @Tag(OIDC_TAG)
    void testLoginRedirectUriInLocation() {
        // Jakarta REST forces a the Location header on a redirect response
        // to be an absolute URI. Even though we sent a server-relative path
        // the response will include the request scheme, host, and port along
        // with the path we requested.
        var expectedLocation = UriBuilder.fromUri(testUri)
            .replacePath("/some/path")
            .build()
            .toString();

        whenRequesting(req -> req
                .queryParam("redirect_uri", "/some/path")
                .auth().oauth2(tokens.getToken("alice"))
                .redirects().follow(false)
                .get("login"))
            .assertThat()
            .statusCode(is(Status.SEE_OTHER.getStatusCode()))
            .header("Location", is(expectedLocation));
    }

    @Test
    @Tag(OIDC_TAG)
    void testCurrentUserInformationOIDC() {
        whenRequesting(req -> req
                .auth().oauth2(tokens.getToken("alice"))
                .get("user"))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("anonymous", is(false))
            .body("username", is("alice"))
            .body("fullName", is("Alice User"));
    }

    @Test
    @Tag(OIDC_TAG)
    void testLogoutOIDC() {
        var authEndpoint = UriBuilder
                .fromUri(URI.create(config.getValue("console.test.oidc-url", String.class)))
                .port(8443)
                .build()
                .toString();
        var expectedLocation = UriBuilder.fromUri(testUri)
                .replacePath("/some/path")
                .build()
                .toString();

        whenRequesting(req -> req
                .queryParam("redirect_uri", "/some/path")
                .auth().oauth2(tokens.getToken("alice"))
                .redirects().follow(false)
                .get("logout"))
            .assertThat()
            .statusCode(is(Status.SEE_OTHER.getStatusCode()))
            .header("Location", startsWith(authEndpoint))
            .header("Location", this::queryParams, hasEntry(is("post_logout_redirect_uri"), is(expectedLocation)))
            // hint is only available when OIDC authorization code flow is used,
            // not when tokens retrieved directly by the client like is done in this test.
            .header("Location", this::queryParams, not(hasKey("id_token_hint")));
    }

    @Test
    void testCurrentUserInformationAnonymous() {
        whenRequesting(req -> req
                .get("user"))
            .assertThat()
            .statusCode(is(Status.OK.getStatusCode()))
            .body("anonymous", is(true))
            .body("username", is("ANONYMOUS"))
            .body("fullName", is("Anonymous"));
    }

    // Helper
    public Map<String, String> queryParams(String value) {
        URI uri = URI.create(value);
        String query = uri.getQuery();

        if (query == null || query.isEmpty()) {
            return Map.of();
        }

        return Arrays.stream(query.split("&")).map(pair -> pair.split("=", 2))
                .collect(toMap(arr -> URLDecoder.decode(arr[0], StandardCharsets.UTF_8),
                        arr -> arr.length > 1 ? URLDecoder.decode(arr[1], StandardCharsets.UTF_8) : ""));
    }
}
