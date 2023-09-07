package com.github.eyefloaters.console.api.support;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

import io.quarkus.jackson.ObjectMapperCustomizer;

@Singleton
public class JacksonObjectMapperCustomizer implements ObjectMapperCustomizer {

    @Inject
    FieldFilter fieldFilter;

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.setFilterProvider(new SimpleFilterProvider()
                .addFilter("fieldFilter", fieldFilter));
    }

}
