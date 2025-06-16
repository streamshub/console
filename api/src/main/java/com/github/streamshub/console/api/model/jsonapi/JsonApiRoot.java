package com.github.streamshub.console.api.model.jsonapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * Base class for all JSON API root request and response bodies.
 *
 * @see <a href="https://jsonapi.org/format/#document-structure">JSON API Document Structure, 7.1 Top Level</a>
 */
@JsonInclude(value = Include.NON_NULL)
public abstract class JsonApiRoot extends JsonApiBase {
}
