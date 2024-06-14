package com.github.streamshub.console.api.support;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.server.authorizer.Authorizer;
import org.jboss.logging.Logger;

import io.strimzi.api.kafka.model.kafka.Kafka;

public class KafkaContext implements Closeable {

    private static final Logger LOGGER = Logger.getLogger(KafkaContext.class);
    public static final KafkaContext EMPTY = new KafkaContext(null, null, null);

    final Kafka resource;
    final Admin client;
    final Authorizer authorizer;

    public KafkaContext(Kafka resource, Admin client, Authorizer authorizer) {
        super();
        this.resource = resource;
        this.client = client;
        this.authorizer = authorizer;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof KafkaContext)) {
            return false;
        }

        KafkaContext other = (KafkaContext) obj;
        return Objects.equals(resource, other.resource)
                && Objects.equals(client, other.client)
                && Objects.equals(authorizer, other.authorizer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, client, authorizer);
    }

    @Override
    public void close() {
        if (authorizer != null) {
            try {
                authorizer.close();
            } catch (IOException e) {
                LOGGER.warn("Exception closing KafkaContext authorizer", e);
            }
        }

        if (client != null) {
            client.close();
        }
    }

    public Kafka resource() {
        return resource;
    }

    public Admin client() {
        return client;
    }

    public Authorizer authorizer() {
        return authorizer;
    }

    public boolean isActive() {
        return Objects.nonNull(resource);
    }

}
