package com.github.streamshub.systemtests.utils.resourceutils.keycloak;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.enums.LogLevel;
import io.skodjob.testframe.executor.Exec;
import io.skodjob.testframe.wait.Wait;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class KeycloakApiUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KeycloakApiUtils.class);

    private KeycloakApiUtils() {}

    private static final String ADMIN_REALMS_PATH = "/admin/realms/";
    private static final String OIDC_TOKEN_PATH = "/realms/master/protocol/openid-connect/token"; // NOSONAR

    public static boolean realmExists(String httpsHostname, String userName, String password, String realmName) {
        String result = executeRequestAndReturnData(request(
            "GET", httpsHostname, ADMIN_REALMS_PATH + realmName, userName, password, null));
        return !result.contains("Realm not found") && !result.isEmpty();
    }

    public static String deleteRealm(String httpsHostname, String userName, String password, String realmName) {
        String result = executeRequestAndReturnData(request(
            "DELETE", httpsHostname, ADMIN_REALMS_PATH + realmName, userName, password, null));

        if (!result.contains("Realm not found")) {
            throw new SetupException("Keycloak realm was not deleted");
        }

        LOGGER.info("Deleted Keycloak realm '{}'", realmName);
        return result;
    }

    public static String importRealm(String httpsHostname, String userName, String password, String realmData) {

        String result = executeRequestAndReturnData(request(
            "POST", httpsHostname, ADMIN_REALMS_PATH,  userName, password, realmData));

        if (!result.isEmpty() && !result.contains("already exists")) {
            throw new SetupException("Console realm was not imported: " + result);
        }

        LOGGER.info("Console realm successfully imported");
        return result;
    }

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

    public static String getClientUuid(String httpsHostname, String userName, String password, String realm, String clientName) {
        String response = executeRequestAndReturnData(request(
            "GET", httpsHostname, ADMIN_REALMS_PATH + realm + "/clients/", userName, password, null));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode clientsArray;

        try {
            clientsArray = mapper.readTree(response);
        } catch (JsonProcessingException e) {
            throw new SetupException("Keycloak client uuid response cannot be mapped to json node", e);
        }

        LOGGER.info("ClientId response keycloak api {}", response);

        String clientUuid = "";
        for (JsonNode client : clientsArray) {
            JsonNode clientIdNode = client.get("clientId");
            if (clientIdNode != null && clientIdNode.textValue().equals(clientName)) {
                clientUuid = client.get("id").textValue();
                break;
            }
        }

        if (clientUuid.isEmpty()) {
            throw new SetupException("Cannot get clientId from keycloak api response");
        }

        LOGGER.info("ClientId from keycloak api {}", clientUuid);
        return clientUuid;
    }

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
}
