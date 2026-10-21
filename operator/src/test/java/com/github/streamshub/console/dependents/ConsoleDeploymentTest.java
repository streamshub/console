package com.github.streamshub.console.dependents;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.hamcrest.Matcher;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import com.github.streamshub.console.ReconciliationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ConsoleDeploymentTest {

    static Matcher<String> findsPattern(Pattern pattern) {
        return new org.hamcrest.text.MatchesPattern(pattern) {
            @Override
            protected boolean matchesSafely(String item) {
                return pattern.matcher(item).find();
            }
        };
    }

    @Test
    void testDefaultAllowedPrefixIsTheDefaultImageRepositoryPath() {
        var pattern = ConsoleDeployment.defaultAllowedPrefix("quay.io/streamshub/console-api:0.12.10");
        assertThat(pattern.toString(), is("^\\Qquay.io/streamshub/\\E"));
        assertThat("quay.io/streamshub/my-image:1", findsPattern(pattern));
        assertThat("quay.io/evil-streamshub/my-evil-image:1", not(findsPattern(pattern)));
    }

    @Test
    void testDefaultAllowedPrefixWithoutSlashPermitsOnlyTheDefaultImage() {
        var pattern = ConsoleDeployment.defaultAllowedPrefix("an_image:a_tag");
        assertThat(pattern.toString(), is("^\\Qan_image:a_tag\\E$"));
        assertThat("an_image:a_tag", findsPattern(pattern));
        assertThat("an_image_evil:1", not(findsPattern(pattern)));
    }

    @Test
    void testAllowedImagePrefixesBlocksDefaultImage() {
        ConsoleDeployment target = new ConsoleDeployment();
        target.logger = mock(Logger.class);
        target.defaultAPIImage = "example.com/my-default-org/console-api:1";
        target.allowedImagePrefixes = Optional.of(List.of("example.com/my-allowed-org/"));
        target.initAllowedPrefixes();

        verify(target.logger).warnf(
                "Configured `%s` of %s blocks use of default API image `%s`",
                ConsoleDeployment.ALLOWED_IMAGE_PREFIXES,
                target.allowedImagePrefixes.get(),
                target.defaultAPIImage);

        ReconciliationException thrown = assertThrows(
                ReconciliationException.class,
                () -> ConsoleDeployment.validateImage(target.defaultAPIImage, target.allowedImagePatterns));

        assertEquals(
                "Console API image '%s' is not permitted. Allowed patterns: %s".formatted(
                        "example.com/my-default-org/console-api:1",
                        "[^\\Qexample.com/my-allowed-org/\\E]"
                ),
                thrown.getMessage());
    }
}
