package com.github.streamshub.systemtests.utils.resourceutils.keycloak;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.skodjob.kubetest4j.KubeTestConstants;
import io.skodjob.kubetest4j.enums.LogLevel;
import io.skodjob.kubetest4j.executor.Exec;
import io.skodjob.kubetest4j.wait.Wait;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

public class KeycloakApiUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KeycloakApiUtils.class);

    private KeycloakApiUtils() {}

    private static final String ADMIN_REALMS_PATH = "/admin/realms/";
    private static final String OIDC_TOKEN_PATH = "/realms/master/protocol/openid-connect/token"; // NOSONAR

    /**
     * Checks whether a Keycloak realm exists.
     *
     * @param httpsHostname  Keycloak HTTPS hostname
     * @param userName       admin username used for authentication
     * @param password       admin password used for authentication
     * @param realmName      name of the realm to check
     *
     * @return {@code true} if the realm exists, {@code false} otherwise
     */
    public static boolean realmExists(String httpsHostname, String userName, String password, String realmName) {
        String result = executeRequestAndReturnData(request(
            "GET", httpsHostname, ADMIN_REALMS_PATH + realmName, userName, password, null));
        return !result.contains("Realm not found") && !result.isEmpty();
    }

    /**
     * Deletes a Keycloak realm.
     *
     * @param httpsHostname  Keycloak HTTPS hostname
     * @param userName       admin username used for authentication
     * @param password       admin password used for authentication
     * @param realmName      name of the realm to delete
     *
     * @throws SetupException if the realm deletion request fails
     */
    public static void deleteRealm(String httpsHostname, String userName, String password, String realmName) {
        String result = executeRequestAndReturnData(request(
            "DELETE", httpsHostname, ADMIN_REALMS_PATH + realmName, userName, password, null));

        if (!result.contains("Realm not found")) {
            throw new SetupException("Keycloak realm was not deleted");
        }

        LOGGER.info("Deleted Keycloak realm {}", realmName);
    }

    /**
     * Imports a Keycloak realm using the provided realm JSON definition.
     *
     * @param httpsHostname  Keycloak HTTPS hostname
     * @param userName       admin username used for authentication
     * @param password       admin password used for authentication
     * @param realmData      JSON definition of the realm to import
     *
     * @throws SetupException if the realm import request fails
     */
    public static void importRealm(String httpsHostname, String userName, String password, String realmData) {

        String result = executeRequestAndReturnData(request(
            "POST", httpsHostname, ADMIN_REALMS_PATH,  userName, password, realmData));

        if (!result.isEmpty() && !result.contains("already exists")) {
            throw new SetupException("Console realm was not imported: " + result);
        }

        LOGGER.info("Console realm successfully imported");
    }

    /**
     * Retrieves the client secret for a given Keycloak client.
     *
     * <p>The method first resolves the internal client UUID using the provided
     * client name and then queries the Keycloak Admin API for the client secret.</p>
     *
     * @param httpsHostname  Keycloak HTTPS hostname
     * @param userName       admin username used for authentication
     * @param password       admin password used for authentication
     * @param realm          realm containing the client
     * @param clientName     client ID (name) whose secret should be retrieved
     *
     * @return the client secret value
     *
     * @throws SetupException if the client secret cannot be retrieved
     */
    public static String getClientSecret(String httpsHostname, String userName, String password, String realm, String clientName) {
        String clientUuid = getClientUuid(httpsHostname, userName, password, realm, clientName);

        String result = new JsonObject(executeRequestAndReturnData(request(
            "GET", httpsHostname, ADMIN_REALMS_PATH + realm + "/clients/" + clientUuid + "/client-secret",  userName, password, null)))
            .getString("value");

        if (result.isEmpty()) {
            throw new SetupException("Cannot get client secret from keycloak api");
        }

        LOGGER.info("Client secret from keycloak api {}", result);
        return result;
    }

    /**
     * Resolves the internal UUID of a Keycloak client by its client ID (name).
     *
     * <p>This method queries the Keycloak Admin API for all clients in the specified
     * realm and searches for the client whose {@code clientId} matches the provided
     * name. Once found, the corresponding internal client UUID ({@code id}) is returned.</p>
     *
     * @param httpsHostname  Keycloak HTTPS hostname
     * @param userName       admin username used for authentication
     * @param password       admin password used for authentication
     * @param realm          realm containing the client
     * @param clientName     client ID (name) whose UUID should be retrieved
     *
     * @return the internal UUID of the client
     *
     * @throws SetupException if the client cannot be found or the response cannot be parsed
     */
    public static String getClientUuid(String httpsHostname, String userName, String password, String realm, String clientName) {
        String response = executeRequestAndReturnData(request(
            "GET", httpsHostname, ADMIN_REALMS_PATH + realm + "/clients/", userName, password, null));

        LOGGER.info("ClientId response keycloak api {}", response);

        try {
            JsonNode clientsArray = new ObjectMapper().readTree(response);
            return StreamSupport.stream(clientsArray.spliterator(), false)
                .filter(client -> {
                    JsonNode clientIdNode = client.get("clientId");
                    return clientIdNode != null && clientIdNode.textValue().equals(clientName);
                })
                .map(client -> client.get("id").textValue())
                .findFirst()
                .map(uuid -> {
                    LOGGER.info("ClientId from keycloak api {}", uuid);
                    return uuid;
                })
                .orElseThrow(() -> new SetupException("Cannot get clientId from keycloak api response"));
        } catch (JsonProcessingException e) {
            throw new SetupException("Keycloak client uuid response cannot be mapped to json node", e);
        }
    }

    /**
     * Builds a curl command for invoking the Keycloak Admin API.
     *
     * <p>The command includes the HTTP method, authentication token, and optional
     * JSON request body. The request is executed with {@code --insecure} to allow
     * HTTPS communication with self-signed certificates typically used in test
     * environments.</p>
     *
     * @param method        HTTP method to use (e.g. GET, POST, DELETE)
     * @param httpsHostname base HTTPS hostname of the Keycloak server
     * @param endpoint      API endpoint path
     * @param userName      admin username used to obtain the access token
     * @param password      admin password used to obtain the access token
     * @param body          optional JSON request body (may be {@code null})
     *
     * @return the curl command as an array of strings ready for execution
     */
    private static String[] request(String method, String httpsHostname, String endpoint, String userName, String password, String body) {
        List<String> cmd = new ArrayList<>(List.of("curl", "--insecure", "-X", method));
        String token = getToken(httpsHostname, userName, password);

        if (token != null) {
            cmd.addAll(List.of("-H", "Authorization: Bearer " + token));
        }
        if (body != null) {
            cmd.addAll(List.of("-H", "Content-Type: application/json", "-d", body));
        }
        cmd.add(httpsHostname + endpoint);

        return cmd.toArray(new String[0]);
    }

    /**
     * Obtains an access token from Keycloak using the admin CLI client.
     *
     * <p>Performs a password grant request against the Keycloak OIDC token endpoint
     * and extracts the {@code access_token} from the response.</p>
     *
     * @param httpsHostname Keycloak HTTPS hostname
     * @param userName      admin username
     * @param password      admin password
     *
     * @return access token used for authenticated Keycloak Admin API requests
     *
     * @throws SetupException if the token cannot be retrieved
     */
    public static String getToken(String httpsHostname, String userName, String password) {
        String token = new JsonObject(
            executeRequestAndReturnData(
                new String[]{"curl", "-v", "--insecure", "-X", "POST",
                    "-d", "client_id=admin-cli&grant_type=password&username=" + userName + "&password=" + password,
                    httpsHostname + OIDC_TOKEN_PATH
                }
            )).getString("access_token");

        if (token.isEmpty()) {
            throw new SetupException("Keycloak cannot get token");
        }

        return token;
    }

    /**
     * Executes a request command and returns the response body.
     *
     * <p>The request is retried until it succeeds or the configured timeout is reached.
     * Retries are triggered if the command output indicates a connection failure.</p>
     *
     * @param request the command to execute (typically a curl request)
     *
     * @return the response body returned by the command
     */
    public static String executeRequestAndReturnData(String[] request) {
        AtomicReference<String> response = new AtomicReference<>("");

        Wait.until("request to Keycloak API will be successful", KubeTestConstants.GLOBAL_POLL_INTERVAL_SHORT, KubeTestConstants.GLOBAL_TIMEOUT_MEDIUM, () -> {
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
}
