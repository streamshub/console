package com.github.streamshub.console.api.model.jsonapi;

import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.github.streamshub.console.api.support.OASModelFilter;

/**
 * Marker class used to allow nodes in the OpenAPI model to be pruned. For
 * example, classes that extend JsonApiResource and do not use the
 * `relationships` property may use this as the JsonApiResource generic type
 * parameter that represents the `relationships`.
 */
@Schema(extensions = @Extension(name = OASModelFilter.REMOVE, parseValue = true, value = "true"))
public final class None {
}
