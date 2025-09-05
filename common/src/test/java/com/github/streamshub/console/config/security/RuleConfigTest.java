package com.github.streamshub.console.config.security;

import java.util.Arrays;
import java.util.stream.Stream;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RuleConfigTest {

    RuleConfig config;
    Validator validator;

    @BeforeEach
    void setup() {
        config = new RuleConfig();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void testDefaultObjectStateInvalid() {
        var violations = validator.validate(config);

        assertEquals(2, violations.size());
        Stream.of("resources", "privileges").forEach(name -> {
            var violation = violations.stream()
                .filter(v -> v.getPropertyPath().iterator().next().getName().equals(name))
                .findFirst()
                .orElseThrow();
            assertThat(violation.getMessage(), containsString("must not be empty"));
        });
    }

    @Test
    void testNullEntriesInvalid() {
        config.setResources(Arrays.asList((String) null));
        config.setResourceNames(Arrays.asList((String) null));
        config.setPrivileges(Arrays.asList((Privilege) null));

        var violations = validator.validate(config);

        assertEquals(3, violations.size());
        Stream.of("resources", "resourceNames", "privileges").forEach(name -> {
            var violation = violations.stream()
                .filter(v -> v.getPropertyPath().iterator().next().getName().equals(name))
                .findFirst()
                .orElseThrow();
            assertThat(violation.getMessage(), containsString("must not be null"));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "t",
        "topic-name",
        "topic-wildcard-*",
        "/topic-regex-(?:x|y|z)/",
        "/ ", // treated as a literal
        " /", // treated as a literal
    })
    void testResourceNamesValid(String resourceName) {
        config.setResources(Arrays.asList("topics"));
        config.setResourceNames(Arrays.asList(resourceName));
        config.setPrivileges(Arrays.asList(Privilege.ALL));

        var violations = validator.validate(config);
        assertEquals(0, violations.size());
    }

    @Test
    void testResourceNamesRegeInvalid() {
        config.setResources(Arrays.asList("topics"));
        config.setResourceNames(Arrays.asList("/(?:dangling-paren/"));
        config.setPrivileges(Arrays.asList(Privilege.ALL));

        var violations = validator.validate(config);
        assertEquals(1, violations.size());
        var violation = violations.stream()
                .filter(v -> v.getPropertyPath().iterator().next().getName().equals("resourceNames"))
                .findFirst()
                .orElseThrow();
        assertThat(violation.getMessage(), is(RuleConfig.INVALID_RESOURCE_NAME));
    }
}
