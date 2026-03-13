package com.github.streamshub.systemtests.setup.apicurio;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.SetupUtils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceOrder;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.ResourceItem;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public class ApicurioOperatorSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(ApicurioOperatorSetup.class);

    private static final String APICURIO_OPERATOR_WATCHED_NAMESPACES = "APICURIO_OPERATOR_WATCHED_NAMESPACES";
    private static final String APICURIO_OPERATOR_NAME = "apicurio-registry-operator-v" + Environment.APICURIO_VERSION;
    private static final String APICURIO_BUNDLE_URL = "https://github.com/Apicurio/apicurio-registry/releases/download/" +
        Environment.APICURIO_VERSION + "/apicurio-registry-operator-" + Environment.APICURIO_VERSION + ".tar.gz";
    private static final String APICURIO_TEMP_FILE_PREFIX = "apicurio_tmp";
    private static final String APICURIO_INSTALL_DIR_NAME = "install";
    private static final String APICURIO_INSTALL_FILE_NAME = "install.yaml";

    private Path extractedArchive;

    private final String operatorNamespace;
    private final String watchNamespace;
    private List<HasMetadata> allResources;

    public ApicurioOperatorSetup(String operatorNamespace, String watchNamespace) {
        this.operatorNamespace = operatorNamespace;
        this.watchNamespace = watchNamespace;

        InputStream installYaml = null;
        try {
            extractedArchive = FileUtils.downloadAndExtractTarGz(APICURIO_BUNDLE_URL, APICURIO_TEMP_FILE_PREFIX);
            installYaml = FileUtils.loadYamlsFromPath(extractedArchive.resolve(APICURIO_INSTALL_DIR_NAME).resolve(APICURIO_INSTALL_FILE_NAME));
        } catch (IOException e) {
            throw new SetupException("Unable to load Apicurio Registry operator resources: " + e.getMessage());
        }

        allResources = ResourceOrder.sort(KubeResourceManager.get().kubeClient().getClient().load(installYaml).items().stream().toList());
        LOGGER.info("Loaded {} resources from Apicurio YAML", allResources.size());
        prepareApicurioCrs();
    }

     /**
     * Prepares Apicurio Operator resources for deployment into the target namespace.
     *
     *  <li>Adjusts namespaces, normalizes RBAC bindings, removes security contexts,
     *      and ensures the operator watches only the configured namespace by setting
     *      the appropriate environment variable on its Deployment.</li>
     *   <li>Sets the {@code WATCH_NAMESPACE} env var on the operator Deployment so it
     *       only reconciles ApicurioRegistry CRs in the configured watch namespace.</li>
     * </ul>
     */
    private void prepareApicurioCrs() {
        allResources.forEach(resource -> {
            SetupUtils.setNamespaceOnNamespacedResources(resource, operatorNamespace);
            SetupUtils.removeSecurityContexts(resource);
            SetupUtils.fixClusterRoleBindingNamespace(resource, operatorNamespace);

            // Set APICURIO_OPERATOR_WATCHED_NAMESPACES env var on the operator Deployment
            if (resource instanceof Deployment deployment) {
                deployment.getSpec().getTemplate().getSpec().getContainers().forEach(container -> {
                    if (container.getEnv() != null) {
                        container.getEnv().stream()
                            .filter(env -> APICURIO_OPERATOR_WATCHED_NAMESPACES.equals(env.getName()))
                            .findFirst()
                            .ifPresentOrElse(
                                env -> {
                                    LOGGER.info("Overriding APICURIO_OPERATOR_WATCHED_NAMESPACES to '{}' in Deployment: {}", watchNamespace, deployment.getMetadata().getName());
                                    // Clear the OLM fieldRef and set a plain value instead
                                    env.setValueFrom(null);
                                    env.setValue(watchNamespace);
                                },
                                () -> {
                                    LOGGER.info("Adding APICURIO_OPERATOR_WATCHED_NAMESPACES='{}' to Deployment: {}", watchNamespace, deployment.getMetadata().getName());
                                    container.getEnv().add(
                                        new EnvVarBuilder()
                                            .withName(APICURIO_OPERATOR_WATCHED_NAMESPACES)
                                            .withValue(watchNamespace)
                                            .build()
                                    );
                                }
                            );
                    }
                });
            }
        });
    }

    public void setup() {
        LOGGER.info("----------- Install Apicurio Registry Operator -----------");

        LOGGER.info("Installing Apicurio Registry operator from: {}", APICURIO_BUNDLE_URL);
        allResources.forEach(resource -> KubeResourceManager.get().createOrUpdateResourceWithoutWait(resource));

        WaitUtils.waitForDeploymentWithPrefixIsReady(operatorNamespace, APICURIO_OPERATOR_NAME);
        // Allow resource manager to clean up on test teardown
        KubeResourceManager.get().pushToStack(new ResourceItem<>(this::teardown));
        LOGGER.info("Installation of Apicurio Registry operator completed");
    }

    public void teardown() {
        LOGGER.info("----------- Uninstall Apicurio Registry Operator -----------");
        allResources.forEach(resource -> KubeResourceManager.get().deleteResourceWithoutWait(resource));
    }
}
