package com.github.eyefloaters.console.api.support;

import java.util.TreeMap;

import org.eclipse.microprofile.openapi.OASFactory;
import org.eclipse.microprofile.openapi.OASFilter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;

import com.github.eyefloaters.console.api.support.StringListParamConverterProvider.StringListParamConverter;

import io.smallrye.openapi.api.util.FilterUtil;
import io.smallrye.openapi.api.util.UnusedSchemaFilter;

public class OASModelFilter extends AbstractOperationFilter implements OASFilter {

    @Override
    public Schema filterSchema(Schema schema) {
        if (schema.getType() == SchemaType.ARRAY && schema.getDefaultValue() instanceof String dflt) {
            schema.setDefaultValue(new StringListParamConverter().fromString(dflt));
        }

        if (schema.getType() == SchemaType.ARRAY && schema.getEnumeration() != null) {
            schema.getItems().setEnumeration(schema.getEnumeration());
            schema.setEnumeration(null);
        }

        return OASFilter.super.filterSchema(schema);
    }

    /**
     * Remove disabled operations from the OpenAPI model
     */
    @Override
    public PathItem filterPathItem(PathItem pathItem) {
        pathItem.getOperations().forEach((method, operation) -> {
            if (disabled(method.name(), operation.getOperationId())) {
                pathItem.setOperation(method, null);
            }
        });

        // Remove the entire path if no operations remain enabled
        return pathItem.getOperations().isEmpty() ? null : pathItem;
    }

    @Override
    public void filterOpenAPI(OpenAPI openAPI) {
        openAPI.getComponents().addSchema("OffsetSpec", OASFactory.createSchema()
                .type(SchemaType.STRING)
                .defaultValue(KafkaOffsetSpec.LATEST)
                .addOneOf(OASFactory.createSchema()
                        .type(SchemaType.STRING)
                        .addEnumeration(KafkaOffsetSpec.EARLIEST)
                        .addEnumeration(KafkaOffsetSpec.LATEST)
                        .addEnumeration(KafkaOffsetSpec.MAX_TIMESTAMP))
                .addOneOf(OASFactory.createSchema()
                        .ref("Instant")));

        // Sort global schemas
        openAPI.getComponents().setSchemas(new TreeMap<>(openAPI.getComponents().getSchemas()));
        // Prune any schemas no longer referenced
        FilterUtil.applyFilter(new UnusedSchemaFilter(), openAPI);
    }
}
