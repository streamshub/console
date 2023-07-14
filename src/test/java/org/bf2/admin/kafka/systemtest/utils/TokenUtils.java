package org.bf2.admin.kafka.systemtest.utils;

import io.restassured.http.Header;
import org.bf2.admin.kafka.admin.KafkaAdminConfigRetriever;
import org.eclipse.microprofile.config.Config;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.ws.rs.core.HttpHeaders;

public class TokenUtils {

    final String tokenEndpoint;

    public TokenUtils(Config config) {
        this.tokenEndpoint = config.getValue(KafkaAdminConfigRetriever.OAUTH_TOKEN_ENDPOINT_URI, String.class);
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
        final String payload = String.format("grant_type=password&username=%1$s&password=%1$s-password&client_id=kafka-cli", username);

        /*
         * Requires JDK 11.0.4+. If the `Host` header is not set, Keycloak will
         * generate tokens with an issuer URI containing localhost:<random port>.
         */
        System.setProperty("jdk.httpclient.allowRestrictedHeaders", "host");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenEndpoint))
                .header("Host", "keycloak:8080")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> response = HttpClient
                    .newBuilder()
                    .build()
                    .send(request, BodyHandlers.ofString());

            try (JsonReader reader = Json.createReader(new StringReader(response.body()))) {
                return reader.readObject();
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
