package com.github.streamshub.console.dependents;

import io.fabric8.kubernetes.api.model.rbac.ClusterRole;

public class ConsoleLabelDiscriminator extends BaseLabelDiscriminator<ClusterRole> {

    public ConsoleLabelDiscriminator() {
        super(ConsoleResource.NAME_LABEL, "console");
    }

}
