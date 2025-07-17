package com.github.streamshub.console.api.support;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class JacksonObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Inject
    FieldFilter fieldFilter;

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.setFilterProvider(new SimpleFilterProvider() {
            private static final long serialVersionUID = 1L;

            @Override
            public PropertyFilter findPropertyFilter(Object filterId, Object valueToFilter) {
                return fieldFilter.getTypedFilter(filterId.toString());
            }
        });
    }

}
