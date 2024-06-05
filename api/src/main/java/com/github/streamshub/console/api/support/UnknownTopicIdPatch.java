package com.github.streamshub.console.api.support;

import java.util.function.Function;

import org.apache.kafka.common.errors.InvalidTopicException;
import org.apache.kafka.common.errors.UnknownTopicIdException;

/**
 * Utility to map an {@linkplain InvalidTopicException} to a
 * {@linkplain UnknownTopicIdException} in cases where a topic ID is not known
 * to the Kafka cluster. This works around KAFKA-15373 and may be removed
 * if/when the fix is merged, released, and this application uses for the
 * client.
 */
public class UnknownTopicIdPatch {

    private UnknownTopicIdPatch() {
    }

    public static <T extends Throwable> Throwable apply(Throwable error, Function<Throwable, T> mapper) {
        if (error instanceof InvalidTopicException) {
            // See KAFKA-15373
            return new UnknownTopicIdException(error.getMessage());
        }

        return mapper.apply(error);
    }

}
