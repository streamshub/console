package com.github.streamshub.console.api.model.jsonapi;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Base class for all JSON API root request and response bodies.
 *
 * @see <a href="https://jsonapi.org/format/#document-structure">JSON API Document Structure, 7.1 Top Level</a>
 */
@JsonInclude(value = Include.NON_NULL)
public abstract class JsonApiRoot extends JsonApiBase {

    @Schema(hidden = true) // for future use
    private List<JsonApiResource<?, ?>> included;

    public List<JsonApiResource<?, ?>> included() { // NOSONAR - wild-card response is necessary
        return included;
    }

    public void addIncluded(JsonApiResource<?, ?> resource) {
        if (included == null) {
            included = new ArrayList<>();
        }
        included.add(resource);
    }
}
