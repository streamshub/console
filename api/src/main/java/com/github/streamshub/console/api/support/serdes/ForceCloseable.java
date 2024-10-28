package com.github.streamshub.console.api.support.serdes;

import java.io.IOException;

/**
 * The de-/serializers in this package are re-used between requests and have made
 * their {@code close} methods no-ops. This interface is implemented by each to perform
 * the actual close operations when a {@link com.github.streamshub.console.api.support.KafkaContext}
 * is disposed.
 */
public interface ForceCloseable {

    void forceClose() throws IOException;

}
