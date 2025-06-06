package com.github.streamshub.systemtests;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import io.skodjob.testframe.TestFrameEnv;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility class responsible for loading and providing UI text messages from a localized JSON resource file.
 * <p>
 * The {@code MessageStore} loads the English messages file at initialization and allows retrieval
 * of specific UI messages based on keys. Placeholders such as <code>{product}</code> and <code>{brand}</code>
 * are replaced during initialization using values found in the JSON itself.
 * </p>
 * <p>
 * This class is systemtests to ensure displayed messages match expected values.
 * </p>
 * <p>
 * The expected JSON file is located at: {@code ../ui/messages/en.json}, relative to the user path.
 * </p>
 *
 * @throws SetupException if the messages file cannot be read or parsed.
 **/
public class MessageStore {
    private static final Logger LOGGER = LogWrapper.getLogger(MessageStore.class);

    private MessageStore() {}

    private static JsonNode jsonResources;
    private static final Path MESSAGES_PATH = Path.of(TestFrameEnv.USER_PATH + "/../ui/messages/en.json");

    static {
        try {
            LOGGER.info("initializing MessageStore");
            // Load the JSON to get product and brand
            String source = Files.readString(MESSAGES_PATH);
            jsonResources = new ObjectMapper().readTree(source);
            // Replace product and brand from the node and load the object again
            jsonResources = new ObjectMapper().readTree(source
                .replace("{product}", getProductName())
                .replace("{brand}", getBrand()));
        } catch (IOException e) {
            throw new SetupException("Cannot load a file with messages", e);
        }
    }

    static final String COMMON = "common";
    static final String HOMEPAGE = "homepage";
    static final String OVERVIEW = "overview";
    static final String CLUSTER_CARD = "ClusterCard";
    static final String PRODUCT = "product";
    static final String BRAND = "brand";
    static final String RECONCILIATION = "reconciliation";
    static final String MESSAGE_BROWSER = "message-browser";

    public static String getProductName() {
        return jsonResources.get(COMMON).get(PRODUCT).textValue();
    }

    public static String getBrand() {
        return jsonResources.get(COMMON).get(BRAND).textValue();
    }

    public static String emptyTopicsDescription() {
        return jsonResources.get(HOMEPAGE).get("empty_topics_description").textValue();
    }

    public static String lastAccessedTopics() {
        return jsonResources.get(HOMEPAGE).get("last_accessed_topics").textValue();
    }
    
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

    public static String reconciliationResume() {
        return jsonResources.get(RECONCILIATION).get("resume").textValue();
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
        return jsonResources.get(MESSAGE_BROWSER).get("no_data_title").textValue();
    }
}
