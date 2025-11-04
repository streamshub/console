package com.github.streamshub.console.api.service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.model.KafkaUser;
import com.github.streamshub.console.api.security.PermissionService;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.security.ResourceTypes;
import com.github.streamshub.console.support.Identifiers;

import io.strimzi.api.kafka.model.kafka.Kafka;

@ApplicationScoped
public class KafkaUserService {

    @Inject
    Logger logger;

    @Inject
    ThreadContext threadContext;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    KafkaContext kafkaContext;

    @Inject
    StrimziResourceService strimziService;

    @Inject
    PermissionService permissionService;

    public CompletionStage<List<KafkaUser>> listUsers(ListRequestContext<KafkaUser> listSupport) {
        Kafka kafkaCluster = kafkaContext.resource();

        if (kafkaCluster != null) {
            var kafkaMeta = kafkaCluster.getMetadata();
            String namespace = kafkaMeta.getNamespace();

            return strimziService.getKafkaUsers(namespace, kafkaMeta.getName())
                .thenApply(userResources -> userResources.stream()
                        .map(userResource -> {
                            String name = userResource.getMetadata().getName();
                            KafkaUser user = KafkaUser.fromId(Identifiers.encode(namespace, name));
                            user.getAttributes().setNamespace(namespace);
                            user.getAttributes().setName(name);
                            user.getAttributes().setCreationTimestamp(userResource.getMetadata().getCreationTimestamp());
                            return user;
                        })
                        .filter(listSupport.filter(KafkaUser.class))
                        .map(listSupport::tally)
                        .filter(listSupport::betweenCursors)
                        .sorted(listSupport.getSortComparator())
                        .dropWhile(listSupport::beforePageBegin)
                        .takeWhile(listSupport::pageCapacityAvailable)
                        .map(permissionService.addPrivileges(ResourceTypes.Kafka.USERS, KafkaUser::name))
                        .toList()
                );
        }

        return CompletableFuture.completedStage(Collections.emptyList());
    }

    public CompletionStage<KafkaUser> describeUser(String userId) {
        Kafka kafkaCluster = kafkaContext.resource();
        String[] idParts = Identifiers.decode(userId);
        String namespace = idParts[0];
        String name = idParts[1];

        if (kafkaCluster != null && namespace.equals(kafkaCluster.getMetadata().getNamespace())) {
            var kafkaMeta = kafkaCluster.getMetadata();

            return strimziService.getKafkaUser(namespace, name, kafkaMeta.getName())
                .thenApply(optUser -> optUser
                        .map(userResource -> {
                            KafkaUser user = KafkaUser.fromId(Identifiers.encode(namespace, name));
                            user.getAttributes().setNamespace(namespace);
                            user.getAttributes().setName(name);
                            user.getAttributes().setCreationTimestamp(userResource.getMetadata().getCreationTimestamp());
                            return user;
                        })
                        .map(permissionService.addPrivileges(ResourceTypes.Kafka.USERS, KafkaUser::name))
                        .orElseThrow(() -> new NotFoundException("No such KafkaUser: " + name)));
        }

        return CompletableFuture.failedStage(new NotFoundException("No such KafkaUser: " + name));
    }
}
