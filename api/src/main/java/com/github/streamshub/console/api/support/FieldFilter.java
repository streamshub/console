package com.github.streamshub.console.api.support;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;

import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

@RequestScoped
public class FieldFilter {

    public static final String FIELDS_DESCR = "List of properties in attributes and relationships to be returned for the resource type";

    /**
     * List of fields actually requested by the client, or the defaults if not
     * requested. This value is set in the resource methods via the consumer produced by
     * {@link #defaultFieldsConsumer() defaultFieldsConsumer}.
     */
    final AtomicReference<List<String>> requestedFields = new AtomicReference<>();

    private final Map<String, List<String>> requestedTypedFields = new HashMap<>(2);

    @Produces
    @RequestScoped
    @Named("requestedFields")
    public Consumer<List<String>> defaultFieldsConsumer() {
        return requestedFields::set;
    }

    public void setTypedFields(Map<String, List<String>> fields) {
        requestedTypedFields.putAll(fields);
    }

    public PropertyFilter getTypedFilter(String type) {
        return new TypedFieldFilter(type);
    }

    public boolean isIncluded(String type, String propertyName) {
        return requestedTypedFields.get(type).contains(propertyName);
    }

    class TypedFieldFilter extends SimpleBeanPropertyFilter {
        final String type;

        public TypedFieldFilter(String type) {
            this.type = type;
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
            String propertyName = writer.getName();

            if (requestedTypedFields.containsKey(type)) {
                return FieldFilter.this.isIncluded(type, propertyName);
            } else {
                return requestedFields.get().contains(propertyName);
            }
        }
    }
}
