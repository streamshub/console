package com.github.streamshub.systemtests.utils.resourceutils.keycloak;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakConfig;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.enums.LogLevel;
import io.skodjob.testframe.executor.Exec;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.wait.Wait;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class KeycloakUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KeycloakUtils.class);

    private KeycloakUtils() {}

    /**
     * Imports a Keycloak realm by sending a POST request with the provided realm JSON definition.
     *
     * <p>This method performs an HTTP POST request (via a shell-executed {@code curl} command)
     * to the {@code /admin/realms} endpoint of the target Keycloak instance. The request uses the
     * supplied bearer token for authentication and sends the {@code realmData} payload as JSON.</p>
     *
     * <p>The response body from the request is returned as a string. If the request fails,
     * the underlying executor is expected to throw or return error output accordingly.</p>
     *
     * @param baseURI    the base URI of the Keycloak instance (e.g. {@code https://keycloak.example.com})
     * @param token      the bearer token used for authorization
     * @param realmData  the JSON payload representing the realm to import
     *
     * @return the raw response body from the Keycloak API call
     */
    public static String importRealm(String baseURI, String token, String realmData) {
        return executeRequestAndReturnData(
            new String[]{
                "curl",
                "--insecure",
                "-X",
                "POST",
                "-H", "Content-Type: application/json",
                "-d", realmData,
                baseURI + "/admin/realms",
                "-H", "Authorization: Bearer " + token
            }
        );
    }

    /**
     * Retrieves the client secret for a given Keycloak client.
     *
     * <p>This method resolves the client's internal UUID, obtains an admin access token,
     * and then performs an authenticated HTTP GET request (via a shell-executed
     * {@code curl} command) to the Keycloak Admin API to fetch the client's secret.</p>
     *
     * <p>The response is parsed as JSON and the {@code "value"} field—representing the
     * actual client secret—is returned.</p>
     *
     * @param keycloakConfig  the configuration object containing Keycloak connection details
     * @param realm           the name of the Keycloak realm where the client resides
     * @param clientName      the display name of the client whose secret should be retrieved
     *
     * @return the client secret value extracted from the Keycloak Admin API response
     */
    public static String getClientSecret(KeycloakConfig keycloakConfig, String realm, String clientName) {
        final String clientUuid = getClientUuid(keycloakConfig, realm, clientName);
        final String token = getToken(keycloakConfig);
        return new JsonObject(executeRequestAndReturnData(
            new String[]{
                "curl",
                "--insecure",
                "-X",
                "GET",
                getKeycloakHostname(true) + "/admin/realms/" + realm + "/clients/" + clientUuid + "/client-secret",
                "-H", "Authorization: Bearer " + token
            }
        )).getString("value");
    }

    /**
     * Resolves the internal UUID of a Keycloak client by its client name.
     *
     * <p>This method authenticates against Keycloak using an admin token, retrieves the list
     * of all clients within the specified realm via an HTTP GET request (executed through
     * a shell-based {@code curl} command), and parses the JSON response to locate the client
     * whose {@code clientId} matches the provided {@code clientName}.</p>
     *
     * <p>Once found, the method extracts and returns the client's internal UUID
     * (the {@code "id"} field). If no matching client is found, an empty string is returned.</p>
     *
     * <p>If the returned JSON cannot be parsed, a {@link SetupException} is thrown.</p>
     *
     * @param keycloakConfig  the configuration object containing Keycloak connection and credential details
     * @param realm           the Keycloak realm to search within
     * @param clientName      the client identifier to look up
     *
     * @return the internal UUID of the matching Keycloak client, or an empty string if not found
     *
     * @throws SetupException if the JSON response cannot be parsed
     */
    public static String getClientUuid(KeycloakConfig keycloakConfig, String realm, String clientName) {
        final String token = getToken(keycloakConfig);

        String response = executeRequestAndReturnData(
            new String[]{
                "curl",
                "--insecure",
                "-X",
                "GET",
                getKeycloakHostname(true) + "/admin/realms/" + realm + "/clients/",
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
    public static void allowNetworkPolicyAllIngressForMatchingLabel(String namespaceName, String policyName, Map<String, String> matchLabels) {
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
                    .withNewPodSelector()
                        .addToMatchLabels(matchLabels)
                    .endPodSelector()
                .endSpec()
                .build();

            KubeResourceManager.get().createResourceWithWait(networkPolicy);
        }
    }

    /**
     * Retrieves an access token from Keycloak using the configured admin credentials.
     *
     * <p>The method issues a direct token request to the master realm’s
     * OpenID Connect token endpoint. It constructs a form-encoded POST body containing:</p>
     *
     * <ul>
     *     <li>{@code client_id=admin-cli} — relying on Keycloak’s built-in admin CLI client.</li>
     *     <li>{@code grant_type=password} — performing a resource-owner-password credential exchange.</li>
     *     <li>{@code username} and {@code password} — taken from the provided {@link KeycloakConfig}.</li>
     * </ul>
     *
     * <p>The response is parsed as JSON, and the {@code access_token} field is returned.
     * If the request fails or the response is malformed, the caller’s handling logic decides
     * how to surface errors.</p>
     *
     * @param keycloakConfig configuration providing Keycloak hostname and admin credentials
     * @return the extracted Keycloak access token string
     */
    public static String getToken(KeycloakConfig keycloakConfig) {
        return new JsonObject(
            executeRequestAndReturnData(
                new String[]{
                    "curl",
                    "-v",
                    "--insecure",
                    "-X",
                    "POST",
                    "-d", "client_id=admin-cli&grant_type=password&username=" + keycloakConfig.getUsername() + "&password=" + keycloakConfig.getPassword(),
                    getKeycloakHostname(true) + "/realms/master/protocol/openid-connect/token"
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
     * Builds the base Keycloak hostname using the cluster domain and optional HTTPS scheme.
     * <p>
     * The hostname is constructed using the predefined {@link Constants#KEYCLOAK_HOSTNAME_PREFIX}
     * followed by the detected cluster domain. If {@code https} is {@code true}, the URI will be
     * prefixed with {@code "https://"}, otherwise no scheme is added.
     *
     * @param https whether to include the {@code https://} scheme in the returned hostname
     * @return the fully resolved Keycloak hostname for the current cluster
     */
    public static String getKeycloakHostname(Boolean https) {
        return (Boolean.TRUE.equals(https) ? "https://"  : "") + Constants.KEYCLOAK_HOSTNAME_PREFIX + "." + ClusterUtils.getClusterDomain();
    }

    /**
     * Constructs the full URI for accessing a specific Keycloak realm.
     * <p>
     * This method uses {@link #getKeycloakHostname(Boolean)} with HTTPS enabled and appends the
     * standard Keycloak realm path ({@code /realms/<realm>}).
     *
     * @param realm the name of the Keycloak realm
     * @return the full URI for the given realm (e.g. {@code https://keycloak.example.com/realms/myrealm})
     */
    public static String getKeycloakRealmUri(String realm) {
        return getKeycloakHostname(true) + "/realms/" + realm;
    }
}
