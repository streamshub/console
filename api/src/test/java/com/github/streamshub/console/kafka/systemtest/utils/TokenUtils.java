package com.github.streamshub.console.kafka.systemtest.utils;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.ws.rs.core.HttpHeaders;

import org.eclipse.microprofile.config.Config;

import io.restassured.http.Header;

public class TokenUtils {

    final String tokenEndpoint;

    public TokenUtils(Config config) {
        this.tokenEndpoint = config.getValue("console.test.oidc-url", String.class) + "/protocol/openid-connect/token";
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

        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenEndpoint))
                .header("Host", "localhost:8080")
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
}
