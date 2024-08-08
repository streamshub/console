package com.github.streamshub.console.api.service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.DescribeClusterOptions;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.apache.kafka.common.KafkaFuture;
import org.eclipse.microprofile.context.ThreadContext;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.Annotations;
import com.github.streamshub.console.api.ClientFactory;
import com.github.streamshub.console.api.model.Condition;
import com.github.streamshub.console.api.model.KafkaCluster;
import com.github.streamshub.console.api.model.KafkaListener;
import com.github.streamshub.console.api.model.Node;
import com.github.streamshub.console.api.support.Holder;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.config.ConsoleConfig;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaStatus;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListener;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListenerConfiguration;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthentication;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerType;
import io.strimzi.api.kafka.model.kafka.listener.ListenerStatus;

import static com.github.streamshub.console.api.BlockingSupplier.get;

@ApplicationScoped
public class KafkaClusterService {

    private static final String AUTHN_KEY = "authentication";
    private static final String AUTHN_METHOD_KEY = "method";

    @Inject
    Logger logger;

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
                        .orElseGet(() -> addKafkaContextData(new KafkaCluster(id, null, null, null), ctx.getValue()));
                })
                .collect(Collectors.toMap(KafkaCluster::getId, Function.identity()));

        List<KafkaCluster> otherClusters = kafkaResources.stream()
                .filter(k -> !configuredClusters.containsKey(k.getId()))
                .toList();

        return Stream.concat(configuredClusters.values().stream(), otherClusters.stream())
                .map(listSupport::tally)
                .filter(listSupport::betweenCursors)
                .sorted(listSupport.getSortComparator())
                .dropWhile(listSupport::beforePageBegin)
                .takeWhile(listSupport::pageCapacityAvailable)
                .map(this::setManaged)
                .toList();
    }

    public CompletionStage<KafkaCluster> describeCluster(List<String> fields) {
        Admin adminClient = kafkaContext.admin();
        DescribeClusterOptions options = new DescribeClusterOptions()
                .includeAuthorizedOperations(fields.contains(KafkaCluster.Fields.AUTHORIZED_OPERATIONS));
        DescribeClusterResult result = adminClient.describeCluster(options);

        return KafkaFuture.allOf(
                result.authorizedOperations(),
                result.clusterId(),
                result.controller(),
                result.nodes())
            .toCompletionStage()
            .thenApply(nothing -> new KafkaCluster(
                        get(result::clusterId),
                        get(result::nodes).stream().map(Node::fromKafkaModel).toList(),
                        Node.fromKafkaModel(get(result::controller)),
                        enumNames(get(result::authorizedOperations))))
            .thenApplyAsync(this::addKafkaContextData, threadContext.currentContextExecutor())
            .thenApply(this::addKafkaResourceData)
            .thenApply(this::setManaged);
    }

    KafkaCluster toKafkaCluster(Kafka kafka) {
        KafkaCluster cluster = new KafkaCluster(kafka.getStatus().getClusterId(), null, null, null);
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
            case ClientFactory.OAUTHBEARER:
                Map<String, String> authMeta = new HashMap<>(2);
                authMeta.put(AUTHN_METHOD_KEY, "oauth");
                authMeta.put("tokenUrl", kafkaContext.tokenUrl().orElse(null));
                cluster.addMeta(AUTHN_KEY, authMeta);
                break;
            case ClientFactory.PLAIN, ClientFactory.SCRAM_SHA256, ClientFactory.SCRAM_SHA512:
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
        cluster.name(kafka.getMetadata().getName());
        cluster.namespace(kafka.getMetadata().getNamespace());
        cluster.creationTimestamp(kafka.getMetadata().getCreationTimestamp());

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
        setKafkaClusterStatus(cluster, kafka);
    }

    void setKafkaClusterStatus(KafkaCluster cluster, Kafka kafka) {
        Optional.ofNullable(kafka.getStatus())
            .ifPresent(status -> {
                cluster.kafkaVersion(status.getKafkaVersion());
                Optional.ofNullable(status.getConditions())
                    .ifPresent(conditions -> {
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
            });
    }

    KafkaCluster setManaged(KafkaCluster cluster) {
        cluster.addMeta("managed", findCluster(cluster)
                .map(kafkaTopic -> Boolean.TRUE)
                .orElse(Boolean.FALSE));
        return cluster;
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
            .map(KafkaListenerAuthentication::getType);
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
