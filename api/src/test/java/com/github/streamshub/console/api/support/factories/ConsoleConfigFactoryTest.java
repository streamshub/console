package com.github.streamshub.console.api.support.factories;

import java.io.UncheckedIOException;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleConfigFactoryTest {

    static final String SECRET = "super-secret-oidc-client-value";

    @Test
    void testBindConfigDoesNotLeakSecretWhenFieldTypeMismatched() throws Exception {
        ConsoleConfigFactory factory = new ConsoleConfigFactory();
        factory.mapper = new ObjectMapper();

        ObjectMapper reader = new ObjectMapper();
        JsonNode tree = reader.readTree("""
            {
              "schemaRegistries": [
                {
                  "name": "test-registry",
                  "url": "http://example.com",
                  "authentication": {
                    "oidc": {
                      "clientId": "console",
                      "clientSecret": "%s"
                    }
                  }
                }
              ]
            }
            """.formatted(SECRET));

        UncheckedIOException thrown = assertThrows(UncheckedIOException.class, () -> factory.bindConfig(tree));

        String fullMessage = thrown.getMessage() + ' ' + thrown.getCause();
        assertFalse(fullMessage.contains(SECRET), () -> "Exception leaked secret value: " + fullMessage);
        assertTrue(thrown.getMessage().contains("schemaRegistries[0].authentication.oidc.clientSecret"),
                () -> "Exception did not identify failing path: " + thrown.getMessage());
    }

    @Test
    void testBindConfigReturnsParsedConfig() throws Exception {
        ConsoleConfigFactory factory = new ConsoleConfigFactory();
        factory.mapper = new ObjectMapper();

        ObjectMapper reader = new ObjectMapper();
        JsonNode tree = reader.readTree("{ \"schemaRegistries\": [] }");

        assertEquals(0, factory.bindConfig(tree).getSchemaRegistries().size());
    }
}
