@OpenAPIDefinition(
        info = @Info(title = "", version = ""),
        components = @Components(
                responses = {
                    @APIResponse(name = "Configurations",
                            description = "Configurations successfully retrieved",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ConfigEntry.ConfigResponse.class))),
                    @APIResponse(name = "BadRequest",
                            description = "Bad request",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "Invalid query parameter", value = """
                                            {
                                              "errors": [
                                                {
                                                  "kind": "Error",
                                                  "id": "2fa1c4c0-abb1-43b9-a4d6-627e05d19ddb",
                                                  "status": "400",
                                                  "code": "4001",
                                                  "title": "Invalid query parameter",
                                                  "detail": "must be one of [ earliest, latest, maxTimestamp ] or a valid UTC ISO timestamp.",
                                                  "source": {
                                                    "parameter": "offsetSpec"
                                                  }
                                                }
                                              ]
                                            }
                                            """))),
                    @APIResponse(name = "NotAuthenticated",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @APIResponse(name = "NotAuthorized",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @APIResponse(name = "NotFound",
                            description = "Requested resource not found",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "Invalid URL", value = """
                                            {
                                              "errors": [
                                                {
                                                  "kind": "Error",
                                                  "id": "2fa1c4c0-abb1-43b9-a4d6-627e05d19ddb",
                                                  "status": "404",
                                                  "code": "4041",
                                                  "title": "Resource not found",
                                                  "detail": "Unable to find matching target resource method"
                                                }
                                              ]
                                            }
                                            """))),
                    @APIResponse(name = "Conflict",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @APIResponse(name = "ServerError",
                            description = "Internal server error",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "Internal Server Error", value = """
                                            {
                                              "errors": [
                                                {
                                                  "kind": "Error",
                                                  "id": "2fa1c4c0-abb1-43b9-a4d6-627e05d19ddb",
                                                  "status": "500",
                                                  "code": "5001",
                                                  "title": "Unexpected error",
                                                  "detail": "The server has encounted an internal error, most likely a bug"
                                                }
                                              ]
                                            }
                                            """))),
                    @APIResponse(name = "ServerTimeout",
                            description = "Backend service timeout",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON,
                                    schema = @Schema(implementation = ErrorResponse.class),
                                    examples = @ExampleObject(name = "Backend Service Timeout", value = """
                                            {
                                              "errors": [
                                                {
                                                  "kind": "Error",
                                                  "id": "2fa1c4c0-abb1-43b9-a4d6-627e05d19ddb",
                                                  "status": "504",
                                                  "code": "5041",
                                                  "title": "Timed out waiting for backend service",
                                                  "detail": "Response from Kafka not received before time limit reached"
                                                }
                                              ]
                                            }
                                            """))),
                },
                examples = {
                    @ExampleObject(name = "Earliest Offset",
                            value = "earliest",
                            description = "Used to retrieve the earliest offset of a partition"),
                    @ExampleObject(name = "Latest Offset",
                            value = "latest",
                            description = "Used to retrieve the latest offset of a partition"),
                    @ExampleObject(name = "Max Timestamp",
                            value = "maxTimestamp",
                            description = """
                            Used to retrieve the offset with the largest timestamp of a partition
                            as message timestamps can be specified client side this may not match
                            the log end offset returned by LatestSpec
                            """),
                    @ExampleObject(name = "Literal Timestamp",
                            value = "2023-01-01T00:00:00Z",
                            description = """
                            Used to retrieve the earliest offset whose timestamp is greater than
                            or equal to the given timestamp in the corresponding partition
                            """)
                }
        ))
package com.github.eyefloaters.console.api;

import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import com.github.eyefloaters.console.api.model.ConfigEntry;
import com.github.eyefloaters.console.api.model.ErrorResponse;
