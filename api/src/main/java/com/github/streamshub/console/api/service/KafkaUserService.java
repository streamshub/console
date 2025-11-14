package com.github.streamshub.console.api.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

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
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.config.security.ResourceTypes;
import com.github.streamshub.console.support.Identifiers;

import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.user.KafkaUserAuthentication;
import io.strimzi.api.kafka.model.user.KafkaUserAuthorizationSimple;
import io.strimzi.api.kafka.model.user.KafkaUserStatus;
import io.strimzi.api.kafka.model.user.acl.AclOperation;
import io.strimzi.api.kafka.model.user.acl.AclResourcePatternType;
import io.strimzi.api.kafka.model.user.acl.AclRuleGroupResource;
import io.strimzi.api.kafka.model.user.acl.AclRuleResource;
import io.strimzi.api.kafka.model.user.acl.AclRuleTopicResource;
import io.strimzi.api.kafka.model.user.acl.AclRuleTransactionalIdResource;

@ApplicationScoped
public class KafkaUserService {

    private static final Supplier<RuntimeException> NO_SUCH_USER =
            () -> new NotFoundException("No such KafkaUser");

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
                        .filter(permissionService.permitted(
                                ResourceTypes.Kafka.USERS,
                                Privilege.LIST,
                                r -> r.getMetadata().getName()))
                        .map(this::createUser)
                        .filter(listSupport.filter(KafkaUser.class))
                        .map(listSupport::tally)
                        .filter(listSupport::betweenCursors)
                        .sorted(listSupport.getSortComparator())
                        .dropWhile(listSupport::beforePageBegin)
                        .takeWhile(listSupport::pageCapacityAvailable)
                        .map(addPrivileges())
                        .toList()
                );
        }

        return CompletableFuture.completedStage(Collections.emptyList());
    }

    public CompletionStage<KafkaUser> describeUser(String userId) {
        Kafka kafkaCluster = kafkaContext.resource();
        String[] idParts = Identifiers.decode(userId);

        if (idParts.length != 2) {
            return CompletableFuture.failedStage(NO_SUCH_USER.get());
        }

        String namespace = idParts[0];
        String name = idParts[1];

        if (kafkaCluster != null && namespace.equals(kafkaCluster.getMetadata().getNamespace())) {
            var kafkaMeta = kafkaCluster.getMetadata();

            return strimziService.getKafkaUser(namespace, name, kafkaMeta.getName())
                .thenApply(optUser -> optUser
                        .map(this::createUser)
                        .map(addPrivileges())
                        .orElseThrow(NO_SUCH_USER));
        }

        return CompletableFuture.failedStage(NO_SUCH_USER.get());
    }

    private UnaryOperator<KafkaUser> addPrivileges() {
        return permissionService.addPrivileges(ResourceTypes.Kafka.USERS, KafkaUser::name);
    }

    private KafkaUser createUser(io.strimzi.api.kafka.model.user.KafkaUser userResource) {
        final var userMeta = userResource.getMetadata();
        final var userSpec = userResource.getSpec();
        final var userStatus = Optional.ofNullable(userResource.getStatus());

        final String namespace = userMeta.getNamespace();
        final String name = userMeta.getName();
        final String userId = Identifiers.encode("", namespace, name);

        KafkaUser user = KafkaUser.fromId(userId);
        KafkaUser.Attributes attr = user.getAttributes();
        attr.setNamespace(namespace);
        attr.setName(name);
        attr.setCreationTimestamp(userResource.getMetadata().getCreationTimestamp());
        attr.setUsername(userStatus.map(KafkaUserStatus::getUsername).orElse(name));
        attr.setAuthenticationType(Optional.ofNullable(userSpec.getAuthentication())
                .map(KafkaUserAuthentication::getType)
                .orElse(null));

        Optional.ofNullable(userSpec.getAuthorization())
            .map(KafkaUserAuthorizationSimple.class::cast)
            .map(KafkaUserAuthorizationSimple::getAcls)
            .map(acls -> {
                @SuppressWarnings("deprecation")
                var accessControl = acls
                    .stream()
                    .map(rule -> new KafkaUser.KafkaUserAccessControl(
                        mapResourceType(rule.getResource()),
                        mapResourceName(rule.getResource()),
                        mapResourcePatternType(rule.getResource()),
                        rule.getHost(),
                        mapOperations(rule.getOperation(), rule.getOperations()),
                        rule.getType().toValue()
                    ))
                    .toList();

                return new KafkaUser.KafkaUserAuthorization(accessControl);
            })
            .ifPresent(attr::setAuthorization);

        return user;
    }

    private static String mapResourceType(AclRuleResource resource) {
        return resource != null ? resource.getType() : null;
    }

    private static String mapResourcePatternType(AclRuleResource resource) {
        if (resource instanceof AclRuleTopicResource topic) {
            return optionalPatternType(topic.getPatternType());
        } else if (resource instanceof AclRuleGroupResource group) {
            return optionalPatternType(group.getPatternType());
        } else if (resource instanceof AclRuleTransactionalIdResource txId) {
            return optionalPatternType(txId.getPatternType());
        }

        // Includes AclRuleClusterResource or any other unexpected type
        return null;
    }

    private static String optionalPatternType(AclResourcePatternType patternType) {
        return patternType != null ? patternType.toValue() : null;
    }

    private static String mapResourceName(AclRuleResource resource) {
        if (resource instanceof AclRuleTopicResource topic) {
            return topic.getName();
        } else if (resource instanceof AclRuleGroupResource group) {
            return group.getName();
        } else if (resource instanceof AclRuleTransactionalIdResource txId) {
            return txId.getName();
        }

        // Includes AclRuleClusterResource or any other unexpected type
        return null;
    }

    private static List<String> mapOperations(AclOperation operation, List<AclOperation> operations) {
        List<String> result = new ArrayList<>();

        if (operation != null) {
            result.add(operation.toValue());
        }

        if (operations != null) {
            operations.stream().map(AclOperation::toValue).forEach(result::add);
        }

        return result;
    }
}
