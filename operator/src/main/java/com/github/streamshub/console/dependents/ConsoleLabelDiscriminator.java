package com.github.streamshub.console.dependents;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.fabric8.kubernetes.api.model.rbac.ClusterRole;

public class ConsoleLabelDiscriminator extends BaseLabelDiscriminator<ClusterRole> {

    public ConsoleLabelDiscriminator() {
        super(ConsoleResource.NAME_LABEL, "console");
    }

    public ConsoleLabelDiscriminator(Map<String, String> labels) {
        super(Stream.concat(
                Stream.of(Map.entry(ConsoleResource.NAME_LABEL, "console")),
                labels.entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
    }
}
