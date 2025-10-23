package com.github.streamshub.console.api;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.console.api.support.serdes.ArtifactReferences;
import com.github.streamshub.console.api.support.serdes.MultiformatSchemaParser;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.SchemaRegistryConfig;

import io.apicurio.registry.resolver.DefaultSchemaResolver;
import io.apicurio.registry.resolver.SchemaResolver;
import io.apicurio.registry.resolver.client.RegistryClientFacade;

@Path("/api/registries/{registryId}/schemas/{schemaId}")
@Tag(name = "Schema Registry Resources")
public class SchemasResource {

    @Inject
    Logger logger;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    Map<String, RegistryClientFacade> registryClients;

    /**
     * Retrieve the schema content from the identified/named registry.
     *
     * <p>
     * Although opaque to the client, the schemaId is a base-64 encoded, JSON-ified
     * {@link io.apicurio.registry.resolver.strategy.ArtifactReference ArtifactReference}
     * which will be parsed with {@link ArtifactReferences#fromSchemaId}.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", ref = "Configurations", content = @Content())
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public Response getSchemaContent(
            @Parameter(description = "Schema registry identifier (name)")
            @PathParam("registryId")
            String registryId,

            @Parameter(description = "Schema identifier")
            @PathParam("schemaId")
            String schemaId) {

        RegistryClientFacade registryClient = consoleConfig.getSchemaRegistries()
                .stream()
                .filter(config -> config.getName().equals(registryId))
                .map(SchemaRegistryConfig::getName)
                .map(registryClients::get)
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Unknown registry"));

        @SuppressWarnings("resource")
        SchemaResolver<Object, ?> schemaResolver = new DefaultSchemaResolver<>();
        schemaResolver.setClientFacade(registryClient);
        schemaResolver.configure(Collections.emptyMap(), new MultiformatSchemaParser<>(Collections.emptySet()));

        var reference = ArtifactReferences.fromSchemaId(schemaId, objectMapper);
        var schema = schemaResolver.resolveSchemaByArtifactReference(reference);

        var response = Optional.ofNullable(schema)
                .map(s -> s.getParsedSchema())
                .map(s -> s.getRawSchema())
                .map(Response::ok)
                .orElseThrow(() -> new NotFoundException("No such schema"));

        return response.build();
    }

}
