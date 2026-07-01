package com.github.streamshub.systemtests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.skodjob.kubetest4j.KubeTestEnv;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class responsible for loading and providing UI text messages from a localized JSON resource file.
 * <p>
 * The {@code MessageStore} loads the English messages file at initialization and allows retrieval
 * of specific UI messages based on keys from the new Vite-based UI's i18n messages.
 * </p>
 * <p>
 * This class is used in systemtests to ensure displayed messages match expected values.
 * </p>
 * @throws SetupException if the messages file cannot be read or parsed.
 **/
public class MessageStore {
    private static final Logger LOGGER = LogWrapper.getLogger(MessageStore.class);

    private MessageStore() {}

    private static JsonNode jsonResources;
    private static final Path MESSAGES_PATH = Path.of(KubeTestEnv.USER_PATH + "/../api/src/main/webui/src/i18n/messages/en.json");

    static {
        try {
            LOGGER.info("initializing MessageStore");
            // Load the JSON file
            String source = Files.readString(MESSAGES_PATH);
            jsonResources = new ObjectMapper().readTree(source);
        } catch (IOException e) {
            throw new SetupException("Cannot load a file with messages", e);
        }
    }

    static final String COMMON = "common";
    static final String TOPICS = "topics";
    static final String OVERVIEW = "overview";
    static final String CLUSTER_CARD = "ClusterCard";
    static final String RECONCILIATION = "reconciliation";
    static final String KAFKA = "kafka";
    static final String GROUPS = "groups";
    
    public static String pauseReconciliationButton() {
        return jsonResources.get(RECONCILIATION).get("pause_reconciliation_button").textValue();
    }

    public static String pauseReconciliation() {
        return jsonResources.get(RECONCILIATION).get("pause_reconciliation").textValue();
    }

    public static String pauseReconciliationText() {
        return jsonResources.get(RECONCILIATION).get("pause_reconciliation_text").textValue();
    }

    public static String reconciliationConfirm() {
        return jsonResources.get(RECONCILIATION).get("confirm").textValue();
    }

    public static String reconciliationCancel() {
        return jsonResources.get(RECONCILIATION).get("cancel").textValue();
    }


    public static String reconciliationPausedWarning() {
        return jsonResources.get(RECONCILIATION).get("reconciliation_paused_warning").textValue();
    }

    public static String resumeReconciliation() {
        return jsonResources.get(RECONCILIATION).get("resume_reconciliation").textValue();
    }

    public static String clusterCardNoMessages() {
        return jsonResources.get(CLUSTER_CARD).get("no_messages").textValue();
    }

    public static String noDataTitle() {
        return jsonResources.get(TOPICS).get("messages").get("noDataTitle").textValue();
    }

    public static String noConsumerGroups() {
        return jsonResources.get(GROUPS).get("noGroups").textValue();
    }

    public static String groupsTitle() {
        return jsonResources.get(GROUPS).get("title").textValue();
    }

    public static String kafkaConnect() {
        return jsonResources.get(KAFKA).get("connect").get("title").textValue();
    }

}
