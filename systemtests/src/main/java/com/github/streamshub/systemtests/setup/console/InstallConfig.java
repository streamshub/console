package com.github.streamshub.systemtests.setup.console;

import java.util.List;
import java.util.function.UnaryOperator;

import com.github.streamshub.systemtests.Environment;

import io.fabric8.kubernetes.api.model.EnvVar;

public abstract class InstallConfig {
    protected String deploymentName = Environment.CONSOLE_DEPLOYMENT_NAME;
    protected String deploymentNamespace;

    protected InstallConfig(String namespace) {
        this.deploymentNamespace = namespace;
    }

    abstract void install();
    abstract void delete();
    abstract List<EnvVar> editEnv(UnaryOperator<List<EnvVar>> editor);

    public String getDeploymentName() {
        return deploymentName;
    }

    public String getDeploymentNamespace() {
        return deploymentNamespace;
    }

    public void setDeploymentNamespace(String deploymentNamespace) {
        this.deploymentNamespace = deploymentNamespace;
    }

    public void setDeploymentName(String deploymentName) {
        this.deploymentName = deploymentName;
    }
}
