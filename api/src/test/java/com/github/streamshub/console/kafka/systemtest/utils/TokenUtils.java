package com.github.streamshub.console.kafka.systemtest.utils;

import java.io.IOException;
import java.io.StringReader;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriBuilder;

import org.eclipse.microprofile.config.Config;

import com.github.streamshub.console.api.support.TrustStoreSupport;
import com.github.streamshub.console.config.security.OidcConfig;

import io.quarkus.tls.TlsConfigurationRegistry;
import io.restassured.http.Header;

public class TokenUtils {

    final String tokenEndpoint;
    final String tokenEndpointHost;
    final SSLContext tls;

    final String oidcUrl;

    public TokenUtils(Config config) {
        this.oidcUrl = config.getValue("console.test.oidc-url", String.class);
        this.tokenEndpoint = oidcUrl + "/protocol/openid-connect/token";
        this.tokenEndpointHost = config.getValue("console.test.oidc-host", String.class);

        var tlsRegistry = CDI.current().select(TlsConfigurationRegistry.class).get();

        try {
            tls = tlsRegistry.get(TrustStoreSupport.trustConfigName(new OidcConfig(), null))
                    .orElseThrow()
                    .createSSLContext();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Header authorizationHeader(String username) {
        return new Header(HttpHeaders.AUTHORIZATION, "Bearer " + getToken(username));
    }

    public Header invalidAuthorizationHeader() {
        return new Header(HttpHeaders.AUTHORIZATION, "Bearer invalid.bearer.token");
    }

    public String getToken(String username) {
        if (username == null) {
            return UUID.randomUUID().toString();
        }

        return getTokenObject(username).getString("access_token");
    }

    public JsonObject getTokenObject(String username) {
        final String form = String.format("grant_type=password&"
                + "username=%1$s&"
                + "password=%1$s-password&"
                + "client_id=console-client", username);

        HttpClient client = HttpClient.newBuilder()
                .sslContext(tls)
                .version(Version.HTTP_1_1)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenEndpoint))
                .header("Host", tokenEndpointHost)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        try {
            HttpResponse<String> response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString());
            JsonObject payload;

            try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                payload = reader.readObject();
            }

            if (response.statusCode() != 200) {
                throw new RuntimeException(payload.toString());
            }

            return payload;
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Drives the OIDC Authorization Code flow against the real Keycloak container,
     * returning the Quarkus session cookies ({@code q_session*}) so callers can make
     * session-authenticated requests.
     *
     * <p>The flow is:
     * <ol>
     *   <li>GET {@code loginUri} → 302 to Keycloak /auth (Quarkus sets a state cookie)</li>
     *   <li>GET the Keycloak login page to obtain the form action URL</li>
     *   <li>POST credentials to the form action → 302 back to the app callback</li>
     *   <li>GET the app callback → Quarkus exchanges the code and sets the session cookie</li>
     * </ol>
     *
     * <p>A raw {@link HttpClient} is used instead of RestAssured because the redirect
     * chain crosses origins (app port ↔ Keycloak port) and RestAssured's automatic
     * redirect following does not carry cookies across different hosts.
     *
     * @param loginUri  the absolute URI of the app's login endpoint (e.g. {@code /session/login})
     * @param username  Keycloak username
     * @param password  Keycloak password
     * @return map of session cookie name → value for the app origin
     */
    public Map<String, String> performOidcLogin(URI loginUri, String username, String password) throws Exception {
        var cookieManager = new CookieManager();

        HttpClient client = HttpClient.newBuilder()
                .sslContext(tls)
                .followRedirects(Redirect.NEVER)
                .version(Version.HTTP_1_1)
                .cookieHandler(cookieManager)
                .build();

        // Step 1: trigger the OIDC redirect from the Quarkus app.
        var step1 = client.send(
                HttpRequest.newBuilder(loginUri).GET().build(),
                BodyHandlers.ofString());
        var kcAuthLocation = step1.headers().firstValue("Location")
                .orElseThrow(() -> new IllegalStateException(
                        "Expected redirect to Keycloak, got: " + step1.statusCode()));

        // Keycloak advertises its internal port in the Location URL; translate it to
        // the externally mapped port that the test container host can actually reach.
        var kcAuthUri = translateKcUri(URI.create(kcAuthLocation));

        // Step 2: fetch the Keycloak login page and extract the form action URL.
        var step2 = client.send(
                HttpRequest.newBuilder(kcAuthUri)
                        .header("Host", tokenEndpointHost)
                        .GET()
                        .build(),
                BodyHandlers.ofString());
        var formAction = parseFormAction(step2.body());

        // Step 3: POST the credentials to Keycloak.
        var formBody = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8)
                + "&credentialId=";

        var step3 = client.send(
                HttpRequest.newBuilder(translateKcUri(URI.create(formAction)))
                        .header("Host", tokenEndpointHost)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formBody))
                        .build(),
                BodyHandlers.ofString());
        var callbackLocation = step3.headers().firstValue("Location")
                .orElseThrow(() -> new IllegalStateException(
                        "Expected redirect to app callback, got: " + step3.statusCode()));

        // Step 4: complete the code exchange — Quarkus handles this internally and
        // sets the session cookie on the response.
        client.send(
                HttpRequest.newBuilder(URI.create(callbackLocation)).GET().build(),
                BodyHandlers.ofString());

        // Collect all q_session* cookies stored by the cookie manager for the app origin.
        var appOrigin = URI.create(loginUri.getScheme() + "://" + loginUri.getAuthority());
        return cookieManager.getCookieStore()
                .get(appOrigin)
                .stream()
                .filter(c -> c.getName().startsWith("q_session"))
                .collect(Collectors.toMap(HttpCookie::getName, HttpCookie::getValue));
    }

    /**
     * Keycloak advertises URLs using its internal container port (8443) in the
     * well-known discovery document. Translate those to the externally mapped port
     * so the test container host can be reached.
     */
    private URI translateKcUri(URI uri) {
        var kc = URI.create(oidcUrl);
        return UriBuilder.fromUri(uri)
                .host(kc.getHost())
                .port(kc.getPort())
                .build();
    }

    /**
     * Extracts the {@code action} attribute value from the first {@code <form>}
     * element in a Keycloak login page.
     */
    private String parseFormAction(String html) {
        var matcher = Pattern.compile("action=\"([^\"]+)\"").matcher(html);
        if (matcher.find()) {
            return matcher.group(1).replace("&amp;", "&");
        }
        throw new IllegalStateException("Could not find form action in Keycloak login page");
    }
}
