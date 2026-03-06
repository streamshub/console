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
import com.github.streamshub.systemtests.utils.resourceutils.keycloak.KeycloakUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.skodjob.testframe.TestFrameEnv;
import io.skodjob.testframe.resources.KubeResourceManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class KeycloakInstanceSetup {

    private static final Logger LOGGER = LogWrapper.getLogger(KeycloakInstanceSetup.class);

    public static final String KEYCLOAK_RESOURCES_PATH =
        TestFrameEnv.USER_PATH + "/src/main/java/com/github/streamshub/systemtests/setup/keycloak/";

    // File paths
    private static final String POSTGRES_DEPLOYMENT_FILE_PATH = KEYCLOAK_RESOURCES_PATH + "postgres.yaml";
    private static final String KEYCLOAK_INSTANCE_FILE_PATH = KEYCLOAK_RESOURCES_PATH + "keycloak-instance.yaml";

    // Keycloak
    private static final String KEYCLOAK_INGRESS_NAME = "keycloak-ingress";
    private static final String KEYCLOAK_SECRET_NAME = "keycloak-initial-admin";
    private static final String KEYCLOAK_TLS_SECRET_NAME = "example-tls-secret";
    private static final String TRUST_STORE_PASSWORD = "changeit"; // NOSONAR - test password

    // Class related
    private HasMetadata keycloakInstance;
    private List<HasMetadata> postgresResources;

    private final String namespace;
    private String userName;
    private String userPassword;

    public KeycloakInstanceSetup(String namespace) {
        this.namespace = namespace;

        InputStream tempYaml = null;
        try {
            tempYaml = FileUtils.getYamlFileFromURL(POSTGRES_DEPLOYMENT_FILE_PATH);
            postgresResources = ResourceOrder.sort(KubeResourceManager.get().kubeClient().getClient().load(tempYaml).items());

            // Keycloak does not have CRDs API so we are forced to modify CRD custom fields
            tempYaml = new ByteArrayInputStream(
                FileUtils.readFile(KEYCLOAK_INSTANCE_FILE_PATH).replace("${HOSTNAME}", httpsHostname())
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

        SetupUtils.setNamespaceOnNamespacedResources(keycloakInstance, namespace);
    }

    public void setup(String secretName, String configMapName) {
        LOGGER.info("----------- Install Keycloak Instance -----------");
        KeycloakUtils.createTlsSecret(namespace, KEYCLOAK_TLS_SECRET_NAME, httpHostname());

        // Deploy postgres
        LOGGER.info("Deploying postgres into namespace {}", namespace);
        postgresResources.forEach(resource -> KubeResourceManager.get().createOrUpdateResourceWithWait(resource));

        KeycloakUtils.allowNetworkPolicyBetweenKeycloakAndPostgres(
            namespace, Constants.KEYCLOAK + "-" + Constants.POSTGRES + "-allow", Labels.getPostgresLabelSelector());

        // Deploy keycloak instance
        LOGGER.info("Deploying Keycloak instance into namespace {}", namespace);
        KubeResourceManager.get().createOrUpdateResourceWithWait(keycloakInstance);

        WaitUtils.waitForStatefulSetReady(namespace, Constants.KEYCLOAK);
        WaitUtils.waitForSecretReady(namespace, KEYCLOAK_SECRET_NAME);

        KeycloakUtils.allowNetworkPolicyAllIngressForMatchingLabel(
            namespace, Constants.KEYCLOAK + "-allow", Labels.getKeycloakLabelSelector());

        KeycloakUtils.patchIngressTls(namespace, httpHostname(), KEYCLOAK_INGRESS_NAME, KEYCLOAK_TLS_SECRET_NAME);

        // After keycloak instance is present
        LOGGER.info("Reading Keycloak admin secret '{}'", KEYCLOAK_SECRET_NAME);
        Secret adminSecret = ResourceUtils.getKubeResource(Secret.class, namespace, KEYCLOAK_SECRET_NAME);

        userName = new String(Base64.getDecoder().decode(adminSecret.getData().get("username")), StandardCharsets.UTF_8);
        userPassword = new String(Base64.getDecoder().decode(adminSecret.getData().get("password")), StandardCharsets.UTF_8);

        LOGGER.info("Preparing truststore for HTTPS communication with Keycloak");
        KeycloakUtils.prepareTrustStore(httpHostname(), TRUST_STORE_PASSWORD);
        KeycloakUtils.createTrustStorePasswordAndConfigmap(namespace, secretName, configMapName, TRUST_STORE_PASSWORD);
    }

    public String getUserName() {
        return userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public String getTrustStorePassword() {
        return TRUST_STORE_PASSWORD;
    }

    public String httpHostname() {
        return getKeycloakHostname(false);
    }

    public String httpsHostname() {
        return getKeycloakHostname(true);
    }

    private static String getKeycloakHostname(Boolean https) {
        return (Boolean.TRUE.equals(https) ? "https://"  : "") + Constants.KEYCLOAK_HOSTNAME_PREFIX + "." + ClusterUtils.getClusterDomain();
    }
}