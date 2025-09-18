package com.github.streamshub.console.api.service;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.jboss.logging.Logger;

import com.github.streamshub.console.api.model.Condition;
import com.github.streamshub.console.api.model.KafkaRebalance;
import com.github.streamshub.console.api.security.PermissionService;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.config.security.ResourceTypes;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.strimzi.api.ResourceAnnotations;
import io.strimzi.api.ResourceLabels;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceMode;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceSpec;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceState;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalanceStatus;

@ApplicationScoped
public class KafkaRebalanceService {

    @Inject
    Logger logger;

    @Inject
    KubernetesClient client;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    KafkaContext kafkaContext;

    @Inject
    PermissionService permissionService;

    public List<KafkaRebalance> listRebalances(ListRequestContext<KafkaRebalance> listSupport) {
        final Map<String, Integer> statuses = new HashMap<>();
        listSupport.meta().put("summary", Map.of("statuses", statuses));

        permissionService.addPrivileges(listSupport.meta(), ResourceTypes.Kafka.REBALANCES, null);

        return rebalanceResources()
                .filter(permissionService.permitted(
                        ResourceTypes.Kafka.REBALANCES,
                        Privilege.LIST,
                        r -> r.getMetadata().getName()))
                .map(this::toKafkaRebalance)
                .map(rebalance -> tallyStatus(statuses, rebalance))
                .filter(listSupport.filter(KafkaRebalance.class))
                .map(listSupport::tally)
                .filter(listSupport::betweenCursors)
                .sorted(listSupport.getSortComparator())
                .dropWhile(listSupport::beforePageBegin)
                .takeWhile(listSupport::pageCapacityAvailable)
                .map(permissionService.addPrivileges(ResourceTypes.Kafka.REBALANCES, KafkaRebalance::name))
                .toList();
    }

    public KafkaRebalance getRebalance(String id) {
        return findRebalance(id)
            .map(this::toKafkaRebalance)
            .map(permissionService.addPrivileges(ResourceTypes.Kafka.REBALANCES, KafkaRebalance::name))
            .orElseThrow(() -> new NotFoundException("No such Kafka rebalance resource"));
    }

    public KafkaRebalance patchRebalance(String id, KafkaRebalance rebalance) {
        return findRebalance(id)
            .map(resource -> {
                var annotations = resource.getMetadata().getAnnotations();
                String action = rebalance.action();

                if (action != null) {
                    annotations.put(ResourceAnnotations.ANNO_STRIMZI_IO_REBALANCE, action);
                } else {
                    annotations.remove(ResourceAnnotations.ANNO_STRIMZI_IO_REBALANCE);
                }

                return client.resource(resource).patch();
            })
            .map(this::toKafkaRebalance)
            .orElseThrow(() -> new NotFoundException("No such Kafka rebalance resource"));
    }

    KafkaRebalance toKafkaRebalance(io.strimzi.api.kafka.model.rebalance.KafkaRebalance resource) {
        KafkaRebalanceSpec rebalanceSpec = resource.getSpec();
        Optional<KafkaRebalanceStatus> rebalanceStatus = Optional.ofNullable(resource.getStatus());
        Optional<KafkaRebalanceState> state = rebalanceStatus
                .map(KafkaRebalanceStatus::getConditions)
                .map(java.util.Collection::stream)
                .orElseGet(Stream::empty)
                .map(io.strimzi.api.kafka.model.common.Condition::getType)
                .filter(Arrays.stream(KafkaRebalanceState.values()).map(Enum::name).toList()::contains)
                .map(KafkaRebalanceState::valueOf)
                .findFirst();

        String id = Base64.getUrlEncoder().encodeToString(Cache.metaNamespaceKeyFunc(resource).getBytes(StandardCharsets.UTF_8));
        KafkaRebalance rebalance = new KafkaRebalance(id);
        rebalance.name(resource.getMetadata().getName());
        rebalance.namespace(resource.getMetadata().getNamespace());
        rebalance.creationTimestamp(resource.getMetadata().getCreationTimestamp());
        rebalance.status(state.map(Enum::name).orElse(null));
        rebalance.mode(Optional.ofNullable(rebalanceSpec.getMode()).map(KafkaRebalanceMode::toValue).orElse(null));
        rebalance.brokers(rebalanceSpec.getBrokers());
        rebalance.goals(rebalanceSpec.getGoals());
        rebalance.skipHardGoalCheck(rebalanceSpec.isSkipHardGoalCheck());
        rebalance.rebalanceDisk(rebalanceSpec.isRebalanceDisk());
        rebalance.excludedTopics(rebalanceSpec.getExcludedTopics());
        rebalance.concurrentPartitionMovementsPerBroker(rebalanceSpec.getConcurrentPartitionMovementsPerBroker());
        rebalance.concurrentIntraBrokerPartitionMovements(rebalanceSpec.getConcurrentIntraBrokerPartitionMovements());
        rebalance.concurrentLeaderMovements(rebalanceSpec.getConcurrentLeaderMovements());
        rebalance.replicationThrottle(rebalanceSpec.getReplicationThrottle());
        rebalance.replicaMovementStrategies(rebalanceSpec.getReplicaMovementStrategies());

        rebalanceStatus.map(KafkaRebalanceStatus::getSessionId)
                .ifPresent(rebalance::sessionId);
        rebalanceStatus.map(KafkaRebalanceStatus::getOptimizationResult)
                .ifPresent(rebalance.optimizationResult()::putAll);
        rebalanceStatus.map(KafkaRebalanceStatus::getConditions)
                .map(conditions -> conditions.stream().map(Condition::new).toList())
                .ifPresent(rebalance::conditions);

        var annotations = resource.getMetadata().getAnnotations();
        var autoApproval = Boolean.parseBoolean(annotations.get(ResourceAnnotations.ANNO_STRIMZI_IO_REBALANCE_AUTOAPPROVAL));
        rebalance.autoApproval(autoApproval);

        state.map(KafkaRebalanceState::getValidAnnotations)
                .map(allowed -> allowed.stream().map(Enum::name).toList())
                .ifPresent(rebalance.allowedActions()::addAll);

        return rebalance;
    }

    KafkaRebalance tallyStatus(Map<String, Integer> statuses, KafkaRebalance rebalance) {
        String status = rebalance.status();
        if (status != null) {
            statuses.compute(rebalance.status(), (k, v) -> v == null ? 1 : v + 1);
        }
        return rebalance;
    }

    private Optional<io.strimzi.api.kafka.model.rebalance.KafkaRebalance> findRebalance(String id) {
        String decodedId = new String(Base64.getUrlDecoder().decode(id));
        String[] idElements = decodedId.split("/");

        if (idElements.length != 2) {
            return Optional.empty();
        }

        Kafka owner = kafkaContext.resource();

        if (owner == null) {
            return Optional.empty();
        }

        var resource = client.resources(io.strimzi.api.kafka.model.rebalance.KafkaRebalance.class)
            .inNamespace(idElements[0])
            .withName(idElements[1])
            .get();

        if (resource == null) {
            return Optional.empty();
        }

        String ownedByCluster = resource.getMetadata().getLabels().get(ResourceLabels.STRIMZI_CLUSTER_LABEL);

        if (Objects.equals(ownedByCluster, owner.getMetadata().getName())) {
            return Optional.of(resource);
        }

        return Optional.empty();
    }

    private Stream<io.strimzi.api.kafka.model.rebalance.KafkaRebalance> rebalanceResources() {
        Kafka owner = kafkaContext.resource();

        if (owner == null) {
            return Stream.empty();
        }

        return client.resources(io.strimzi.api.kafka.model.rebalance.KafkaRebalance.class)
                .inNamespace(owner.getMetadata().getNamespace())
                .withLabel(ResourceLabels.STRIMZI_CLUSTER_LABEL, owner.getMetadata().getName())
                .list()
                .getItems()
                .stream()
                .filter(Predicate.not(this::isTemplate));
    }

    private boolean isTemplate(io.strimzi.api.kafka.model.rebalance.KafkaRebalance rebalance) {
        return Optional.ofNullable(rebalance.getMetadata().getAnnotations())
            .map(annotations -> annotations.get(ResourceAnnotations.ANNO_STRIMZI_IO_REBALANCE_TEMPLATE))
            .map(Boolean::valueOf)
            .orElse(false);
    }
}
