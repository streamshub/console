package io.streamshub.console.api.v1alpha1;

import io.fabric8.kubernetes.model.annotation.Group;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * {@code @CSVMetadata} is runtime-retained, so loading {@link Console}'s annotations must keep
 * working even though this module only depends on {@code quarkus-operator-sdk-annotations} as
 * {@code provided} (i.e. it is not forced onto a downstream consumer's runtime classpath).
 */
class ConsoleReflectionTest {

    @Test
    void consoleAnnotationsAreReadable() {
        assertDoesNotThrow(() -> Console.class.getAnnotations());
        Group group = Console.class.getAnnotation(Group.class);
        assertNotNull(group);
        assertEquals("console.streamshub.github.com", group.value());
    }
}
