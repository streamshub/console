package com.github.eyefloaters.console.api.model;

import jakarta.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.Explode;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;

public class ListFetchParams {

    @QueryParam("sort")
    @Parameter(name = "sort",
        in = ParameterIn.QUERY,
        explode = Explode.FALSE,
        schema = @Schema(implementation = String[].class),
        description = """
            Comma-separated list of fields by which the result set will be ordered.
            The sort order for each sort field will be ascending unless it is prefixed
            with a minus (U+002D HYPHEN-MINUS, "-"), in which case it will be descending.

            Unrecognized field names or fields of type `object` or `array` will be
            ignored.
            """)
    String sort;

    public String getSort() {
        return sort;
    }

}
