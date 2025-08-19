package com.github.streamshub.systemtests.setup.console;

import com.github.streamshub.systemtests.Environment;

public abstract class InstallConfig {
    protected String deploymentName = Environment.CONSOLE_DEPLOYMENT_NAME;
    protected String deploymentNamespace;

    protected InstallConfig(String namespace) {
        this.deploymentNamespace = namespace;
    }

    abstract void install();

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
