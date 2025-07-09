package com.github.streamshub.console.api.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;

import org.eclipse.microprofile.context.ThreadContext;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.jboss.logging.Logger;

import com.github.streamshub.console.api.model.FetchParams;
import com.github.streamshub.console.api.model.connect.ConnectCluster;
import com.github.streamshub.console.api.model.connect.Connector;
import com.github.streamshub.console.api.model.connect.ConnectorPlugin;
import com.github.streamshub.console.api.model.connect.ConnectorTask;
import com.github.streamshub.console.api.security.PermissionService;
import com.github.streamshub.console.api.support.AuthenticationSupport;
import com.github.streamshub.console.api.support.FieldFilter;
import com.github.streamshub.console.api.support.KafkaConnectAPI;
import com.github.streamshub.console.api.support.KafkaConnectAPI.ConnectorOffsets;
import com.github.streamshub.console.api.support.KafkaContext;
import com.github.streamshub.console.api.support.ListRequestContext;
import com.github.streamshub.console.api.support.Promises;
import com.github.streamshub.console.api.support.TrustStoreSupport;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaConnectConfig;
import com.github.streamshub.console.config.authentication.Authenticated;
import com.github.streamshub.console.config.security.Privilege;

import io.quarkus.tls.TlsConfiguration;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.CompletableFuture.completedStage;

@ApplicationScoped
public class KafkaConnectService {

    static final CompletionStage<ConnectCluster> PROMISE_NULL_CLUSTER = completedStage(null);
    static final CompletionStage<Map<Integer, Map<String, String>>> PROMISE_EMPTY_CONFIG = completedStage(emptyMap());
    static final CompletionStage<List<String>> PROMISE_NULL_TOPICS = completedStage(null);
    static final CompletionStage<List<Connector.ConnectorOffset>> PROMISE_NULL_OFFSETS = completedStage(null);
    static final CompletionStage<List<ConnectorPlugin>> PROMISE_NULL_PLUGINS = completedStage(null);
    static final CompletionStage<List<Connector>> PROMISE_EMPTY_CONNECTORS = completedStage(emptyList());

    @Inject
    Logger logger;

    @Inject
    ThreadContext threadContext;

    @Inject
    TrustStoreSupport trustStores;

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    KafkaContext kafkaContext;

    @Inject
    PermissionService permissionService;

    @Produces
    @ApplicationScoped
    Map<String, KafkaConnectAPI> kafkaConnectClients;

    Optional<ClientRequestFilter> additionalFilter = Optional.empty();

    public /* test */ void setAdditionalFilter(Optional<ClientRequestFilter> additionalFilter) {
        this.additionalFilter = additionalFilter;
    }

    @PostConstruct
    public void initialize() {
        this.kafkaConnectClients = consoleConfig.getKafkaConnectClusters().stream()
                .map(cluster -> Map.entry(cluster.getName(), createClient(cluster)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private KafkaConnectAPI createClient(KafkaConnectConfig config) {
        var trustStore = trustStores.getTlsConfiguration(config, null)
                .map(TlsConfiguration::getTrustStore)
                .orElse(null);

        RestClientBuilder builder = RestClientBuilder.newBuilder()
                .baseUri(URI.create(config.getUrl()))
                .trustStore(trustStore)
                .register(createAuthenticationFilter(config));

        additionalFilter.ifPresent(builder::register);

        return builder.build(KafkaConnectAPI.class);
    }

    private ClientRequestFilter createAuthenticationFilter(Authenticated config) {
        AuthenticationSupport authentication = new AuthenticationSupport(config);

        return requestContext -> authentication.get()
                .ifPresent(authn -> requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authn));
    }

    public CompletionStage<List<ConnectCluster>> listClusters(FieldFilter fields, ListRequestContext<ConnectCluster> listSupport) {
        var pendingServerInfo = consoleConfig.getKafkaConnectClusters()
                .stream()
                .filter(listSupport.filter(KafkaConnectConfig.class))
                .map(KafkaConnectConfig::getName)
                .filter(permissionService.permitted(ConnectCluster.API_TYPE, Privilege.LIST, Function.identity()))
                .map(clusterName -> describeCluster(clusterName, fields, listSupport.getFetchParams()))
                .toList();

        return Promises.joinStages(pendingServerInfo)
                .thenApply(results -> results.stream()
                        .filter(listSupport.filter(ConnectCluster.class))
                        .map(listSupport::tally)
                        .filter(listSupport::betweenCursors)
                        .sorted(listSupport.getSortComparator())
                        .dropWhile(listSupport::beforePageBegin)
                        .takeWhile(listSupport::pageCapacityAvailable)
                        .toList());
    }

    public CompletionStage<ConnectCluster> describeCluster(String clusterName, FieldFilter fields, FetchParams fetchParams) {
        var client = kafkaConnectClients.get(clusterName);

        var includePlugins = fields.isIncluded(ConnectCluster.FIELDS_PARAM, ConnectCluster.Fields.PLUGINS.toString());
        var pluginPromise = includePlugins
                ? client.getConnectorPlugins()
                        .thenApply(plugins -> plugins.stream().map(ConnectorPlugin::new).toList())
                : PROMISE_NULL_PLUGINS;

        var includeConnectors = fetchParams.includes(ConnectCluster.Fields.CONNECTORS.toString());
        var fetchConnectors = includeConnectors || fields.isIncluded(ConnectCluster.FIELDS_PARAM, ConnectCluster.Fields.CONNECTORS.toString());
        var connectorPromise = fetchConnectors
                ? listConnectors(clusterName, fields, fetchParams)
                : PROMISE_EMPTY_CONNECTORS;

        return client.getWorkerDetails()
                .thenApply(server -> {
                    var cluster = new ConnectCluster(encode("", clusterName));
                    cluster.name(clusterName);
                    cluster.commit(server.commit());
                    cluster.kafkaClusterId(server.kafkaClusterId());
                    cluster.version(server.version());
                    cluster.kafkaClusters(consoleConfig.getKafkaConnectCluster(clusterName).getKafkaClusters());
                    return cluster;
                })
                .thenCombine(pluginPromise, ConnectCluster::plugins)
                .thenCombine(connectorPromise, (cluster, connectors) -> cluster.connectors(connectors, includeConnectors));
    }

    public CompletionStage<List<Connector>> listConnectors(FieldFilter fields, ListRequestContext<Connector> listSupport) {
        var pendingClusterResults = consoleConfig.getKafkaConnectClusters()
                .stream()
                .filter(listSupport.filter(KafkaConnectConfig.class))
                .map(KafkaConnectConfig::getName)
                .map(clusterName -> listConnectors(clusterName, fields, listSupport.getFetchParams()))
                .toList();

        return Promises.joinStages(pendingClusterResults)
                .thenApply(clusterResults -> clusterResults.stream().flatMap(Collection::stream))
                .thenApply(results -> results
                        .filter(listSupport.filter(Connector.class))
                        .map(listSupport::tally)
                        .filter(listSupport::betweenCursors)
                        .sorted(listSupport.getSortComparator())
                        .dropWhile(listSupport::beforePageBegin)
                        .takeWhile(listSupport::pageCapacityAvailable)
                        .toList());
    }

    public CompletionStage<Connector> describeConnector(
            String connectorId,
            FieldFilter fields,
            FetchParams fetchParams) {
        String[] idParts = decode(connectorId);
        return describeConnector(idParts[0], idParts[1], fields, fetchParams);
    }

    private CompletionStage<List<Connector>> listConnectors(
            String clusterName,
            FieldFilter fields,
            FetchParams fetchParams) {

        return kafkaConnectClients.get(clusterName).getConnectors()
                .thenApplyAsync(names -> names.stream()
                        .filter(permissionService.permitted(Connector.API_TYPE, Privilege.LIST, Function.identity()))
                        .map(name -> describeConnector(clusterName, name, fields, fetchParams)),
                        threadContext.currentContextExecutor())
                .thenCompose(Promises::joinStages);
    }

    private CompletionStage<Connector> describeConnector(
            String clusterName,
            String connectorName,
            FieldFilter fields,
            FetchParams fetchParams) {

        var client = kafkaConnectClients.get(clusterName);

        var includeTopics = fields.isIncluded(Connector.FIELDS_PARAM, Connector.Fields.TOPICS.toString());
        var includeOffsets = fields.isIncluded(Connector.FIELDS_PARAM, Connector.Fields.OFFSETS.toString());

        var includeTasks = fetchParams.includes(Connector.Fields.TASKS.toString());
        var fetchTasks = includeTasks || fields.isIncluded(Connector.FIELDS_PARAM, Connector.Fields.TASKS.toString());
        var includeTaskConfigs = includeTasks && fields.isIncluded(ConnectorTask.FIELDS_PARAM, ConnectorTask.Fields.CONFIG.toString());

        var includeCluster = fetchParams.includes(Connector.Fields.CONNECT_CLUSTER.toString());
        var clusterPromise = includeCluster ? describeCluster(clusterName, fields, fetchParams) : PROMISE_NULL_CLUSTER;

        var topicsPromise = includeTopics
                ? describeConnectorTopics(clusterName, connectorName)
               : PROMISE_NULL_TOPICS;

        var offsetPromise = includeOffsets
            ? describeConnectorOffsets(clusterName, connectorName)
            : PROMISE_NULL_OFFSETS;

        var taskConfigPromise = includeTaskConfigs
            ? describeConnectorTasks(clusterName, connectorName)
            : PROMISE_EMPTY_CONFIG;

        return client.getConnector(connectorName)
            .thenCombine(
                client.getConnectorStatus(connectorName),
                (info, state) -> {
                    Connector connector = new Connector(encode("", clusterName, connectorName));
                    connector.name(connectorName);
                    connector.type(info.type());
                    connector.config(info.config());
                    connector.state(state.connector().state());
                    connector.trace(state.connector().trace());
                    connector.workerId(state.connector().workerId());

                    if (fetchTasks) {
                        mapTasks(connector, info, state, includeTasks);
                    }

                    return connector;
                }
            )
            .thenCombine(topicsPromise, Connector::topics)
            .thenCombine(offsetPromise, Connector::offsets)
            .thenCombine(taskConfigPromise, this::mapTaskConfigs)
            .thenCombine(
                clusterPromise,
                (connector, cluster) -> connector.connectCluster(cluster, includeCluster)
            );
    }

    private void mapTasks(Connector connector, KafkaConnectAPI.ConnectorInfo info, KafkaConnectAPI.ConnectorStateInfo state, boolean include) {
        List<ConnectorTask> tasks = new ArrayList<>();

        for (var taskInfo : info.tasks()) {
            var taskState = state.tasks().stream()
                    .filter(t -> Objects.equals(t.id(), taskInfo.task()))
                    .findFirst();
            var taskId = encode(connector.getId(), String.valueOf(taskInfo.task()));
            ConnectorTask task = new ConnectorTask(taskId);

            task.taskId(taskInfo.task());
            taskState.ifPresent(ts -> {
                task.state(ts.state());
                task.trace(ts.trace());
                task.workerId(ts.workerId());
            });
            task.connector(connector, include);
            tasks.add(task);
        }

        connector.tasks(tasks, true);
    }

    private Connector mapTaskConfigs(Connector connector, Map<Integer, Map<String, String>> configs) {
        for (var config : configs.entrySet()) {
            for (var task : connector.taskResources()) {
                if (task.taskId().equals(config.getKey())) {
                    task.config(config.getValue());
                    break;
                }
            }
        }

        return connector;
    }

    private CompletionStage<List<String>> describeConnectorTopics(String clusterName, String connectorName) {
        return kafkaConnectClients.get(clusterName)
                .getConnectorTopics(connectorName)
                .thenApply(topics -> Optional.ofNullable(topics.get(connectorName))
                        .map(KafkaConnectAPI.TopicInfo::topics)
                        .orElse(null));
    }

    private CompletionStage<List<Connector.ConnectorOffset>> describeConnectorOffsets(String clusterName, String connectorName) {
        return kafkaConnectClients.get(clusterName)
                .getConnectorOffsets(connectorName)
                .thenApply(ConnectorOffsets::offsets)
                .thenApply(offsets -> offsets.stream()
                        .map(o -> new Connector.ConnectorOffset(o.offset(), o.partition()))
                        .toList());
    }

    private CompletionStage<Map<Integer, Map<String, String>>> describeConnectorTasks(String clusterName, String connectorName) {
        return kafkaConnectClients.get(clusterName)
                .getConnectorTasks(connectorName)
                .thenApply(tasks -> tasks.stream()
                        .map(t -> Map.entry(t.id().task(), t.config()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    /**
     * id values must be URL-safe, so we encode the unknown/variable values of object
     * names from Kafka Connect.
     */
    static String encode(String base, String... values) {
        StringBuilder encoded = new StringBuilder(base);
        for (String v : values) {
            if (!encoded.isEmpty()) {
                encoded.append('.');
            }
            encoded.append(Base64.getUrlEncoder().encodeToString(v.getBytes(StandardCharsets.UTF_8)));
        }
        return encoded.toString();
    }

    static String[] decode(String value) {
        List<String> decoded = new ArrayList<>();
        for (String v : value.split("\\.")) {
            decoded.add(new String(Base64.getUrlDecoder().decode(v), StandardCharsets.UTF_8));
        }
        return decoded.toArray(String[]::new);
    }
}
