package com.github.streamshub.systemtests.setup.keycloak;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.SetupUtils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceOrder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class KeycloakOperatorSetup {

    private static final Logger LOGGER = LogWrapper.getLogger(KeycloakOperatorSetup.class);

    private static final String KEYCLOAK_OPERATOR_DEPLOYMENT_NAME = "keycloak-operator";

    private static final String RAW_K8S_CONTENT_URL = "https://raw.githubusercontent.com/keycloak/keycloak-k8s-resources/refs/tags/" + Environment.KEYCLOAK_VERSION;
    private static final String KEYCLOAK_REALM_IMPORTS_CRD_URL = RAW_K8S_CONTENT_URL + "/kubernetes/keycloakrealmimports.k8s.keycloak.org-v1.yml";
    private static final String KEYCLOAK_CRD_URL = RAW_K8S_CONTENT_URL + "/kubernetes/keycloaks.k8s.keycloak.org-v1.yml";
    private static final String KEYCLOAK_OPERATOR_URL = RAW_K8S_CONTENT_URL + "/kubernetes/kubernetes.yml";

    private final String deploymentNamespace;
    private List<HasMetadata> allResources = new ArrayList<>();

    public KeycloakOperatorSetup(String deploymentNamespace) {
        this.deploymentNamespace = deploymentNamespace;

        InputStream tempYaml = null;
        try {
            for (String resourceUrl : List.of(KEYCLOAK_CRD_URL, KEYCLOAK_REALM_IMPORTS_CRD_URL, KEYCLOAK_OPERATOR_URL)) {
                tempYaml = FileUtils.getYamlFileFromURL(resourceUrl);
                List<HasMetadata> resourcesFromYaml = KubeResourceManager.get().kubeClient().getClient().load(tempYaml).items();
                allResources.addAll(resourcesFromYaml);
            }

        } catch (Exception e) {
            throw new SetupException("Unable to load Keycloak Operator resources: " + e.getMessage());
        }
        allResources = ResourceOrder.sort(allResources);
        LOGGER.info("Loaded {} resources for Keycloak operator", allResources.size());
        prepareKeycloakCrs();
    }

    private void prepareKeycloakCrs() {
        allResources.forEach(resource -> {
            SetupUtils.setNamespaceOnNamespacedResources(resource, deploymentNamespace);
            SetupUtils.removeSecurityContexts(resource);
            SetupUtils.fixClusterRoleBindingNamespace(resource, deploymentNamespace);
            SetupUtils.fixRoleBindingNamespace(resource, deploymentNamespace);
            if (resource instanceof Deployment deployment) {
                Container container = deployment.getSpec().getTemplate().getSpec().getContainers().getFirst();

                if (!Environment.KEYCLOAK_OPERATOR_IMAGE.isEmpty()) {
                    container.setImage(Environment.KEYCLOAK_OPERATOR_IMAGE);
                }

                if (!Environment.KEYCLOAK_SERVER_IMAGE.isEmpty()) {
                    container.getEnv().stream()
                        .filter(env -> env.getName().equals("RELATED_IMAGE_KEYCLOAK"))
                        .findFirst()
                        .ifPresent(env -> env.setValue(Environment.KEYCLOAK_SERVER_IMAGE));
                }
            }
        });
    }

    public void setup() {
        LOGGER.info("----------- Install Keycloak Operator -----------");

        allResources.forEach(resource ->
            KubeResourceManager.get().createOrUpdateResourceWithoutWait(resource));

        WaitUtils.waitForDeploymentWithPrefixIsReady(deploymentNamespace, KEYCLOAK_OPERATOR_DEPLOYMENT_NAME);
        LOGGER.info("Installation of Keycloak operator completed");
    }
}