package com.github.streamshub.systemtests.clients;

import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Labels;
import io.fabric8.kubernetes.api.model.LocalObjectReference;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.sundr.builder.annotations.Buildable;

import java.security.InvalidParameterException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Buildable(editableEnabled = false)
public class KafkaClients {

    private String producerName;
    private String consumerName;
    private String message;
    private String messageKey;
    private int messageCount;
    private String consumerGroup;
    private long delayMs;
    private String username;
    private String headers;
    private String bootstrapAddress;
    private String topicName;
    private String additionalConfig;
    private String namespaceName;

    public String getProducerName() {
        return producerName;
    }
    public String getConsumerName() {
        return consumerName;
    }

    public String getMessage() {
        return message;
    }
    public String getMessageKey() {
        return messageKey;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public String getConsumerGroup() {
        return consumerGroup;
    }
    public String getAdditionalConfig() {
        return additionalConfig;
    }

    public long getDelayMs() {
        return delayMs;
    }

    public String getUsername() {
        return username;
    }

    public String getHeaders() {
        return headers;
    }

    public String getBootstrapAddress() {
        return bootstrapAddress;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getNamespaceName() {
        return namespaceName;
    }

    public void setProducerName(String producerName) {
        this.producerName = producerName;
    }

    public void setConsumerName(String consumerName) {
        this.consumerName = consumerName;
    }

    public void setMessage(String message) {
        if (message == null || message.isEmpty()) {
            message = "Hello-world";
        }
        this.message = message;
    }

    public void setMessageKey(String messageKey) {
        if (messageKey == null || messageKey.isEmpty()) {
            messageKey = "DefaultKey";
        }
        this.messageKey = messageKey;
    }

    public void setMessageCount(int messageCount) {
        if (messageCount <= 0) {
            throw new InvalidParameterException("Message count is less than 1");
        }
        this.messageCount = messageCount;
    }

    public void setConsumerGroup(String consumerGroup) {
        this.consumerGroup = consumerGroup;
    }

    public void setDelayMs(long delayMs) {
        this.delayMs = delayMs;
    }


    public void setUsername(String username) {
        this.username = username;
    }


    public void setHeaders(String headers) {
        this.headers = headers;
    }

    public void setBootstrapAddress(String bootstrapAddress) {
        if (bootstrapAddress == null || bootstrapAddress.isEmpty()) {
            throw new InvalidParameterException("Bootstrap server is not set.");
        }
        this.bootstrapAddress = bootstrapAddress;
    }

    public void setTopicName(String topicName) {
        if (topicName == null || topicName.isEmpty()) {
            throw new InvalidParameterException("Topic name is not set.");
        }
        this.topicName = topicName;
    }

    public void setNamespaceName(String namespaceName) {
        this.namespaceName = namespaceName;
    }

    public void setAdditionalConfig(String additionalConfig) {
        this.additionalConfig = (additionalConfig == null || additionalConfig.isEmpty()) ? "" : additionalConfig;
    }

    public Job consumer() {
        return defaultConsumer()
            .editSpec()
                .withBackoffLimit(10)
            .endSpec()
            .build();
    }

    public Job producer() {
        return defaultProducer()
            .editSpec()
                .withBackoffLimit(5)
            .endSpec()
            .build();
    }

    public List<LocalObjectReference> getClientsPullSecret() {
        return Collections.singletonList(new LocalObjectReference(Environment.TEST_CLIENTS_PULL_SECRET));
    }

    public JobBuilder defaultProducer() {
        if (producerName == null || producerName.isEmpty()) {
            throw new InvalidParameterException("Producer name is not set.");
        }

        Map<String, String> producerLabels = new HashMap<>();
        producerLabels.put(Labels.APP, producerName);
        producerLabels.put(Labels.KAFKA_CLIENTS_LABEL_KEY, Labels.KAFKA_CLIENTS_LABEL_VALUE);

        final JobBuilder builder = new JobBuilder()
            .withNewMetadata()
                .withNamespace(this.getNamespaceName())
                .withLabels(producerLabels)
                .withName(producerName)
            .endMetadata()
            .withNewSpec()
                .withBackoffLimit(0)
                .withNewTemplate()
                    .withNewMetadata()
                        .withName(producerName)
                        .withNamespace(this.getNamespaceName())
                        .withLabels(producerLabels)
                    .endMetadata()
                    .withNewSpec()
                        .withImagePullSecrets(Environment.isTestClientsPullSecretPresent() ? getClientsPullSecret() : null)
                        .withRestartPolicy("Never")
                        .withContainers()
                            .addNewContainer()
                            .withName(producerName)
                            .withImagePullPolicy("IfNotPresent")
                            .withImage(Environment.TEST_CLIENTS_IMAGE)
                            .addNewEnv()
                                .withName("BOOTSTRAP_SERVERS")
                                .withValue(this.getBootstrapAddress())
                            .endEnv()
                            .addNewEnv()
                                .withName("TOPIC")
                                .withValue(this.getTopicName())
                            .endEnv()
                            .addNewEnv()
                                .withName("DELAY_MS")
                                .withValue(String.valueOf(delayMs))
                            .endEnv()
                            .addNewEnv()
                                .withName("LOG_LEVEL")
                                .withValue("DEBUG")
                            .endEnv()
                            .addNewEnv()
                                .withName("MESSAGE_COUNT")
                                .withValue(String.valueOf(messageCount))
                            .endEnv()
                            .addNewEnv()
                                .withName("MESSAGE")
                                .withValue(message)
                            .endEnv()
                            .addNewEnv()
                                .withName("MESSAGE_KEY")
                                .withValue(messageKey)
                            .endEnv()
                            .addNewEnv()
                                .withName("PRODUCER_ACKS")
                                .withValue("all")
                            .endEnv()
                            .addNewEnv()
                                .withName("ADDITIONAL_CONFIG")
                                .withValue(this.getAdditionalConfig())
                            .endEnv()
                            .addNewEnv()
                                .withName("BLOCKING_PRODUCER")
                                .withValue("true")
                            .endEnv()
                            .addNewEnv()
                                .withName("CLIENT_TYPE")
                                .withValue("KafkaProducer")
                            .endEnv()
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec();

        if (this.getHeaders() != null) {
            builder
                .editSpec()
                    .editTemplate()
                        .editSpec()
                            .editFirstContainer()
                                .addNewEnv()
                                    .withName("HEADERS")
                                    .withValue(this.getHeaders())
                                .endEnv()
                            .endContainer()
                        .endSpec()
                    .endTemplate()
                .endSpec();
        }

        return builder;
    }

    public JobBuilder defaultConsumer() {
        if (consumerName == null || consumerName.isEmpty()) {
            throw new InvalidParameterException("Consumer name is not set.");
        }

        return new JobBuilder()
            .withNewMetadata()
                .withNamespace(this.getNamespaceName())
                .withLabels(Labels.getClientsLabels(consumerName))
                .withName(consumerName)
            .endMetadata()
            .withNewSpec()
                .withBackoffLimit(0)
                .withNewTemplate()
                    .withNewMetadata()
                        .withLabels(Labels.getClientsLabels(consumerName))
                        .withNamespace(this.getNamespaceName())
                        .withName(consumerName)
                    .endMetadata()
                    .withNewSpec()
                        .withImagePullSecrets(Environment.isTestClientsPullSecretPresent() ? getClientsPullSecret() : null)
                        .withRestartPolicy("Never")
                        .withContainers()
                            .addNewContainer()
                                .withName(consumerName)
                                .withImagePullPolicy("IfNotPresent")
                                .withImage(Environment.TEST_CLIENTS_IMAGE)
                                .addNewEnv()
                                    .withName("BOOTSTRAP_SERVERS")
                                    .withValue(this.getBootstrapAddress())
                                .endEnv()
                                .addNewEnv()
                                    .withName("TOPIC")
                                    .withValue(this.getTopicName())
                                .endEnv()
                                .addNewEnv()
                                    .withName("DELAY_MS")
                                    .withValue(String.valueOf(delayMs))
                                .endEnv()
                                .addNewEnv()
                                    .withName("LOG_LEVEL")
                                    .withValue("DEBUG")
                                .endEnv()
                                .addNewEnv()
                                    .withName("MESSAGE_COUNT")
                                    .withValue(String.valueOf(messageCount))
                                .endEnv()
                                .addNewEnv()
                                    .withName("GROUP_ID")
                                    .withValue(consumerGroup)
                                .endEnv()
                                .addNewEnv()
                                    .withName("ADDITIONAL_CONFIG")
                                    .withValue(this.getAdditionalConfig())
                                .endEnv()
                                .addNewEnv()
                                    .withName("CLIENT_TYPE")
                                    .withValue("KafkaConsumer")
                                .endEnv()
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec();
    }
}

