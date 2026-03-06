package com.github.streamshub.systemtests.setup.keycloak;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.SetupUtils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceOrder;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.keycloak.KeycloakApiUtils;
import com.github.streamshub.systemtests.utils.resourceutils.keycloak.KeycloakUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.skodjob.testframe.TestFrameEnv;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class KeycloakInstanceSetup {

    private static final Logger LOGGER = LogWrapper.getLogger(KeycloakInstanceSetup.class);

    public static final String KEYCLOAK_RESOURCES_PATH =
        TestFrameEnv.USER_PATH + "/src/main/java/com/github/streamshub/systemtests/setup/keycloak";

    // File paths
    private static final String POSTGRES_RESOURCES = KEYCLOAK_RESOURCES_PATH + "/postgres";
    private static final String KEYCLOAK_INSTANCE_FILE_PATH = KEYCLOAK_RESOURCES_PATH + "/keycloak-instance.yaml";
    private static final String DEFAULT_REALM_TEMPLATE_PATH = KEYCLOAK_RESOURCES_PATH + "/console-realm-template.json";

    private static final String KEYCLOAK_HOSTNAME_PREFIX = "console-oidc";

    // Class related
    private final String namespace;
    private final String keycloakIngressName;
    private final String keycloakSecretName;
    private final String keycloakTlsSecretName;
    private final String trustStorePassword;
    private final String trustStoreSecretName;
    private final String trustStoreConfigMap;
    private final String realmName;
    private final String keycloakName;

    private HasMetadata keycloakInstance;
    private List<HasMetadata> postgresResources;

    private String clientId;
    private String clientSecret;

    private String userName;
    private String userPassword;

    public KeycloakInstanceSetup(String namespace) {
        this.namespace = namespace;
        this.keycloakName = "keycloak-" + namespace;
        this.keycloakIngressName = "keycloak-ingress-" + namespace;
        this.keycloakSecretName = keycloakName + "-initial-admin";
        this.keycloakTlsSecretName = "keycloak-tls-secret-" + namespace;
        this.trustStorePassword = "changeit"; // NOSONAR
        this.trustStoreSecretName = "keycloak-truststore-secret-" + namespace;
        this.trustStoreConfigMap = "keycloak-truststore-configmap-" + namespace;
        this.realmName = "console-realm-" + namespace;
        this.clientId = "console-client-" + namespace;

        InputStream tempYaml = null;
        try {
            tempYaml = FileUtils.loadYamlsFromPath(Path.of(POSTGRES_RESOURCES));
            postgresResources = ResourceOrder.sort(KubeResourceManager.get().kubeClient().getClient().load(tempYaml).items());

            // Keycloak does not have CRDs API so we are forced to modify CRD custom fields
            tempYaml = new ByteArrayInputStream(FileUtils.readFile(KEYCLOAK_INSTANCE_FILE_PATH)
                    .replace("${NAME}", keycloakName)
                    .replace("${HOSTNAME}", httpsHostname())
                    .replace("${NAMESPACE}", namespace)
                    .replace("${TLS_SECRET_NAME}", keycloakTlsSecretName)
                    .getBytes(StandardCharsets.UTF_8));

            keycloakInstance = KubeResourceManager.get().kubeClient().getClient().load(tempYaml).items().getFirst();
        } catch (Exception e) {
            throw new SetupException("Unable to load KeycloakInstance or Postgres resources: " + e.getMessage());
        }
        prepareKeycloakInstanceCr();
    }

    private void prepareKeycloakInstanceCr() {
        postgresResources.forEach(resource -> {
            SetupUtils.setNamespaceOnNamespacedResources(resource, namespace);
            SetupUtils.setContainerImage(resource, Constants.POSTGRES, Constants.POSTGRES, Environment.POSTGRES_IMAGE);
        });

    }

    /**
     * Deploys and initializes a Keycloak instance with a backing PostgreSQL database.
     *
     * <p>The setup performs the following steps:</p>
     * <ul>
     *   <li>Creates TLS secrets required for Keycloak ingress.</li>
     *   <li>Deploys the PostgreSQL database and configures network access.</li>
     *   <li>Deploys the Keycloak StatefulSet and waits until it becomes ready.</li>
     *   <li>Configures ingress TLS and network policies for Keycloak.</li>
     *   <li>Retrieves the Keycloak admin credentials from the generated secret.</li>
     *   <li>Prepares a truststore and related Kubernetes resources for secure HTTPS communication.</li>
     * </ul>
     */
    public void setup() {
        LOGGER.info("----------- Install Keycloak Instance -----------");
        KeycloakUtils.createTlsSecret(namespace, keycloakTlsSecretName, httpHostname());

        // Deploy postgres
        LOGGER.info("Deploying postgres into namespace {}", namespace);
        postgresResources.forEach(resource -> KubeResourceManager.get().createOrUpdateResourceWithWait(resource));

        KeycloakUtils.allowNetworkPolicyBetweenKeycloakAndPostgres(
            namespace, Constants.KEYCLOAK + "-" + Constants.POSTGRES + "-allow", Labels.getPostgresLabelSelector());

        // Deploy keycloak instance
        LOGGER.info("Deploying Keycloak instance into namespace {}", namespace);
        KubeResourceManager.get().createOrUpdateResourceWithWait(keycloakInstance);

        WaitUtils.waitForStatefulSetReady(namespace, keycloakName);
        WaitUtils.waitForSecretReady(namespace, keycloakSecretName);

        KeycloakUtils.allowNetworkPolicyAllIngressForMatchingLabel(
            namespace, Constants.KEYCLOAK + "-allow", Labels.getKeycloakLabelSelector());

        KeycloakUtils.patchIngressTls(namespace, httpHostname(), keycloakIngressName, keycloakTlsSecretName);

        // After keycloak instance is present
        LOGGER.info("Reading Keycloak admin secret '{}'", keycloakSecretName);
        Secret adminSecret = ResourceUtils.getKubeResource(Secret.class, namespace, keycloakSecretName);

        userName = new String(Base64.getDecoder().decode(adminSecret.getData().get("username")), StandardCharsets.UTF_8);
        userPassword = new String(Base64.getDecoder().decode(adminSecret.getData().get("password")), StandardCharsets.UTF_8);

        LOGGER.info("Preparing truststore for HTTPS communication with Keycloak");
        KeycloakUtils.importCertificatesIntoTruststore(httpHostname(), trustStorePassword);

        KeycloakUtils.createTrustStorePasswordAndConfigmap(namespace, trustStoreSecretName, trustStoreConfigMap,
            Map.of(Constants.PASSWORD_KEY_NAME, Base64.getEncoder().encodeToString(trustStorePassword.getBytes())));
    }

    /**
     * Imports a Keycloak realm configured for the Console UI.
     *
     * <p>Optionally deletes an existing realm before importing a new one, loads a
     * realm template with the provided role and user mappings, and waits until
     * the realm becomes ready. After the realm is created, the client secret for
     * the configured client is retrieved.</p>
     *
     * @param consoleUiUrl       URL of the Console UI used in the realm configuration
     * @param deleteRealmBefore  whether an existing realm with the same name should be deleted before import
     * @param roleMapping        group-to-role mappings to apply in the realm
     * @param userMapping        users to create in the realm
     */
    public void importConsoleRealm(String consoleUiUrl, boolean deleteRealmBefore, List<KeycloakTestConfig.GroupRoleMapping> roleMapping, List<KeycloakTestConfig.User> userMapping) {
        if (deleteRealmBefore && KeycloakApiUtils.realmExists(httpsHostname(), userName, userPassword, realmName)) {
            LOGGER.info("Realm {} already exists, deleting before reimport", realmName);
            KeycloakApiUtils.deleteRealm(httpsHostname(), userName, userPassword, realmName);
            WaitUtils.waitForKeycloakRealmDeleted(httpsHostname(), userName, userPassword, realmName);
        }
        JsonObject realmJson = KeycloakUtils.loadRealmTemplate(consoleUiUrl, DEFAULT_REALM_TEMPLATE_PATH, realmName, clientId, roleMapping, userMapping);
        KeycloakApiUtils.importRealm(httpsHostname(), userName, userPassword, realmJson.encode());
        WaitUtils.waitForKeycloakRealmReady(httpsHostname(), userName, userPassword, realmName);

        // Once realm is ready, client can get it's ID assigned
        clientSecret = KeycloakApiUtils.getClientSecret(httpsHostname(), userName, userPassword, realmName, clientId);
    }

    public String httpHostname() {
        return getKeycloakHostname(false);
    }

    public String httpsHostname() {
        return getKeycloakHostname(true);
    }

    private static String getKeycloakHostname(Boolean https) {
        return (Boolean.TRUE.equals(https) ? "https://"  : "") + KEYCLOAK_HOSTNAME_PREFIX + "." + ClusterUtils.getClusterDomain();
    }

    public String namespace() {
        return namespace;
    }

    public String userName() {
        return userName;
    }
    public String userPassword() {
        return userPassword;
    }

    public String getTrustStoreSecretName() {
        return trustStoreSecretName;
    }

    public String getTrustStoreConfigMap() {
        return trustStoreConfigMap;
    }

    public String realmName() {
        return realmName;
    }

    public String clientId() {
        return clientId;
    }

    public String clientSecret() {
        return clientSecret;
    }
}