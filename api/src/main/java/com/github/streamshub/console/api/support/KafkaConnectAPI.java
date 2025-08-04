package com.github.streamshub.console.api.support;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.RestClientBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaConnectConfig;
import com.github.streamshub.console.config.authentication.Authenticated;

import io.quarkus.cache.CacheResult;
import io.quarkus.tls.TlsConfiguration;

@Path("/")
public interface KafkaConnectAPI {

    static record ServerInfo(
        String commit,
        @JsonProperty("kafka_cluster_id")
        String kafkaClusterId,
        String version
    ) { /* Data container only */ }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<ServerInfo> getWorkerDetails();

    static record PluginInfo(
            @JsonProperty("class")
            String className,
            String type,
            String version
    ) { /* Data container only */ }

    @Path("connector-plugins")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<List<PluginInfo>> getConnectorPlugins();

    static record ConfigKeyInfo(
            @JsonProperty("default_value")
            String defaultValue,
            List<String> dependents,
            @JsonProperty("display_name")
            String displayName,
            String documentation,
            String group,
            String importance,
            String name,
            Integer order,
            Boolean required,
            String type,
            String width
    ) { /* Data container only */ }

    @Path("connector-plugins/{pluginName}/config")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<List<ConfigKeyInfo>> getConnectorPluginConfig(@PathParam("pluginName") String pluginName);

    @Path("connectors")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<List<String>> getConnectors();

    static record ConnectorTaskId(
            String connector,
            Integer task
    ) { /* Data container only */ }

    static record ConnectorInfo(
            Map<String, String> config,
            String name,
            List<ConnectorTaskId> tasks,
            String type
    ) { /* Data container only */ }

    @Path("connectors/{connectorName}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<ConnectorInfo> getConnector(@PathParam("connectorName") String connectorName);

    static record ConnectorOffset(
            Map<String, Object> offset,
            Map<String, Object> partition
    ) { /* Data container only */ }

    static record ConnectorOffsets(
            List<ConnectorOffset> offsets
    ) { /* Data container only */ }

    @Path("connectors/{connectorName}/offsets")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<ConnectorOffsets> getConnectorOffsets(@PathParam("connectorName") String connectorName);

    static record ConnectorState(
            String state,
            String trace,
            @JsonProperty("worker_id")
            String workerId
    ) { /* Data container only */ }

    static record TaskState(
            Integer id,
            String state,
            String trace,
            @JsonProperty("worker_id")
            String workerId
    ) { /* Data container only */ }

    static record ConnectorStateInfo(
            String name,
            ConnectorState connector,
            List<TaskState> tasks,
            String type
    ) { /* Data container only */ }

    @Path("connectors/{connectorName}/status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<ConnectorStateInfo> getConnectorStatus(@PathParam("connectorName") String connectorName);

    static record TaskInfo(
            ConnectorTaskId id,
            Map<String, String> config
    ) { /* Data container only */ }

    @Path("connectors/{connectorName}/tasks")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<List<TaskInfo>> getConnectorTasks(@PathParam("connectorName") String connectorName);

    static record TopicInfo(
            List<String> topics
    ) { /* Data container only */ }

    @Path("connectors/{connectorName}/topics")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    CompletionStage<Map<String, TopicInfo>> getConnectorTopics(@PathParam("connectorName") String connectorName);

    @ApplicationScoped
    public static class Client {

        @Inject
        ConsoleConfig consoleConfig;

        @Inject
        TrustStoreSupport trustStores;

        Map<String, KafkaConnectAPI> clients;

        Optional<ClientRequestFilter> additionalFilter = Optional.empty();

        public /* test */ void setAdditionalFilter(Optional<ClientRequestFilter> additionalFilter) {
            this.additionalFilter = additionalFilter;
        }

        private class ProxyRequestFilter implements ClientRequestFilter {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                if (additionalFilter.isPresent()) {
                    additionalFilter.get().filter(requestContext);
                }
            }
        }

        @PostConstruct
        public void initialize() {
            this.clients = consoleConfig.getKafkaConnectClusters().stream()
                    .map(cluster -> Map.entry(cluster.clusterKey(), createClient(cluster)))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }

        private KafkaConnectAPI createClient(KafkaConnectConfig config) {
            var trustStore = trustStores.getTlsConfiguration(config, null)
                    .map(TlsConfiguration::getTrustStore)
                    .orElse(null);

            RestClientBuilder builder = RestClientBuilder.newBuilder()
                    .baseUri(URI.create(config.getUrl()))
                    .trustStore(trustStore)
                    .register(createAuthenticationFilter(config))
                    .register(new ProxyRequestFilter());

            return builder.build(KafkaConnectAPI.class);
        }

        private ClientRequestFilter createAuthenticationFilter(Authenticated config) {
            AuthenticationSupport authentication = new AuthenticationSupport(config);

            return requestContext -> authentication.get()
                    .ifPresent(authn -> requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authn));
        }

        @CacheResult(cacheName = "kafka-connect-worker-details")
        public CompletionStage<ServerInfo> getWorkerDetails(String clusterKey) {
            return clients.get(clusterKey).getWorkerDetails();
        }

        @CacheResult(cacheName = "kafka-connect-connector-plugins")
        public CompletionStage<List<PluginInfo>> getConnectorPlugins(String clusterKey) {
            return clients.get(clusterKey).getConnectorPlugins();
        }

        @CacheResult(cacheName = "kafka-connect-connector-plugin-config")
        public CompletionStage<List<ConfigKeyInfo>> getConnectorPluginConfig(String clusterKey, String pluginName) {
            return clients.get(clusterKey).getConnectorPluginConfig(pluginName);
        }

        @CacheResult(cacheName = "kafka-connect-connectors")
        public CompletionStage<List<String>> getConnectors(String clusterKey) {
            return clients.get(clusterKey).getConnectors();
        }

        @CacheResult(cacheName = "kafka-connect-connector")
        public CompletionStage<ConnectorInfo> getConnector(String clusterKey, String connectorName) {
            return clients.get(clusterKey).getConnector(connectorName);
        }

        @CacheResult(cacheName = "kafka-connect-connector-offsets")
        public CompletionStage<ConnectorOffsets> getConnectorOffsets(String clusterKey, String connectorName) {
            return clients.get(clusterKey).getConnectorOffsets(connectorName);
        }

        @CacheResult(cacheName = "kafka-connect-connector-status")
        public CompletionStage<ConnectorStateInfo> getConnectorStatus(String clusterKey, String connectorName) {
            return clients.get(clusterKey).getConnectorStatus(connectorName);
        }

        @CacheResult(cacheName = "kafka-connect-connector-tasks")
        public CompletionStage<List<TaskInfo>> getConnectorTasks(String clusterKey, String connectorName) {
            return clients.get(clusterKey).getConnectorTasks(connectorName);
        }

        @CacheResult(cacheName = "kafka-connect-connector-topics")
        public CompletionStage<Map<String, TopicInfo>> getConnectorTopics(String clusterKey, String connectorName) {
            return clients.get(clusterKey).getConnectorTopics(connectorName);
        }
    }
}
