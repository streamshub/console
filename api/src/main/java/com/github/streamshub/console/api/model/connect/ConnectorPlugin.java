package com.github.streamshub.console.api.model.connect;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.streamshub.console.api.support.KafkaConnectAPI;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ConnectorPlugin(
        @JsonProperty("class")
        String className,
        String type,
        String version,
        List<ConfigInfo> config
) {

    public ConnectorPlugin(KafkaConnectAPI.PluginInfo info) {
        this(info.className(), info.type(), info.version(), new ArrayList<>());
    }

    static record ConfigInfo(
            String defaultValue,
            List<String> dependents,
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
}
