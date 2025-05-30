package com.github.streamshub.console.api.model.jsonapi;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Base class for all JSON API root request and response bodies.
 *
 * @see <a href="https://jsonapi.org/format/#document-structure">JSON API Document Structure, 7.1 Top Level</a>
 */
@JsonInclude(value = Include.NON_NULL)
public abstract class JsonApiRoot extends JsonApiBase {

    @Schema(implementation = JsonApiResource[].class, requiredProperties = { "type", "id" })
    private List<JsonApiResource<?, ?>> included;

    @JsonProperty
    public List<JsonApiResource<?, ?>> included() { // NOSONAR - wild-card response is necessary
        return included;
    }

    public void addIncluded(JsonApiResource<?, ?> resource) {
        if (included == null) {
            included = new ArrayList<>();
        }

        // Do not add duplicates
        var resourceId = resource.identifier();

        if (included.stream().noneMatch(i -> i.hasIdentifier(resourceId))) {
            included.add(resource);
        }
    }
}
