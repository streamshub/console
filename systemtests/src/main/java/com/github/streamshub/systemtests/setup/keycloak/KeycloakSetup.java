package com.github.streamshub.systemtests.setup.keycloak;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.ScraperPod;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.KeycloakUtils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.LabelSelectorBuilder;
import io.fabric8.kubernetes.api.model.Namespace;
import io.fabric8.kubernetes.api.model.NamespaceBuilder;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.skodjob.testframe.TestFrameEnv;
import io.skodjob.testframe.executor.Exec;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.ResourceItem;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class KeycloakSetup {
    private static final Logger LOGGER = LogWrapper.getLogger(KeycloakSetup.class);

    // Paths
    private static final String DEFAULT_KEYCLOAK_REALM = "console-realm.json";

    public static final String KEYCLOAK_RESOURCES_PATH = TestFrameEnv.USER_PATH + "/src/main/java/com/github/streamshub/systemtests/setup/keycloak/";
    public static final String KEYCLOAK_PREPARE_SCRIPT_PATH = KEYCLOAK_RESOURCES_PATH + "prepare_keycloak_operator.sh";
    public static final String PREPARE_TRUST_STORE_SCRIPT_PATH = KEYCLOAK_RESOURCES_PATH + "prepare_keycloak_truststore.sh";
    public static final String KEYCLOAK_TEARDOWN_SCRIPT_PATH = KEYCLOAK_RESOURCES_PATH + "teardown_keycloak_operator.sh";
    private static final String POSTGRES_DEPLOYMENT_FILE_PATH = KEYCLOAK_RESOURCES_PATH + "postgres.yaml";
    private static final String KEYCLOAK_INSTANCE_FILE_PATH = KEYCLOAK_RESOURCES_PATH + "keycloak-instance.yaml";

    // Deployment variables
    private static final String KEYCLOAK = "keycloak";
    private static final String KEYCLOAK_OPERATOR_DEPLOYMENT_NAME = "keycloak-operator";
    private static final String KEYCLOAK_SECRET_NAME = "keycloak-initial-admin";

    // Postgres
    private static final String POSTGRES = "postgres";
    private static final String POSTGRES_SECRET_NAME = "keycloak-db-secret";
    private static final String POSTGRES_USER_NAME = "testuser";
    private static final String POSTGRES_PASSWORD = "testpasswd";

    private String namespace;

    public KeycloakSetup(String namespace) {
        this.namespace = namespace;
    }

    /**
     * Sets up a complete Keycloak environment in the given namespace and returns a Keycloak configuration.
     *
     * <p>If the namespace does not already exist, it creates the namespace and deploys the necessary resources
     * including a scraper pod, Keycloak Operator, PostgreSQL instance, network policies, and a Keycloak instance.</p>
     * <p>Once Keycloak is running, it retrieves admin credentials from the Keycloak secret and constructs a {@link KeycloakConfig} object.</p>
     * <p>It also imports default realms into Keycloak and prepares a truststore for secure communication.</p>
     *
     * <p>This method ensures a ready-to-use Keycloak setup, including realm data and credentials, for integration testing or secured application access.</p>
     *
     * @return a {@link KeycloakConfig} object containing admin credentials and connection details for the deployed Keycloak instance
     */
    public KeycloakConfig setupKeycloakAndReturnConfig() {
        // If keycloak has been already deployed
        if (ResourceUtils.getKubeResource(Namespace.class, namespace) == null) {
            KubeResourceManager.get().createOrUpdateResourceWithWait(new NamespaceBuilder().withNewMetadata().withName(namespace).endMetadata().build());
            KubeResourceManager.get().createResourceWithoutWait(ScraperPod.getDefaultPod(namespace, Constants.SCRAPER_NAME).build());
            deployKeycloakOperator();
            deployPostgres();
            allowNetworkPolicyBetweenKeycloakAndPostgres();
            deployKeycloakInstance();
            KeycloakUtils.allowNetworkPolicyAllIngressForMatchingLabel(namespace, KEYCLOAK + "-allow", Map.of(Labels.APP, KEYCLOAK));
        }

        Secret keycloakAdminSecret = ResourceUtils.getKubeResource(Secret.class, namespace, KEYCLOAK_SECRET_NAME);

        KeycloakConfig keycloakConfig = new KeycloakConfig(namespace,
            new String(Base64.getDecoder().decode(keycloakAdminSecret.getData().get("password")), StandardCharsets.UTF_8),
            new String(Base64.getDecoder().decode(keycloakAdminSecret.getData().get("username")), StandardCharsets.UTF_8));

        // Prepare truststore
        Exec.exec(List.of(Constants.BASH_CMD, PREPARE_TRUST_STORE_SCRIPT_PATH, namespace, KeycloakUtils.getKeycloakHostname(false), Constants.TRUST_STORE_PASSWORD, Environment.KEYCLOAK_TRUST_STORE_FILE_PATH));
        return keycloakConfig;
    }

    /**
     * Deploys the Keycloak Operator into the specified namespace.
     *
     * <p>The method executes the Keycloak prepare script with the provided namespace, version,
     * and hostname to install the operator components.</p>
     * <p>It waits for the Keycloak Operator deployment to become ready and adds a cleanup action
     * to the resource stack to ensure proper teardown later.</p>
     *
     * <p>This ensures that the Keycloak Operator is fully deployed and operational before proceeding with further operations.</p>
     */
    private void deployKeycloakOperator() {
        LOGGER.info("Preparing Keycloak Operator in Namespace: {}", namespace);
        Exec.exec(List.of(Constants.BASH_CMD, KEYCLOAK_PREPARE_SCRIPT_PATH, namespace, Environment.KEYCLOAK_VERSION,
            KeycloakUtils.getKeycloakHostname(false)));

        WaitUtils.waitForDeploymentWithPrefixIsReady(namespace, KEYCLOAK_OPERATOR_DEPLOYMENT_NAME);

        LOGGER.info("Keycloak Operator in Namespace: {} is ready", namespace);
        KubeResourceManager.get().pushToStack(new ResourceItem<>(this::deleteKeycloakOperator));
    }

    /**
     * Deletes the Keycloak Operator from the specified namespace.
     *
     * <p>The method runs the Keycloak teardown script with the given namespace and Keycloak version
     * to remove the operator components.</p>
     * <p>After executing the teardown, it waits for the Keycloak Operator deployment to be ready,
     * ensuring the cleanup completes successfully.</p>
     *
     * <p>This is typically used to reset the operator environment between tests or deployments.</p>
     */
    private void deleteKeycloakOperator() {
        LOGGER.info("Tearing down Keycloak Operator in Namespace: {}", namespace);
        Exec.exec(List.of(Constants.BASH_CMD, KEYCLOAK_TEARDOWN_SCRIPT_PATH, namespace, Environment.KEYCLOAK_VERSION));
        WaitUtils.waitForDeploymentDeletion(namespace, KEYCLOAK_OPERATOR_DEPLOYMENT_NAME);
    }

    /**
     * Deploys a Keycloak instance into the specified namespace using a predefined YAML template.
     *
     * <p>The method replaces the hostname placeholder in the Keycloak template with the actual hostname
     * and applies the configuration to the target namespace.</p>
     *
     * <p>It waits for the Keycloak StatefulSet to become ready, registers a cleanup action,
     * and waits for the associated Keycloak Secret to be created.</p>
     *
     * <p>This ensures that the Keycloak instance is fully deployed and ready for use in the test environment.</p>
     */
    private void deployKeycloakInstance() {
        LOGGER.info("Deploying Keycloak instance into Namespace: {}", namespace);
        KubeResourceManager.get().kubeCmdClient().inNamespace(namespace).applyContent(FileUtils.readFile(KEYCLOAK_INSTANCE_FILE_PATH)
            .replace("${HOSTNAME}", KeycloakUtils.getKeycloakHostname(true)));

        WaitUtils.waitForStatefulSetReady(namespace, KEYCLOAK);
        WaitUtils.waitForSecretReady(namespace, KEYCLOAK_SECRET_NAME);

        LOGGER.info("Keycloak instance and Keycloak Secret are ready");
        KubeResourceManager.get().pushToStack(new ResourceItem<>(this::deleteKeycloakInstance));
    }

    /**
     * Deletes the Keycloak instance from the specified namespace.
     *
     * <p>The method removes the Keycloak deployment using the predefined YAML file,
     * and explicitly deletes the associated Keycloak Secret by name.</p>
     *
     * <p>This ensures that all resources related to the Keycloak instance are properly cleaned up
     * from the namespace after tests or deployments.</p>
     */
    private void deleteKeycloakInstance() {
        LOGGER.info("Deleting Keycloak in Namespace: {}", namespace);
        KubeResourceManager.get().kubeCmdClient().inNamespace(namespace).delete(KEYCLOAK_INSTANCE_FILE_PATH);
        Secret s = ResourceUtils.getKubeResource(Secret.class, namespace, KEYCLOAK_SECRET_NAME);
        if (s != null) {
            KubeResourceManager.get().deleteResourceWithWait(s);
        }
    }

    /**
     * Deploys a PostgreSQL instance into the specified namespace using a templated YAML file.
     *
     * <p>The method replaces the image placeholder in the YAML with the configured PostgreSQL image
     * and applies the resulting configuration in the target namespace.</p>
     *
     * <p>It waits for the PostgreSQL deployment to become ready, registers a cleanup action,
     * and creates a Kubernetes Secret containing the database credentials.</p>
     *
     * <p>This ensures that PostgreSQL is properly deployed and accessible for components that depend on it.</p>
     */
    private void deployPostgres() {
        LOGGER.info("Deploying Postgres into Namespace: {}", namespace);

        // Get Deployment, Service, ConfigMap and set their namespace, otherwise manager throws exception
        Deployment postgresYamlContent;

        try {
            postgresYamlContent = (Deployment) KubeResourceManager.get().kubeClient().readResourcesFromFile(Path.of(POSTGRES_DEPLOYMENT_FILE_PATH))
                .stream()
                .filter(o -> o.getKind().equals(HasMetadata.getKind(Deployment.class)))
                .findFirst()
                .orElseThrow();
        } catch (IOException e) {
            throw new SetupException("Postgres Deployment YAML could not be loaded", e);
        }

        Deployment postgresDeployment = postgresYamlContent
            .edit()
            .editMetadata()
                .withNamespace(namespace)
            .endMetadata()
            .editSpec()
                .editTemplate()
                    .editSpec()
                        .editFirstContainer()
                            .withImage(Environment.POSTGRES_IMAGE)
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();


        ConfigMap postgresConfig = new ConfigMapBuilder()
            .withNewMetadata()
                .withNamespace(namespace)
                .withName("postgres-config")
            .endMetadata()
            .addToData(Map.of(
                "POSTGRES_DB", KEYCLOAK,
                "POSTGRES_USER", POSTGRES_USER_NAME,
                "POSTGRES_PASSWORD", POSTGRES_PASSWORD,
                "PGDATA", "/var/lib/postgresql/data/pgdata"
            ))
            .build();

        Service postgresService = new ServiceBuilder()
            .withNewMetadata()
                .withName(POSTGRES)
                .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
                .withSelector(Map.of(Labels.APP, POSTGRES))
            .withPorts(new ServicePortBuilder()
                .withName(POSTGRES)
                .withPort(5432)
                .withNewTargetPort(5432)
                .build())
            .endSpec()
            .build();

        Secret postgresSecret = new SecretBuilder()
            .withNewMetadata()
                .withName(POSTGRES_SECRET_NAME)
                .withNamespace(namespace)
            .endMetadata()
            .withType("Opaque")
            .addToData("username", Base64.getEncoder().encodeToString(POSTGRES_USER_NAME.getBytes(StandardCharsets.UTF_8)))
            .addToData("password", Base64.getEncoder().encodeToString(POSTGRES_PASSWORD.getBytes(StandardCharsets.UTF_8)))
            .build();

        // Must be in this order to deploy successfully: ConfigMap -> Deployment -> Service -> Secret
        KubeResourceManager.get().createResourceWithWait(postgresConfig, postgresDeployment, postgresService, postgresSecret);
    }

    /**
     * Allows network traffic from Keycloak pods to PostgreSQL pods by applying a Kubernetes NetworkPolicy.
     *
     * <p>This method is only executed if the environment is configured to deny traffic by default via network policies.</p>
     *
     * <p>It creates and applies an Ingress NetworkPolicy that permits traffic from pods labeled as Keycloak
     * to pods labeled as PostgreSQL within the specified namespace.</p>
     *
     * <p>This ensures connectivity between Keycloak and its backing PostgreSQL database in restricted network environments.</p>
     */
    private void allowNetworkPolicyBetweenKeycloakAndPostgres() {
        if (Environment.DEFAULT_TO_DENY_NETWORK_POLICIES) {
            LabelSelector labelSelector = new LabelSelectorBuilder()
                .addToMatchLabels(Labels.APP, KEYCLOAK)
                .build();

            LOGGER.info("Apply NetworkPolicy Ingress for postgres from keycloak Pods with LabelSelector {}", labelSelector);

            NetworkPolicy networkPolicy = new NetworkPolicyBuilder()
                .withNewMetadata()
                    .withName(KEYCLOAK + "-" + POSTGRES + "-allow")
                    .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                    .addNewIngress()
                        .addNewFrom()
                            .withPodSelector(labelSelector)
                        .endFrom()
                    .endIngress()
                    .withNewPodSelector()
                       .addToMatchLabels(Labels.APP, POSTGRES)
                    .endPodSelector()
                    .withPolicyTypes("Ingress")
                .endSpec()
                .build();

            KubeResourceManager.get().createResourceWithWait(networkPolicy);
        }
    }

    /**
     * Imports console Keycloak realms from predefined JSON files into the Keycloak instance.
     *
     * <p>This method reads each realm JSON file from the configured resource path, encodes it,
     * and imports it into the Keycloak server using the admin token.</p>
     *
     * <p>If the realm already exists, the import is skipped. If the import fails for any other reason,
     * an exception is thrown. Missing or unreadable files will also result in a runtime exception.</p>
     *
     * <p>This ensures that the Keycloak instance is pre-populated with the necessary console realms for testing or configuration.</p>
     */
    public static void importConsoleRealm(KeycloakConfig keycloakConfig, String consoleURL) {
        String token = KeycloakUtils.getToken(keycloakConfig.getNamespace(), keycloakConfig);

        LOGGER.info("Importing console Keycloak realm to Keycloak");
        Path path = Path.of(KEYCLOAK_RESOURCES_PATH + DEFAULT_KEYCLOAK_REALM);
        try {
            LOGGER.info("Importing realm from file: {}", path);
            String consoleRealmString = Files.readString(path, StandardCharsets.UTF_8)
                .replace("${CONSOLE_URL}", consoleURL);

            String jsonRealm = new JsonObject(consoleRealmString).encode();
            String result = KeycloakUtils.importRealm(keycloakConfig.getNamespace(), keycloakConfig.getHttpsUri(), token, jsonRealm);

            if (!result.isEmpty() && !result.contains("already exists")) {
                throw new SetupException(String.format("Realm from file path: %s wasn't imported!", path));
            }

            LOGGER.info("Console realm successfully imported");
        } catch (IOException e) {
            throw new SetupException(String.format("Unable to load file with path: %s due to exception: %n", path) + e);
        }
    }
}
