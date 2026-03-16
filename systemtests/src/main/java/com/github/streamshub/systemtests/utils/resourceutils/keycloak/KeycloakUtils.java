package com.github.streamshub.systemtests.utils.resourceutils.keycloak;

import com.github.streamshub.console.dependents.ConsoleResource;
import com.github.streamshub.systemtests.utils.Certificates;
import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakTestConfig;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class KeycloakUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KeycloakUtils.class);

    private KeycloakUtils() {}

    /**
     * Loads and patches a Keycloak realm template with dynamic roles, groups, users,
     * and console client configuration.
     *
     * @param consoleURL console base URL
     * @param realmPath  path to the realm template file
     * @param realmName  name of the realm to create
     * @param clientId   client ID used by the console
     * @param mapping    role/group mappings
     * @param users      users to include in the realm
     *
     * @return patched realm definition as JsonObject
     */
    public static JsonObject loadRealmTemplate(String consoleURL, String realmPath, String realmName, String clientId,
        List<KeycloakTestConfig.GroupRoleMapping> mapping, List<KeycloakTestConfig.User> users
    ) {
        JsonObject realm = new JsonObject(FileUtils.readFile(realmPath));

        // Realm name
        realm.put("realm", realmName);

        // Patch dynamic roles
        realm.getJsonObject("roles").put("realm", new JsonArray(
            mapping.stream()
                .map(m -> new JsonObject()
                    .put("name", m.roleName())
                    .put("description", m.roleDescription()))
                .toList()));

        // Patch dynamic groups
        realm.put("groups", new JsonArray(
            mapping.stream()
                .map(m -> new JsonObject()
                    .put("name", m.groupName())
                    .put("path", m.groupPath())
                    .put("realmRoles", new JsonArray(List.of(m.roleName()))))
                .toList()));

        // Patch dynamic users
        realm.put("users", new JsonArray(
            users.stream()
                .map(u -> new JsonObject()
                    .put("username", u.username())
                    .put("enabled", true)
                    .put("emailVerified", true)
                    .put("firstName", u.firstName())
                    .put("lastName", u.lastName())
                    .put("email", u.email())
                    .put("credentials", new JsonArray(List.of(
                        new JsonObject().put("type", "password").put("value", u.password()))))
                    .put("groups", new JsonArray(List.of(u.groupPath()))))
                .toList()));

        // Patch console URL in client
        JsonArray clients = realm.getJsonArray("clients");
        JsonObject client = clients.getJsonObject(0);
        client.put("clientId", clientId);
        client.put("redirectUris", new JsonArray(List.of(consoleURL + "/*")));
        client.put("webOrigins", new JsonArray(List.of(consoleURL + "/")));

        JsonObject rolesClient = realm.getJsonObject("roles").getJsonObject("client");
        rolesClient.put(clientId, new JsonArray());

        return realm;
    }

    /**
     * Creates a NetworkPolicy allowing all ingress traffic to pods matching the given labels.
     *
     * @param namespaceName namespace where the policy is created
     * @param policyName    name of the NetworkPolicy
     * @param matchLabels   pod selector for allowed ingress
     */
    public static void allowNetworkPolicyAllIngressForMatchingLabel(String namespaceName, String policyName, LabelSelector matchLabels) {
        if (!Environment.DEFAULT_TO_DENY_NETWORK_POLICIES) {
            return;
        }
        LOGGER.info("Apply NetworkPolicy with Ingress to accept all connections to the Pods matching labels: {}", matchLabels);

        KubeResourceManager.get().createOrUpdateResourceWithWait(new NetworkPolicyBuilder()
            .withNewMetadata()
                .withName(policyName)
                .withNamespace(namespaceName)
            .endMetadata()
            .editSpec()
                // keeping ingress empty to allow all connections
                .addNewIngress()
                .endIngress()
                .withPodSelector(matchLabels)
            .endSpec()
            .build());
    }

    /**
     * Creates a NetworkPolicy allowing Keycloak pods to access Postgres pods.
     *
     * @param namespace     namespace where the policy is created
     * @param policyName    name of the NetworkPolicy
     * @param postgresLabel label selector identifying Postgres pods
     */
    public static void allowNetworkPolicyBetweenKeycloakAndPostgres(String namespace, String policyName, LabelSelector postgresLabel) {
        if (!Environment.DEFAULT_TO_DENY_NETWORK_POLICIES) {
            return;
        }
        LOGGER.info("Applying NetworkPolicy: Keycloak → Postgres");

        KubeResourceManager.get().createOrUpdateResourceWithWait(new NetworkPolicyBuilder()
            .withNewMetadata()
                .withName(policyName)
                .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
                .addNewIngress()
                    .addNewFrom()
                        .withPodSelector(Labels.getKeycloakLabelSelector())
                    .endFrom()
                .endIngress()
                .withPodSelector(postgresLabel)
                .withPolicyTypes(Ingress.class.getSimpleName())
            .endSpec()
            .build());
    }

    /**
     * Generates a self-signed certificate and creates a TLS secret for Keycloak.
     *
     * @param namespace     namespace where the secret will be created
     * @param tlsSecretName name of the TLS secret
     * @param hostname      hostname for the generated certificate
     */
    public static void createTlsSecret(String namespace, String tlsSecretName, String hostname) {
        LOGGER.info("Creating TLS secret '{}' for hostname '{}' in namespace '{}'", tlsSecretName, hostname, namespace);
        try {
            var entry = Certificates.generateSelfSignedCertificate(hostname, "StreamsHub");
            Map<String, String> pem = Certificates.toPemStrings(entry.getKey(), entry.getValue());

            KubeResourceManager.get().createOrUpdateResourceWithoutWait(new SecretBuilder()
                .withNewMetadata()
                    .withName(tlsSecretName)
                    .withNamespace(namespace)
                .endMetadata()
                .withType("kubernetes.io/tls")
                .addToData("tls.crt",
                    Base64.getEncoder().encodeToString(pem.get("tls.crt").getBytes(StandardCharsets.UTF_8)))
                .addToData("tls.key",
                    Base64.getEncoder().encodeToString(pem.get("tls.key").getBytes(StandardCharsets.UTF_8)))
                .build());
        } catch (Exception e) {
            throw new SetupException("Failed to generate TLS secret for Keycloak: " + e.getMessage());
        }
    }

    /**
     * Patches the Keycloak Ingress resource to enable TLS using the provided secret.
     *
     * @param namespace     namespace of the ingress
     * @param httpHostname  hostname exposed by the ingress
     * @param ingressName   name of the ingress resource
     * @param tlsSecretName TLS secret used for HTTPS
     */
    public static void patchIngressTls(String namespace, String httpHostname, String ingressName, String tlsSecretName) {
        if (ClusterUtils.isOcp()) {
            return;
        }

        LOGGER.info("Non-OCP cluster detected — patching Keycloak Ingress with TLS");
        WaitUtils.waitForIngressToBePresent(namespace, ingressName);

        KubeResourceManager.get().createOrUpdateResourceWithWait(ResourceUtils.getKubeResource(Ingress.class, namespace, ingressName)
            .edit()
                .editSpec()
                    .addNewTl()
                        .withHosts(httpHostname)
                        .withSecretName(tlsSecretName)
                    .endTl()
                .endSpec()
            .build());

        Utils.sleepWait(TimeConstants.COMPONENT_LOAD_TIMEOUT);

        String nginxController = ResourceUtils.listKubeResourcesByLabelSelector(Pod.class, Constants.NGINX_INGRESS_NAMESPACE, Labels.getNginxPodLabelSelector())
            .getFirst().getMetadata().getName();

        WaitUtils.waitForLogInPod(Constants.NGINX_INGRESS_NAMESPACE, nginxController, httpHostname);
    }

    /**
     * Imports the Keycloak server certificate into the local truststore.
     *
     * @param httpHostname       Keycloak hostname
     * @param trustStorePassword password protecting the truststore
     */
    public static void importCertificatesIntoTruststore(String httpHostname, String trustStorePassword) {
        try {
            Certificates.importCertificateIntoTrustStore(httpHostname, 443,
                Path.of(Environment.KEYCLOAK_TRUST_STORE_FILE_PATH), trustStorePassword, "keycloak-ca");
        } catch (Exception e) {
            throw new SetupException("Failed to prepare Keycloak truststore: " + e.getMessage());
        }
    }

    /**
     * Creates a secret with the truststore password and a configmap containing the truststore.
     *
     * @param namespace               namespace where resources are created
     * @param secretName              name of the secret storing the password
     * @param configMapName           name of the configmap storing the truststore
     * @param trustStorePasswordData  encoded truststore password data
     */
    public static void createTrustStorePasswordAndConfigmap(String namespace, String secretName, String configMapName, Map<String, String> trustStorePasswordData) {
        LOGGER.info("Create secret with trust store password for console");
        KubeResourceManager.get().createOrUpdateResourceWithWait(new SecretBuilder()
            .withNewMetadata()
                .withName(secretName)
                .withNamespace(namespace)
                .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
            .endMetadata()
            .addToData(trustStorePasswordData)
            .build());

        // Configmap with truststore
        LOGGER.info("Create configmap with trust store");
        String encodedTrustStore = Base64.getEncoder().encodeToString(FileUtils.readFileBytes(Environment.KEYCLOAK_TRUST_STORE_FILE_PATH));

        LOGGER.info("Encoded TrustStore: {}", encodedTrustStore);

        KubeResourceManager.get().createOrUpdateResourceWithWait(new ConfigMapBuilder()
            .withNewMetadata()
                .withName(configMapName)
                .withNamespace(namespace)
                .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
            .endMetadata()
            .addToBinaryData(Constants.TRUST_STORE_KEY_NAME, encodedTrustStore)
            .build());
    }

    /**
     * Constructs the full URI for accessing a specific Keycloak realm.
     *
     * @param realm the name of the Keycloak realm
     * @return the full URI for the given realm (e.g. {@code https://keycloak.example.com/realms/myrealm})
     */
    public static String getKeycloakRealmUri(String httpsHostname, String realm) {
        return httpsHostname + "/realms/" + realm;
    }
}
