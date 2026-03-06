package com.github.streamshub.systemtests.utils.resourceutils.keycloak;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.console.dependents.ConsoleResource;
import com.github.streamshub.console.utils.Certificates;
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
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.enums.LogLevel;
import io.skodjob.testframe.executor.Exec;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.wait.Wait;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class KeycloakUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KeycloakUtils.class);

    private KeycloakUtils() {}

    @SuppressWarnings("MethodLength")
    public static void importConsoleRealm(String consoleURL, String httpsHostname, String userName, String password,
                                          List<KeycloakTestConfig.GroupRoleMapping> mapping, List<KeycloakTestConfig.User> users
    ) {
        // Generated from the mapping and initial test JSON - now flexible for adding new roles or users
        JsonObject realm = new JsonObject()
            .put("realm", Constants.KEYCLOAK_REALM)
            .put("enabled", true)
            .put("accessTokenLifespan", 6000)
            .put("ssoSessionIdleTimeout", 864000)
            .put("ssoSessionMaxLifespan", 864000)
            .put("accessCodeLifespan", 6000)
            .put("accessCodeLifespanUserAction", 6000)
            .put("notBefore", 0)
            .put("sslRequired", "external")
            .put("rememberMe", false)
            .put("ssoSessionIdleTimeoutRememberMe", 0)
            .put("ssoSessionMaxLifespanRememberMe", 0)
            .put("roles", new JsonObject()
                .put("realm", new JsonArray(
                    mapping.stream()
                        .map(m -> new JsonObject()
                            .put("name", m.roleName())
                            .put("description", m.roleDescription()))  // use actual descriptions, not ""
                        .toList()))
                .put("client", new JsonObject()
                    .put(Constants.KEYCLOAK_CLIENT_ID, new JsonArray())))
            .put("groups", new JsonArray(
                mapping.stream()
                    .map(m -> new JsonObject()
                        .put("name", m.groupName())  // groupName should NOT include leading "/"
                        .put("path", m.groupPath())
                        .put("realmRoles", new JsonArray(List.of(m.roleName()))))
                    .toList()))
            .put("users", new JsonArray(
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
                    .toList()))
            .put("clients", new JsonArray(List.of(
                new JsonObject()
                    .put("clientId", Constants.KEYCLOAK_CLIENT_ID)
                    .put("description", "private client for streamshub console")
                    .put("enabled", true)
                    .put("bearerOnly", false)
                    .put("consentRequired", false)
                    .put("standardFlowEnabled", true)
                    .put("implicitFlowEnabled", false)
                    .put("clientAuthenticatorType", "client-secret")
                    .put("redirectUris", new JsonArray(List.of(consoleURL + "/*")))
                    .put("webOrigins", new JsonArray(List.of(consoleURL + "/")))
                    .put("directAccessGrantsEnabled", true)
                    .put("serviceAccountsEnabled", true)
                    .put("publicClient", false)
                    .put("frontchannelLogout", true)
                    .put("protocol", "openid-connect")
                    .put("fullScopeAllowed", true)
                    .put("nodeReRegistrationTimeout", -1)
                    .put("defaultClientScopes", new JsonArray(List.of("profile", "groups", "email"))))))
            .put("clientScopes", new JsonArray(List.of(
                new JsonObject()
                    .put("name", "profile")
                    .put("protocol", "openid-connect")
                    .put("attributes", new JsonObject()
                        .put("include.in.token.scope", "true")
                        .put("display.on.consent.screen", "true"))
                    .put("protocolMappers", new JsonArray(List.of(
                        new JsonObject()
                            .put("name", "profile")
                            .put("protocol", "openid-connect")
                            .put("protocolMapper", "oidc-usermodel-attribute-mapper")
                            .put("config", new JsonObject()
                                .put("userinfo.token.claim", "true")
                                .put("user.attribute", "profile")
                                .put("id.token.claim", "true")
                                .put("access.token.claim", "true")
                                .put("claim.name", "profile")
                                .put("jsonType.label", "String")),
                        new JsonObject()
                            .put("name", "username")
                            .put("protocol", "openid-connect")
                            .put("protocolMapper", "oidc-usermodel-attribute-mapper")
                            .put("config", new JsonObject()
                                .put("userinfo.token.claim", "true")
                                .put("user.attribute", "username")
                                .put("id.token.claim", "true")
                                .put("access.token.claim", "true")
                                .put("claim.name", "preferred_username")
                                .put("jsonType.label", "String"))))),
                new JsonObject()
                    .put("name", "basic")
                    .put("protocol", "openid-connect")
                    .put("attributes", new JsonObject()
                        .put("include.in.token.scope", "false")
                        .put("display.on.consent.screen", "false"))
                    .put("protocolMappers", new JsonArray(List.of(
                        new JsonObject()
                            .put("name", "sub")
                            .put("protocol", "openid-connect")
                            .put("protocolMapper", "oidc-sub-mapper"),
                        new JsonObject()
                            .put("name", "auth_time")
                            .put("protocol", "openid-connect")
                            .put("protocolMapper", "oidc-usersessionmodel-note-mapper")
                            .put("config", new JsonObject()
                                .put("user.session.note", "AUTH_TIME")
                                .put("id.token.claim", "true")
                                .put("access.token.claim", "true")
                                .put("claim.name", "auth_time")
                                .put("jsonType.label", "long"))))),
                new JsonObject()
                    .put("name", "email")
                    .put("protocol", "openid-connect")
                    .put("attributes", new JsonObject()
                        .put("include.in.token.scope", "true")
                        .put("display.on.consent.screen", "true"))
                    .put("protocolMappers", new JsonArray(List.of(
                        new JsonObject()
                            .put("name", "email")
                            .put("protocol", "openid-connect")
                            .put("protocolMapper", "oidc-usermodel-attribute-mapper")
                            .put("config", new JsonObject()
                                .put("userinfo.token.claim", "true")
                                .put("user.attribute", "email")
                                .put("id.token.claim", "true")
                                .put("access.token.claim", "true")
                                .put("claim.name", "email")
                                .put("jsonType.label", "String"))))),
                new JsonObject()
                    .put("name", "groups")
                    .put("protocol", "openid-connect")
                    .put("attributes", new JsonObject()
                        .put("include.in.token.scope", "true")
                        .put("display.on.consent.screen", "true"))
                    .put("protocolMappers", new JsonArray(List.of(
                        new JsonObject()
                            .put("name", "Groups Mapper")
                            .put("protocol", "openid-connect")
                            .put("protocolMapper", "oidc-group-membership-mapper")
                            .put("config", new JsonObject()
                                .put("full.path", "true")
                                .put("multivalued", "true")
                                .put("id.token.claim", "true")
                                .put("access.token.claim", "true")
                                .put("claim.name", "groups"))))))));

        String result = importRealm(httpsHostname, userName, password, realm.encode());
        if (!result.isEmpty() && !result.contains("already exists")) {
            throw new SetupException("Console realm was not imported: " + result);
        }

        LOGGER.info("Console realm successfully imported");
    }

    public static String importRealm(String httpsHostname, String userName, String password, String realmData) {
        String token = getToken(httpsHostname, userName, password);
        return executeRequestAndReturnData(
            new String[]{
                "curl",
                "--insecure",
                "-X",
                "POST",
                "-H", "Content-Type: application/json",
                "-d", realmData,
                httpsHostname + "/admin/realms",
                "-H", "Authorization: Bearer " + token
            }
        );
    }

    public static String getClientSecret(String httpsHostname, String userName, String password, String realm, String clientName) {
        final String clientUuid = getClientUuid(httpsHostname, userName, password, realm, clientName);
        final String token = getToken(httpsHostname, userName, password);
        return new JsonObject(executeRequestAndReturnData(
            new String[]{
                "curl",
                "--insecure",
                "-X",
                "GET",
                httpsHostname + "/admin/realms/" + realm + "/clients/" + clientUuid + "/client-secret",
                "-H", "Authorization: Bearer " + token
            }
        )).getString("value");
    }

    public static String getClientUuid(String httpsHostname, String userName, String password, String realm, String clientName) {
        final String token = getToken(httpsHostname, userName, password);
        String response = executeRequestAndReturnData(
            new String[]{
                "curl",
                "--insecure",
                "-X",
                "GET",
                httpsHostname + "/admin/realms/" + realm + "/clients/",
                "-H", "Authorization: Bearer " + token
            }
        );

        ObjectMapper mapper = new ObjectMapper();
        JsonNode clientsArray;

        try {
            clientsArray = mapper.readTree(response);
        } catch (JsonProcessingException e) {
            throw new SetupException("Keycloak client uuid response cannot be mapped to json node", e);
        }

        String clientUuid = "";
        for (JsonNode client: clientsArray) {
            if (client.get("clientId").textValue().equals(clientName)) {
                clientUuid = client.get("id").textValue();
                break;
            }
        }

        return clientUuid;
    }

    /**
     * Creates and applies a permissive NetworkPolicy that allows all ingress traffic to Pods
     * matching a set of labels, but only if the environment is configured to use default-deny
     * network policies.
     *
     * <p>This method checks the {@code Environment.DEFAULT_TO_DENY_NETWORK_POLICIES} flag to
     * determine whether network policies should be applied. If enabled, it constructs a
     * Kubernetes {@link NetworkPolicy} resource that:</p>
     *
     * <ul>
     *     <li>Targets Pods in the given namespace whose labels match {@code matchLabels}.</li>
     *     <li>Defines an empty <em>ingress</em> rule block, which Kubernetes interprets as
     *         "allow all ingress traffic" to the selected Pods.</li>
     *     <li>Creates the policy in the cluster and waits for it to become active via
     *         {@code KubeResourceManager.createResourceWithWait}.</li>
     * </ul>
     *
     * <p>If the environment flag is disabled, no action is performed.</p>
     *
     * @param namespaceName the namespace in which the NetworkPolicy should be created
     * @param policyName    the name of the NetworkPolicy resource
     * @param matchLabels   the labels identifying which Pods the policy applies to
     */
    public static void allowNetworkPolicyAllIngressForMatchingLabel(String namespaceName, String policyName, LabelSelector matchLabels) {
        if (Environment.DEFAULT_TO_DENY_NETWORK_POLICIES) {
            LOGGER.info("Apply NetworkPolicy with Ingress to accept all connections to the Pods matching labels: {}", matchLabels);

            NetworkPolicy networkPolicy = new NetworkPolicyBuilder()
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
                .build();

            KubeResourceManager.get().createResourceWithWait(networkPolicy);
        }
    }

    public static void allowNetworkPolicyBetweenKeycloakAndPostgres(String namespace, String policyName, LabelSelector postgresLabel) {
        if (Environment.DEFAULT_TO_DENY_NETWORK_POLICIES) {
            LOGGER.info("Applying NetworkPolicy: Keycloak → Postgres");
            KubeResourceManager.get().createResourceWithWait(new NetworkPolicyBuilder()
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
    }

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

    public static void prepareTrustStore(String httpHostname, String trustStorePassword) {
        try {
            Certificates.importCertificateIntoTrustStore(httpHostname, 443,
                Path.of(Environment.KEYCLOAK_TRUST_STORE_FILE_PATH), trustStorePassword, "keycloak-ca");
        } catch (Exception e) {
            throw new SetupException("Failed to prepare Keycloak truststore: " + e.getMessage());
        }
    }

    public static void createTrustStorePasswordAndConfigmap(String namespace, String secretName, String configMapName, String trustStorePassword) {
        LOGGER.info("Create secret with trust store password for console");
        KubeResourceManager.get().createOrUpdateResourceWithWait(new SecretBuilder()
            .withNewMetadata()
                .withName(secretName)
                .withNamespace(namespace)
                .addToLabels(ConsoleResource.MANAGEMENT_LABEL)
            .endMetadata()
            .addToData(Constants.PASSWORD_KEY_NAME, Base64.getEncoder().encodeToString(trustStorePassword.getBytes()))
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

    public static String getToken(String httpsHostname, String userName, String password) {
        return new JsonObject(
            executeRequestAndReturnData(
                new String[]{
                    "curl",
                    "-v",
                    "--insecure",
                    "-X",
                    "POST",
                    "-d", "client_id=admin-cli&grant_type=password&username=" + userName + "&password=" + password,
                    httpsHostname + "/realms/master/protocol/openid-connect/token"
                }
            )).getString("access_token");
    }

    /**
     * Executes a shell request (typically a {@code curl} command) and returns the response
     * once it succeeds. The method retries until the command produces a valid output that
     * does not contain a "Connection refused" error.
     *
     * <p>The request is executed via {@link Exec#exec}, and the output is captured. If the
     * request fails or the Keycloak API is temporarily unavailable, the method waits and
     * retries using the {@link Wait#until} utility with standard polling and timeout
     * intervals.</p>
     *
     * <p>Any exceptions during command execution are logged and cause a retry. When the
     * request eventually succeeds, the output from the command is returned.</p>
     *
     * @param request the command to execute, represented as a string array suitable for {@code Exec.exec}
     * @return the full command output once the request succeeds
     */
    public static String executeRequestAndReturnData(String[] request) {
        AtomicReference<String> response = new AtomicReference<>("");

        Wait.until("request to Keycloak API will be successful", TestFrameConstants.GLOBAL_POLL_INTERVAL_SHORT, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM, () -> {
            try {
                String commandOutput = Exec.exec(LogLevel.DEBUG, false, request).out();
                if (!commandOutput.contains("Connection refused")) {
                    response.set(commandOutput);
                    return true;
                }

                return false;
            } catch (Exception e) {
                LOGGER.warn("Exception occurred during doing request on Keycloak API: {}", e.getMessage());
                return false;
            }
        });

        return response.get();
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
