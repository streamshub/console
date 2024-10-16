package com.github.streamshub.console.api;

import java.util.Base64;
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
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.serdes.ArtifactReferences;
import com.github.streamshub.console.api.support.serdes.MultiformatSchemaParser;

import io.apicurio.registry.resolver.DefaultSchemaResolver;
import io.apicurio.registry.resolver.SchemaResolver;
import io.apicurio.registry.rest.client.RegistryClient;

@Path("/api/schemas/{schemaId}")
@Tag(name = "Schema Registry Resources")
public class SchemasResource {

    @Inject
    Logger logger;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    Map<String, KafkaContext> kafkaContexts;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @APIResponse(responseCode = "200", ref = "Configurations", content = @Content())
    @APIResponse(responseCode = "404", ref = "NotFound")
    @APIResponse(responseCode = "500", ref = "ServerError")
    @APIResponse(responseCode = "504", ref = "ServerTimeout")
    public Response getSchemaContent(
            @Parameter(description = "Schema identifier")
            @PathParam("schemaId")
            String schemaId) {

        int clusterTerm = schemaId.indexOf('.');

        if (clusterTerm < 0) {
            throw new NotFoundException("No such schema");
        }

        String clusterId = new String(Base64.getUrlDecoder().decode(schemaId.substring(0, clusterTerm)));

        if (!kafkaContexts.containsKey(clusterId)) {
            throw new NotFoundException("No such schema");
        }

        RegistryClient registryClient = kafkaContexts.get(clusterId).schemaRegistryContext().registryClient();

        if (registryClient == null) {
            throw new NotFoundException("Schema not found, no registry is configured");
        }

        @SuppressWarnings("resource")
        SchemaResolver<Object, ?> schemaResolver = new DefaultSchemaResolver<>();
        schemaResolver.setClient(registryClient);
        schemaResolver.configure(Collections.emptyMap(), new MultiformatSchemaParser<>(Collections.emptySet()));

        schemaId = schemaId.substring(clusterTerm + 1);

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
