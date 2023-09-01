package com.github.eyefloaters.console.api.model;

import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@Schema(name = "ConfigAttributes")
@JsonInclude(Include.NON_NULL)
public class ConfigEntry {

    public static class ConfigResponse extends DataResponse<ConfigResource> {
        public ConfigResponse(Map<String, ConfigEntry> data) {
            super(new ConfigResource(data));
        }
    }

    public interface ConfigEntryMap extends Map<String, ConfigEntry> {
    }

    @Schema(name = "Config")
    public static final class ConfigResource extends Resource<Map<String, ConfigEntry>> {
        public ConfigResource(Map<String, ConfigEntry> data) {
            super(null, "configs", data);
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

    /**
     * Compare the {@code value} field of this ConfigEntry with the other ConfigEntry's
     * {@code value} field.
     *
     * <p>Numeric fields will be parsed before comparison, passwords will always compare
     * as equal (to avoid potential information leakage), and other types will be
     * compared as strings.
     *
     * <p>This method assumes that both ConfigEntry objects have the same type and
     * that neither has a null value.
     */
    int compareValues(ConfigEntry other) {
        return switch (type) {
            case "DOUBLE" -> Double.compare(Double.parseDouble(value), Double.parseDouble(other.value));
            case "INT", "LONG", "SHORT" -> Long.compare(Long.parseLong(value), Long.parseLong(other.value));
            case "PASSWORD" -> 0;
            default -> value.compareTo(other.value);
        };
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
