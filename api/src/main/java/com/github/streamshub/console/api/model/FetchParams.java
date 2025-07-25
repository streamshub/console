package com.github.streamshub.console.api.model;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

public class FetchParams {

    public static final String INCLUDE_PARAM = "include";

    @QueryParam(INCLUDE_PARAM)
    @Parameter(name = INCLUDE_PARAM,
        in = ParameterIn.QUERY,
        explode = Explode.FALSE,
        schema = @Schema(implementation = String[].class),
        description = """
            Comma-separated (U+002C COMMA, ",") list of relationship paths.
            A relationship path is a dot-separated (U+002E FULL-STOP, ".") list
            of relationship names. An empty value indicates that no related resources
            should be returned.

            Unrecognized relationship names will be ignored.
            """)
    String include;

    List<String> includeEntries;

    public String getRawInclude() {
        return include;
    }

    public List<String> getIncludeEntries() {
        if (includeEntries == null) {
            includeEntries = Optional.ofNullable(include)
                .map(s -> s.split(","))
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(String::trim)
                .toList();
        }

        return includeEntries;
    }

    public boolean includes(String relationshipName) {
        return getIncludeEntries().contains(relationshipName);
    }
}
