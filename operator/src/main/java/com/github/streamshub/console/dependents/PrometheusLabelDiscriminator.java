package com.github.streamshub.console.dependents;

import io.fabric8.kubernetes.api.model.rbac.ClusterRole;

public class PrometheusLabelDiscriminator extends BaseLabelDiscriminator<ClusterRole> {

    public PrometheusLabelDiscriminator() {
        super(ConsoleResource.NAME_LABEL, "prometheus");
    }

}
