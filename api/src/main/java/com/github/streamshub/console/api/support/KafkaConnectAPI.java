package com.github.streamshub.console.api.support;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import com.fasterxml.jackson.annotation.JsonProperty;

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

}
