package com.github.streamshub.console.api.v1alpha1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.github.streamshub.console.api.v1alpha1.spec.ConsoleSpec;
import com.github.streamshub.console.api.v1alpha1.status.ConsoleStatus;

import io.fabric8.kubernetes.api.model.Namespaced;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.model.annotation.Group;
import io.fabric8.kubernetes.model.annotation.Version;
import io.sundr.builder.annotations.Buildable;

@Version("v1alpha1")
@Group("console.streamshub.github.com")
@Buildable(editableEnabled = false, builderPackage = "io.fabric8.kubernetes.api.builder")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Console extends CustomResource<ConsoleSpec, ConsoleStatus> implements Namespaced {

    private static final long serialVersionUID = 1L;

    @JsonIgnore
    public ConsoleStatus getOrCreateStatus() {
        if (status == null) {
            status = new ConsoleStatus();
        }

        return status;
    }

}
