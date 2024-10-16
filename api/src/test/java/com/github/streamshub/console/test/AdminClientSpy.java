package com.github.streamshub.console.test;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import jakarta.enterprise.inject.literal.NamedLiteral;
import jakarta.enterprise.util.TypeLiteral;

import org.apache.kafka.clients.admin.Admin;
import org.mockito.Mockito;

import com.github.streamshub.console.api.ClientFactory;

import io.quarkus.test.junit.QuarkusMock;

/**
 * Provides a mechanism for integration tests to spy and override
 * particular Admin methods to, for example, force an error condition.
 *
 * See {@link #install(Consumer)} for usage.
 */
public final class AdminClientSpy {

    static final TypeLiteral<Function<Map<String, Object>, Admin>> CLIENT_BUILDER_TYPE_LITERAL =
            new TypeLiteral<Function<Map<String, Object>, Admin>>() {
                private static final long serialVersionUID = 1L;
            };

    static final TypeLiteral<UnaryOperator<Admin>> CLIENT_FILTER_TYPE_LITERAL =
            new TypeLiteral<UnaryOperator<Admin>>() {
                private static final long serialVersionUID = 1L;
            };

    static final String KAFKA_ADMIN_BUILDER = "kafkaAdminBuilder";
    static final String KAFKA_ADMIN_FILTER = "kafkaAdminFilter";

    /**
     * Create and install a spy Admin instance to be used for a request instead of
     * {@link ClientFactory#kafkaAdminBuilder}.
     *
     * @param adminSetup a consumer that accepts the Admin client for spying
     */
    public static void install(Consumer<Admin> adminSetup) {
        UnaryOperator<Admin> filter = client -> {
            client = Mockito.spy(client);
            adminSetup.accept(client);
            return client;
        };

        QuarkusMock.installMockForType(filter, CLIENT_FILTER_TYPE_LITERAL, NamedLiteral.of(KAFKA_ADMIN_FILTER));
    }

    /**
     * Create and install a spy Admin instance to be used for a request instead of
     * {@link ClientFactory#kafkaAdminBuilder}.
     *
     * @param configSetup a function that may optionally change the client configuration
     * @param adminSetup a consumer that accepts the Admin client for spying
     */
    public static void install(UnaryOperator<Map<String, Object>> configSetup, Consumer<Admin> adminSetup) {
        Function<Map<String, Object>, Admin> builder = config -> {
            Admin client = Mockito.spy(Admin.create(configSetup.apply(config)));
            adminSetup.accept(client);
            return client;
        };

        QuarkusMock.installMockForType(builder, CLIENT_BUILDER_TYPE_LITERAL, NamedLiteral.of(KAFKA_ADMIN_BUILDER));
    }

    private AdminClientSpy() {
        // No instances
    }
}
