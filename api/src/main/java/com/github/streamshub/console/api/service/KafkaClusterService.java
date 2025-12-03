package com.github.streamshub.console.api.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.clients.admin.QuorumInfo;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.Annotations;
import com.github.streamshub.console.api.model.Condition;
import com.github.streamshub.console.api.model.KafkaCluster;
import com.github.streamshub.console.api.model.KafkaListener;
import com.github.streamshub.console.api.model.jsonapi.Identifier;
import com.github.streamshub.console.api.security.PermissionService;
import com.github.streamshub.console.api.support.Holder;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.api.support.MetadataQuorumSupport;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.config.security.ResourceTypes;
import com.github.streamshub.console.support.KafkaConfigs;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.strimzi.api.ResourceAnnotations;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaBuilder;
import io.strimzi.api.kafka.model.kafka.KafkaClusterSpec;
import io.strimzi.api.kafka.model.kafka.KafkaSpec;
import io.strimzi.api.kafka.model.kafka.KafkaStatus;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListener;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerConfiguration;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationCustom;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationScramSha512;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;
import io.strimzi.api.kafka.model.kafka.listener.ListenerStatus;

import static com.github.streamshub.console.api.BlockingSupplier.get;

@ApplicationScoped
public class KafkaClusterService {

    private static final String AUTHN_KEY = "authentication";
    private static final String AUTHN_METHOD_KEY = "method";

    @Inject
    Logger logger;

    @Inject
    KubernetesClient client;

    /**
     * ThreadContext of the request thread. This is used to execute asynchronous
     * tasks to allow access to request-scoped beans.
     */
    @Inject
    ThreadContext threadContext;

    @Inject
    Holder<SharedIndexInformer<Kafka>> kafkaInformer;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    MetricsService metricsService;

    @Inject
    NodeService nodeService;

    @Inject
    /**
     * All Kafka contexts known to the application
     */
    Map<String, KafkaContext> kafkaContexts;

    @Inject
    /**
     * Kafka context for a single-Kafka request.
     * E.g. {@linkplain #describeCluster(List) describeCluster}.
     */
    KafkaContext kafkaContext;

    @Inject
    PermissionService permissionService;

    boolean listUnconfigured = false;
    Predicate<KafkaCluster> includeAll = k -> listUnconfigured;

    public List<KafkaCluster> listClusters(ListRequestContext<KafkaCluster> listSupport) {
        List<KafkaCluster> kafkaResources = kafkaResources()
                .map(this::toKafkaCluster)
                // Hide unconfigured clusters for now.
                .filter(includeAll.or(KafkaCluster::isConfigured))
                .toList();

        Map<String, KafkaCluster> configuredClusters = kafkaContexts
                .entrySet()
                .stream()
                .map(ctx -> {
                    String id = ctx.getKey();
                    var config = ctx.getValue().clusterConfig();

                    return kafkaResources.stream()
                        .filter(k -> Objects.equals(k.name(), config.getName()))
                        .filter(k -> Objects.equals(k.namespace(), config.getNamespace()))
                        .map(k -> addKafkaContextData(k, ctx.getValue()))
                        .findFirst()
                        .orElseGet(() -> addKafkaContextData(KafkaCluster.fromId(id), ctx.getValue()));
                })
                .collect(Collectors.toMap(KafkaCluster::getId, Function.identity()));

        List<KafkaCluster> otherClusters = kafkaResources.stream()
                .filter(k -> !configuredClusters.containsKey(k.getId()))
                .toList();

        permissionService.addPrivileges(listSupport.meta(), ResourceTypes.Global.KAFKAS, null);

        return Stream.concat(configuredClusters.values().stream(), otherClusters.stream())
                .filter(permissionService.permitted(ResourceTypes.Global.KAFKAS, Privilege.LIST, KafkaCluster::getId))
                .filter(listSupport.filter(KafkaCluster.class))
                .map(listSupport::tally)
                .filter(listSupport::betweenCursors)
                .sorted(listSupport.getSortComparator())
                .dropWhile(listSupport::beforePageBegin)
                .takeWhile(listSupport::pageCapacityAvailable)
                .map(this::setManaged)
                .map(permissionService.addPrivileges(ResourceTypes.Global.KAFKAS, KafkaCluster::getId))
                .toList();
    }

    public CompletionStage<KafkaCluster> describeCluster(List<String> fields) {
        Admin adminClient = kafkaContext.admin();
        DescribeClusterOptions options = new DescribeClusterOptions()
                .includeAuthorizedOperations(fields.contains(KafkaCluster.Fields.AUTHORIZED_OPERATIONS));
        var clusterResult = adminClient.describeCluster(options);
        var quorumResult = MetadataQuorumSupport.quorumInfo(adminClient.describeMetadataQuorum());

        return CompletableFuture.allOf(
                clusterResult.authorizedOperations().toCompletionStage().toCompletableFuture(),
                clusterResult.clusterId().toCompletionStage().toCompletableFuture(),
                quorumResult)
            .thenApplyAsync(
                    nothing -> new KafkaCluster(kafkaContext.clusterId(), enumNames(get(clusterResult::authorizedOperations))),
                    threadContext.currentContextExecutor())
            .thenComposeAsync(
                    cluster -> addNodes(cluster, clusterResult, quorumResult),
                    threadContext.currentContextExecutor())
            .thenApplyAsync(this::addKafkaContextData, threadContext.currentContextExecutor())
            .thenApply(this::addKafkaResourceData)
            .thenCompose(cluster -> addMetrics(cluster, fields))
            .thenApply(this::setManaged)
            .thenApplyAsync(
                    permissionService.addPrivileges(ResourceTypes.Global.KAFKAS, KafkaCluster::getId),
                    threadContext.currentContextExecutor());
    }

    private CompletionStage<KafkaCluster> addNodes(KafkaCluster cluster, DescribeClusterResult clusterResult, CompletableFuture<QuorumInfo> quorumResult) {
        return nodeService.getNodes(clusterResult, quorumResult)
            .thenAccept(nodes -> {
                var identifiers = nodes.stream().map(n -> new Identifier("nodes", n.getId())).toList();
                var nodesRelationship = cluster.nodes();
                nodesRelationship.getData().addAll(identifiers);
                nodesRelationship.addMeta("summary", nodeService.summarize(nodes));
                nodesRelationship.addMeta("count", identifiers.size());
            })
            .thenApply(nothing -> cluster);
    }

    public KafkaCluster patchCluster(KafkaCluster cluster) {
        Kafka resource = kafkaContext.resource();

        if (resource != null) {
            var paused = Optional.ofNullable(cluster.reconciliationPaused())
                    .map(Object::toString)
                    .orElse(null);

            var patch = new KafkaBuilder()
                    .withNewMetadata()
                        .withNamespace(resource.getMetadata().getNamespace())
                        .withName(resource.getMetadata().getName())
                        .withAnnotations(resource.getMetadata().getAnnotations())
                    .endMetadata()
                    .withSpec(resource.getSpec());

            if (paused != null) {
                patch.editMetadata()
                    .addToAnnotations(ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION, paused)
                    .endMetadata();
            } else {
                patch.editMetadata()
                    .removeFromAnnotations(ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION)
                    .endMetadata();
            }

            resource = client.resource(patch.build()).forceConflicts().serverSideApply();

            return toKafkaCluster(resource);
        } else {
            throw new BadRequestException("Kafka cluster is not associated with a Strimzi Kafka resource");
        }
    }

    KafkaCluster toKafkaCluster(Kafka kafka) {
        KafkaCluster cluster = KafkaCluster.fromId(kafka.getStatus().getClusterId());
        setKafkaClusterProperties(cluster, kafka);

        // Identify that the cluster is configured with connection information
        String clusterKey = Cache.metaNamespaceKeyFunc(kafka);
        cluster.setConfigured(consoleConfig.getKafka().getCluster(clusterKey).isPresent());

        return cluster;
    }

    KafkaCluster addKafkaContextData(KafkaCluster cluster, KafkaContext kafkaContext) {
        var config = kafkaContext.clusterConfig();
        cluster.setConfigured(true);
        cluster.name(config.getName());
        cluster.namespace(config.getNamespace());

        if (config.getId() != null) {
            // configuration has overridden the id
            cluster.setId(config.getId());
        }

        if (kafkaContext.applicationScoped()) {
            if (kafkaContext.hasCredentials(Admin.class)) {
                cluster.addMeta(AUTHN_KEY, Map.of(AUTHN_METHOD_KEY, "anonymous"));
            } else {
                addAuthenticationMethod(cluster, kafkaContext);
            }
        } else {
            addAuthenticationMethod(cluster, kafkaContext);
        }

        return cluster;
    }

    KafkaCluster addKafkaContextData(KafkaCluster cluster) {
        return addKafkaContextData(cluster, kafkaContext);
    }

    void addAuthenticationMethod(KafkaCluster cluster, KafkaContext kafkaContext) {
        switch (kafkaContext.saslMechanism(Admin.class)) {
            case KafkaConfigs.MECHANISM_OAUTHBEARER:
                Map<String, String> authMeta = new HashMap<>(2);
                authMeta.put(AUTHN_METHOD_KEY, "oauth");
                authMeta.put("tokenUrl", kafkaContext.tokenUrl().orElse(null));
                cluster.addMeta(AUTHN_KEY, authMeta);
                break;
            case KafkaConfigs.MECHANISM_PLAIN, KafkaConfigs.MECHANISM_SCRAM_SHA256, KafkaConfigs.MECHANISM_SCRAM_SHA512:
                cluster.addMeta(AUTHN_KEY, Map.of(AUTHN_METHOD_KEY, "basic"));
                break;
            default:
                break;
        }
    }

    KafkaCluster addKafkaResourceData(KafkaCluster cluster) {
        findCluster(cluster).ifPresent(kafka -> setKafkaClusterProperties(cluster, kafka));
        return cluster;
    }

    void setKafkaClusterProperties(KafkaCluster cluster, Kafka kafka) {
        ObjectMeta kafkaMeta = kafka.getMetadata();
        cluster.name(kafkaMeta.getName());
        cluster.namespace(kafkaMeta.getNamespace());
        cluster.creationTimestamp(kafkaMeta.getCreationTimestamp());
        Optional.ofNullable(kafkaMeta.getAnnotations()).ifPresent(annotations -> {
            String paused = annotations.get(ResourceAnnotations.ANNO_STRIMZI_IO_PAUSE_RECONCILIATION);

            if (paused != null) {
                cluster.reconciliationPaused(Boolean.parseBoolean(paused));
            }
        });

        var comparator = Comparator
            .comparingInt((GenericKafkaListener listener) ->
                listenerSortKey(listener, Annotations.CONSOLE_LISTENER))
            .thenComparingInt((GenericKafkaListener listener) -> {
                if (KafkaListenerType.INTERNAL.equals(listener.getType())) {
                    // sort internal listeners last
                    return 1;
                }
                return -1;
            });

        var listeners = kafka.getSpec()
            .getKafka()
            .getListeners()
            .stream()
            .filter(Predicate.not(l -> annotatedListener(l, Annotations.CONSOLE_HIDDEN)))
            .sorted(comparator)
            .map(listener -> new KafkaListener(
                        listener.getType().toValue(),
                        listenerStatus(kafka, listener).map(ListenerStatus::getBootstrapServers).orElse(null),
                        getAuthType(listener).orElse(null)))
            .toList();

        cluster.listeners(listeners);
        cluster.cruiseControlEnabled(Objects.nonNull(kafka.getSpec().getCruiseControl()));
        setKafkaClusterStatus(cluster, kafka);
    }

    void setKafkaClusterStatus(KafkaCluster cluster, Kafka kafka) {
        Optional.ofNullable(kafka.getStatus()).ifPresentOrElse(status -> {
            String kafkaVersion = Optional.ofNullable(status.getKafkaVersion())
                                        .orElseGet(() -> getKafkaVersionFromSpec(kafka));
            cluster.kafkaVersion(kafkaVersion);
            Optional.ofNullable(status.getConditions()).ifPresent(conditions -> {
                cluster.conditions(conditions.stream().map(Condition::new).toList());

                conditions.stream()
                    .filter(c -> "NotReady".equals(c.getType()) && "True".equals(c.getStatus()))
                    .findFirst()
                    .ifPresentOrElse(
                            c -> cluster.status("NotReady"),
                            () -> cluster.status("Ready"));
            });
            Optional.ofNullable(status.getKafkaNodePools())
                    .ifPresent(pools -> cluster.nodePools(pools.stream().map(pool -> pool.getName()).toList()));
        }, () -> cluster.kafkaVersion(getKafkaVersionFromSpec(kafka)));
    }

    KafkaCluster setManaged(KafkaCluster cluster) {
        cluster.setManaged(findCluster(cluster)
                .map(kafkaTopic -> Boolean.TRUE)
                .orElse(Boolean.FALSE));
        return cluster;
    }

    CompletionStage<KafkaCluster> addMetrics(KafkaCluster cluster, List<String> fields) {
        if (!fields.contains(KafkaCluster.Fields.METRICS)) {
            return CompletableFuture.completedStage(cluster);
        }

        if (kafkaContext.prometheus() == null) {
            logger.warnf("Kafka cluster metrics were requested, but Prometheus URL is not configured");
            cluster.metrics(null);
            return CompletableFuture.completedStage(cluster);
        }

        String namespace = cluster.namespace();
        String name = cluster.name();
        String rangeQuery;
        String valueQuery;

        try (var rangesStream = getClass().getResourceAsStream("/metrics/queries/kafkaCluster_ranges.promql");
             var valuesStream = getClass().getResourceAsStream("/metrics/queries/kafkaCluster_values.promql")) {
            rangeQuery = new String(rangesStream.readAllBytes(), StandardCharsets.UTF_8)
                    .formatted(namespace, name);
            valueQuery = new String(valuesStream.readAllBytes(), StandardCharsets.UTF_8)
                    .formatted(namespace, name);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        var rangeResults = metricsService.queryRanges(rangeQuery).toCompletableFuture();
        var valueResults = metricsService.queryValues(valueQuery).toCompletableFuture();

        return CompletableFuture.allOf(
                rangeResults.thenAccept(cluster.metrics().ranges()::putAll),
                valueResults.thenAccept(cluster.metrics().values()::putAll))
            .thenApply(nothing -> cluster);
    }

    private String getKafkaVersionFromSpec(Kafka kafka) {
        return Optional.ofNullable(kafka.getSpec())
                .map(KafkaSpec::getKafka)
                .map(KafkaClusterSpec::getVersion)
                .orElse(null);
    }


    private Optional<Kafka> findCluster(KafkaCluster cluster) {
        return findCluster(Cache.namespaceKeyFunc(cluster.namespace(), cluster.name()));
    }

    private Optional<Kafka> findCluster(String clusterKey) {
        return kafkaResources()
                .filter(k -> Objects.equals(clusterKey, Cache.metaNamespaceKeyFunc(k)))
                .findFirst();
    }

    private Stream<Kafka> kafkaResources() {
        return kafkaInformer.map(informer -> informer
                    .getStore()
                    .list()
                    .stream()
                    .filter(Predicate.not(k -> annotatedKafka(k, Annotations.CONSOLE_HIDDEN))))
                .orElseGet(Stream::empty);
    }

    static int listenerSortKey(GenericKafkaListener listener, Annotations listenerAnnotation) {
        return annotatedListener(listener, listenerAnnotation) ? -1 : 1;
    }

    static boolean annotatedKafka(Kafka kafka, Annotations listenerAnnotation) {
        return Optional.ofNullable(kafka.getMetadata())
            .map(ObjectMeta::getAnnotations)
            .map(annotations -> annotations.get(listenerAnnotation.value()))
            .map(Boolean::valueOf)
            .orElse(false);
    }

    static boolean annotatedListener(GenericKafkaListener listener, Annotations listenerAnnotation) {
        return Optional.ofNullable(listener.getConfiguration())
            .map(GenericKafkaListenerConfiguration::getBootstrap)
            .map(config -> config.getAnnotations())
            .map(annotations -> annotations.get(listenerAnnotation.value()))
            .map(Boolean::valueOf)
            .orElse(false);
    }

    static Optional<ListenerStatus> listenerStatus(Kafka kafka, GenericKafkaListener listener) {
        String listenerName = listener.getName();

        return Optional.ofNullable(kafka.getStatus())
            .map(KafkaStatus::getListeners)
            .map(Collection::stream)
            .orElseGet(Stream::empty)
            .filter(listenerStatus -> listenerName.equals(listenerStatus.getName()))
            .findFirst();
    }

    static Optional<String> getAuthType(GenericKafkaListener listener) {
        return Optional.of(listener)
            .map(GenericKafkaListener::getAuth)
            .map(authn -> {
                if (authn instanceof KafkaListenerAuthenticationCustom custom) {
                    String saslMechanism = KafkaConfigs.saslMechanism(custom.getListenerConfig());

                    if (KafkaConfigs.MECHANISM_SCRAM_SHA512.equals(saslMechanism)) {
                        return KafkaListenerAuthenticationScramSha512.SCRAM_SHA_512;
                    } else if (KafkaConfigs.MECHANISM_OAUTHBEARER.equals(saslMechanism)) {
                        return KafkaConfigs.TYPE_OAUTH;
                    }
                }

                return authn.getType();
            });
    }

    static List<String> enumNames(Collection<? extends Enum<?>> values) {
        return Optional.ofNullable(values)
                .map(Collection::stream)
                .map(ops -> ops.map(Enum::name).toList())
                .orElse(null);
    }

    /* test */ public void setListUnconfigured(boolean listUnconfigured) {
        this.listUnconfigured = listUnconfigured;
    }
}
