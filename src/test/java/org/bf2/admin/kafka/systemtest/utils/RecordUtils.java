package org.bf2.admin.kafka.systemtest.utils;

import io.restassured.http.ContentType;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonValue;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;

import static io.restassured.RestAssured.given;

public class RecordUtils {

    public static final String RECORDS_PATH = "/api/v1/topics/{topicName}/records";

    static final Logger log = Logger.getLogger(RecordUtils.class);
    final Config config;
    final String token;

    public RecordUtils(Config config, String token) {
        this.config = config;
        this.token = token;
    }

    private Map<String, ?> getHeaders() {
        return token != null ?
            Map.of(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + token) :
            Collections.emptyMap();
    }

    public void produceRecord(String topicName, ZonedDateTime timestamp, Map<String, Object> headers, String key, String value) {
        given()
            .log().ifValidationFails()
            .headers(getHeaders())
            .contentType(ContentType.JSON)
            .body(buildRecordRequest(null, timestamp, headers, key, value).toString())
        .when()
            .post(RECORDS_PATH, topicName)
        .then()
            .log().ifValidationFails()
            .statusCode(Status.CREATED.getStatusCode());
    }

    public JsonObject buildRecordRequest(Integer partition, ZonedDateTime timestamp, Map<String, Object> headers, String key, String value) {
        return Json.createObjectBuilder()
            .add("partition", partition != null ? Json.createValue(partition) : JsonValue.NULL)
            .add("timestamp", timestamp != null ? Json.createValue(timestamp.toString()) : JsonValue.NULL)
            .add("headers", headers != null ? Json.createObjectBuilder(headers).build() : JsonValue.NULL)
            .add("key", key != null ? Json.createValue(key) : JsonValue.NULL)
            .add("value", value != null ? Json.createValue(value) : JsonValue.NULL)
            .build();
    }
}
