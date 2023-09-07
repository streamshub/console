package com.github.eyefloaters.console.api.support;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.UriInfo;

import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

@RequestScoped
public class FieldFilter extends SimpleBeanPropertyFilter {

    public static final String FIELDS_DESCR = "List of attribute fields returned for each resource";

    @Inject
    UriInfo requestUri;

    /**
     * List of fields actually requested by the client, or the defaults if not
     * requested. This value is set in the resource methods via the consumer produced by
     * {@link #defaultFieldsConsumer() defaultFieldsConsumer}.
     */
    final AtomicReference<List<String>> requestedFields = new AtomicReference<>();

    @Produces
    @RequestScoped
    @Named("requestedFields")
    public Consumer<List<String>> defaultFieldsConsumer() {
        return requestedFields::set;
    }

    @Override
    protected boolean include(BeanPropertyWriter writer) {
        return isIncluded(writer);
    }

    @Override
    protected boolean include(PropertyWriter writer) {
        return isIncluded(writer);
    }

    boolean isIncluded(PropertyWriter writer) {
        return requestedFields.get().contains(writer.getName());
    }
}
