package com.github.eyefloaters.console.api.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;

@Version("v1alpha1")
@Group("console.eyefloaters.github.com")
public class Console extends CustomResource<ConsoleSpec, ConsoleStatus> {

    private static final long serialVersionUID = 1L;

    @JsonIgnore
    public ConsoleStatus getOrCreateStatus() {
        if (status == null) {
            status = new ConsoleStatus();
        }

        return status;
    }

}
