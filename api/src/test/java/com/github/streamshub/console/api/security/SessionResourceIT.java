package com.github.streamshub.console.api.security;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.config.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

import com.github.streamshub.console.api.support.Holder;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.kafka.systemtest.TestPlainProfile;
import com.github.streamshub.console.kafka.systemtest.utils.TokenUtils;
import com.github.streamshub.console.test.TestHelper;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;

import static com.github.streamshub.console.test.TestHelper.whenRequesting;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
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
    void testLogoutOIDCWithRPInitiatedFlow() throws Exception {
        var authEndpoint = UriBuilder
                .fromUri(URI.create(config.getValue("console.test.oidc-url", String.class)))
                .port(8443)
                .build()
                .toString();
        var expectedPostLogoutUri = UriBuilder.fromUri(testUri)
                .replacePath("/")
                .build()
                .toString();

        // Perform the full OIDC Authorization Code flow so that Quarkus sets a
        // session cookie. The RP-initiated logout endpoint only works for
        // session-authenticated users, not for stateless Bearer token callers.
        var loginUri = UriBuilder.fromUri(testUri).replacePath("/api/session/login").build();
        var sessionCookies = tokens.performOidcLogin(loginUri, "alice", "alice-password");

        whenRequesting(req -> req
                .cookies(sessionCookies)
                .redirects().follow(false)
                .get("logout"))
            .assertThat()
            .statusCode(is(Status.FOUND.getStatusCode()))
            .header("Location", startsWith(authEndpoint))
            .header("Location", this::queryParams, hasEntry(is("post_logout_redirect_uri"), is(expectedPostLogoutUri)))
            // id_token_hint is included because we used the authorization code flow and
            // Quarkus holds the id_token server-side in the session.
            .header("Location", this::queryParams, hasKey("id_token_hint"));
    }

    @Test
    @Tag(OIDC_TAG)
    void testLogoutOIDCWithLocalOnlyFlow() throws Exception {
        QuarkusMock.installMockForType(
                Holder::empty,
                new TypeLiteral<Supplier<Holder<String>>>() {
                    // used for type signature
                },
                NamedLiteral.of(OidcDiscoveryResponseFilter.OIDC_END_SESSION_ENDPOINT));

        // Perform the full OIDC Authorization Code flow so that Quarkus sets a
        // session cookie. The RP-initiated logout endpoint only works for
        // session-authenticated users, not for stateless Bearer token callers.
        var loginUri = UriBuilder.fromUri(testUri).replacePath("/api/session/login").build();
        var sessionCookies = tokens.performOidcLogin(loginUri, "alice", "alice-password");

        // Logout is local only. Client is redirected to the root URL
        var expectedLocation = UriBuilder.fromUri(testUri)
                .replacePath("/")
                .build()
                .toString();

        whenRequesting(req -> req
                .cookies(sessionCookies)
                .redirects().follow(false)
                .get("logout"))
            .assertThat()
            .statusCode(is(Status.SEE_OTHER.getStatusCode()))
            .header("Location", is(expectedLocation))
            .header("Location", this::queryParams, is(anEmptyMap()));
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
