package com.github.streamshub.systemtests.setup.console;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;

public abstract class InstallConfig {
    protected String deploymentName = Environment.CONSOLE_DEPLOYMENT_NAME;
    protected String deploymentNamespace = Constants.CO_NAMESPACE;

    protected InstallConfig() {}

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
