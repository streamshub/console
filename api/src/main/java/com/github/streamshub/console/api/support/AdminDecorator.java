package com.github.streamshub.console.api.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.resource.PatternType;
import org.apache.kafka.common.resource.ResourcePattern;
import org.apache.kafka.common.resource.ResourceType;
import org.apache.kafka.common.security.auth.KafkaPrincipal;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.server.authorizer.Action;
import org.apache.kafka.server.authorizer.AuthorizableRequestContext;
import org.apache.kafka.server.authorizer.AuthorizationResult;
import org.eclipse.microprofile.context.ThreadContext;

import io.quarkus.security.identity.SecurityIdentity;
import io.vertx.core.http.HttpServerRequest;

@Dependent
public class AdminDecorator {

    @Inject
    KafkaContext kafkaContext;

    @Inject
    HttpServerRequest request;

    @Inject
    SecurityIdentity securityIdentity;

    /**
     * ThreadContext of the request thread. This is used to execute asynchronous
     * tasks to allow access to request-scoped beans such as an injected
     * {@linkplain Admin Admin client}
     */
    @Inject
    ThreadContext threadContext;

    AuthorizableRequestContext requestContext;

    @PostConstruct
    void setup() {
        requestContext = new AuthorizableRequestContext() {
            @Override
            public String listenerName() {
                return null;
            }

            @Override
            public SecurityProtocol securityProtocol() {
                return null;
            }

            @Override
            public KafkaPrincipal principal() {
                return new KafkaPrincipal(KafkaPrincipal.USER_TYPE, securityIdentity.getPrincipal().getName());
            }

            @Override
            public InetAddress clientAddress() {
                return Optional.ofNullable(request.remoteAddress().hostAddress()).map(address -> {
                    try {
                        return InetAddress.getByName(address);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }).orElse(null);
            }

            @Override
            public int requestType() {
                return 0;
            }

            @Override
            public int requestVersion() {
                return 0;
            }

            @Override
            public String clientId() {
                return null;
            }

            @Override
            public int correlationId() {
                return 0;
            }
        };
    }

    boolean authorized(ResourceType type, String name, AclOperation operation) {
        Action action = new Action(operation, new ResourcePattern(type, name, PatternType.LITERAL), 0, false, false);
        return AuthorizationResult.ALLOWED == kafkaContext.authorizer().authorize(requestContext, List.of(action)).get(0);
    }

    public CompletionStage<List<TopicListing>> listTopics(ListTopicsOptions options) {
        return kafkaContext.admin()
                .listTopics(options)
                .listings()
                .toCompletionStage()
                .thenApplyAsync(topics -> topics.stream()
                        .filter(t -> authorized(ResourceType.TOPIC, t.name(), AclOperation.DESCRIBE))
                        .toList(), threadContext.currentContextExecutor());
    }
}
