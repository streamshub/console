package com.github.streamshub.console.dependents;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConsoleDeploymentTest {

    @Test
    void testDefaultAllowedPrefixIsTheDefaultImageRepositoryPath() {
        assertEquals("quay.io/streamshub/",
                ConsoleDeployment.defaultAllowedPrefix("quay.io/streamshub/console-api:0.12.10"));
    }

    @Test
    void testDefaultAllowedPrefixWithoutSlashPermitsOnlyTheDefaultImage() {
        String prefix = ConsoleDeployment.defaultAllowedPrefix("an_image:a_tag");

        assertEquals("an_image:a_tag", prefix);
    }
}
