package com.github.streamshub.systemtests.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.setup.keycloak.KeycloakConfig;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.skodjob.testframe.TestFrameConstants;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.wait.Wait;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.Logger;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class KeycloakUtils {
    private static final Logger LOGGER = LogWrapper.getLogger(KeycloakUtils.class);

    private KeycloakUtils() {}

    private final static LabelSelector SCRAPER_SELECTOR = new LabelSelector(null, Map.of(Labels.APP, Constants.SCRAPER_NAME));

    public static String importRealm(String namespaceName, String baseURI, String token, String realmData) {
        final String testSuiteScraperPodName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, namespaceName, Constants.SCRAPER_NAME).get(0).getMetadata().getName();

        return executeRequestAndReturnData(
            namespaceName,
            testSuiteScraperPodName,
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

    public static String getClientSecret(String namespaceName, KeycloakConfig keycloakConfig, String realm, String clientName) {
        final String testSuiteScraperPodName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, namespaceName, Constants.SCRAPER_NAME).get(0).getMetadata().getName();
        final String clientUuid = getClientUuid(namespaceName, keycloakConfig, realm, clientName);
        final String token = getToken(namespaceName, keycloakConfig);
        return new JsonObject(executeRequestAndReturnData(
            namespaceName,
            testSuiteScraperPodName,
            new String[]{
                "curl",
                "--insecure",
                "-X",
                "GET",
                keycloakConfig.getHttpsUri() + "/admin/realms/" + realm + "/clients/" + clientUuid + "/client-secret",
                "-H", "Authorization: Bearer " + token
            }
        )).getString("value");
    }

    public static String getClientUuid(String namespaceName, KeycloakConfig keycloakConfig, String realm, String clientName) {
        final String testSuiteScraperPodName = ResourceUtils.listKubeResourcesByPrefix(Pod.class, namespaceName, Constants.SCRAPER_NAME).get(0).getMetadata().getName();
        final String token = getToken(namespaceName, keycloakConfig);

        String response = executeRequestAndReturnData(
            namespaceName,
            testSuiteScraperPodName,
            new String[]{
                "curl",
                "--insecure",
                "-X",
                "GET",
                keycloakConfig.getHttpsUri() + "/admin/realms/" + realm + "/clients/",
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

    public static void allowNetworkPolicyAllIngressForMatchingLabel(String namespaceName, String policyName, Map<String, String> matchLabels) {
        if (Environment.DEFAULT_TO_DENY_NETWORK_POLICIES) {
            LOGGER.info("Apply NetworkPolicy with Ingress to accept all connections to the Pods matching labels: {}", matchLabels.toString());

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

    public static String getToken(String namespaceName, KeycloakConfig keycloakConfig) {
        final String testSuiteScraperPodName = ResourceUtils.listKubeResourcesByLabelSelector(Pod.class, namespaceName, SCRAPER_SELECTOR).get(0).getMetadata().getName();

        return new JsonObject(
            executeRequestAndReturnData(
                namespaceName,
                testSuiteScraperPodName,
                new String[]{
                    "curl",
                    "-v",
                    "--insecure",
                    "-X",
                    "POST",
                    "-d", "client_id=admin-cli&grant_type=password&username=" + keycloakConfig.getUsername() + "&password=" + keycloakConfig.getPassword(),
                    keycloakConfig.getHttpsUri() + "/realms/master/protocol/openid-connect/token"
                }
            )).getString("access_token");
    }

    public static String executeRequestAndReturnData(String namespaceName, String scraperPodName, String[] request) {
        AtomicReference<String> response = new AtomicReference<>("");

        Wait.until("request to Keycloak API will be successful", TestFrameConstants.GLOBAL_POLL_INTERVAL_SHORT, TestFrameConstants.GLOBAL_TIMEOUT_MEDIUM, () -> {
            try {
                String commandOutput = KubeResourceManager.get().kubeCmdClient().inNamespace(namespaceName).execInPod(scraperPodName, request).out().trim();

                if (!commandOutput.contains("Connection refused")) {
                    response.set(commandOutput);
                    return true;
                }

                return false;
            } catch (Exception e) {
                LOGGER.warn("Exception occurred during doing request on Keycloak API: " + e.getMessage());
                return false;
            }
        });

        return response.get();
    }

    public static String getKeycloakHostname(Boolean https) {
        return (Boolean.TRUE.equals(https) ? "https://"  : "") + Constants.KEYCLOAK_HOSTNAME_PREFIX + "." + ClusterUtils.getClusterDomain();
    }

    public static String getKeycloakRealmUri(String realm) {
        return getKeycloakHostname(true) + "/realms/" + realm;
    }
}
