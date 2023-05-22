package io.strimzi.kafka.instance.model;

import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class ConfigEntry {

    public static class ConfigEntryMap extends java.util.HashMap<String, ConfigEntry> {
        private static final long serialVersionUID = 1L;
        private ConfigEntryMap() {
        }
    }

    @JsonIgnore
    private String name;

    private String value;

    @Schema(readOnly = true)
    private String source;

    @Schema(readOnly = true)
    private boolean sensitive;

    @Schema(readOnly = true)
    private boolean readOnly;

    @Schema(readOnly = true)
    private String type;

    @Schema(readOnly = true)
    private String documentation;

    public static ConfigEntry fromKafkaModel(org.apache.kafka.clients.admin.ConfigEntry kafkaEntry) {
        ConfigEntry entry = new ConfigEntry();

        entry.setName(kafkaEntry.name());
        entry.setValue(kafkaEntry.value());
        entry.setSource(Optional.ofNullable(kafkaEntry.source()).map(Enum::name).orElse(null));
        entry.setSensitive(kafkaEntry.isSensitive());
        entry.setReadOnly(kafkaEntry.isReadOnly());
        entry.setType(Optional.ofNullable(kafkaEntry.type()).map(Enum::name).orElse(null));
        entry.setDocumentation(kafkaEntry.documentation());

        return entry;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isSensitive() {
        return sensitive;
    }

    public void setSensitive(boolean sensitive) {
        this.sensitive = sensitive;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getDocumentation() {
        return documentation;
    }

    public void setDocumentation(String documentation) {
        this.documentation = documentation;
    }

}
