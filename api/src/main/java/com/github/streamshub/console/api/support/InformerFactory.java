package com.github.streamshub.console.api.support;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.jboss.logging.Logger;

import com.github.streamshub.console.config.ConsoleConfig;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import io.strimzi.api.kafka.model.topic.KafkaTopicSpec;

@ApplicationScoped
public class InformerFactory {

    private static final String STRIMZI_CLUSTER = "strimzi.io/cluster";

    @Inject
    Logger logger;

    @Inject
    KubernetesClient k8s;

    @Inject
    ConsoleConfig consoleConfig;

    @Produces
    @ApplicationScoped
    @Named("KafkaInformer")
    Holder<SharedIndexInformer<Kafka>> kafkaInformer = Holder.empty();

    @Produces
    @ApplicationScoped
    @Named("KafkaTopics")
    // Keys: namespace -> cluster name -> topic name
    Map<String, Map<String, Map<String, KafkaTopic>>> topics = new ConcurrentHashMap<>();

    SharedIndexInformer<KafkaTopic> topicInformer;

    /**
     * Initialize CDI beans produced by this factory. Executed on application startup.
     *
     * @param event CDI startup event
     */
    void onStartup(@Observes Startup event) {
        if (consoleConfig.getKubernetes().isEnabled()) {
            var kafkaResources = k8s.resources(Kafka.class).inAnyNamespace();

            try {
                kafkaInformer = Holder.of(kafkaResources.inform());
            } catch (KubernetesClientException e) {
                logger.warnf("Failed to create Strimzi Kafka informer: %s", e.getMessage());
            }

            try {
                topicInformer = k8s.resources(KafkaTopic.class).inAnyNamespace().inform();
                topicInformer.addEventHandler(new KafkaTopicEventHandler(topics));
            } catch (KubernetesClientException e) {
                logger.warnf("Failed to create Strimzi KafkaTopic informer: %s", e.getMessage());
            }
        } else {
            logger.warn("Kubernetes client connection is disabled. Custom resource information will not be available.");
        }
    }

    void disposeKafkaInformer(@Disposes @Named("KafkaInformer") Holder<SharedIndexInformer<Kafka>> informer) {
        informer.ifPresent(SharedIndexInformer::close);
    }

    /**
     * Close the KafkaTopic informer used to update the topics map being disposed.
     *
     * @param topics map of KafkaTopics being disposed.
     */
    void disposeKafkaTopics(@Disposes Map<String, Map<String, Map<String, KafkaTopic>>> topics) {
        if (topicInformer != null) {
            topicInformer.close();
        }
    }

    private class KafkaTopicEventHandler implements ResourceEventHandler<KafkaTopic> {
        Map<String, Map<String, Map<String, KafkaTopic>>> topics;

        public KafkaTopicEventHandler(Map<String, Map<String, Map<String, KafkaTopic>>> topics) {
            this.topics = topics;
        }

        @Override
        public void onAdd(KafkaTopic topic) {
            topicMap(topic).ifPresent(map -> map.put(topicName(topic), topic));
        }

        @Override
        public void onUpdate(KafkaTopic oldTopic, KafkaTopic topic) {
            onDelete(oldTopic, false);
            onAdd(topic);
        }

        @Override
        public void onDelete(KafkaTopic topic, boolean deletedFinalStateUnknown) {
            topicMap(topic).ifPresent(map -> map.remove(topicName(topic)));
        }

        private static String topicName(KafkaTopic topic) {
            return Optional.ofNullable(topic.getSpec())
                    .map(KafkaTopicSpec::getTopicName)
                    .orElseGet(() -> topic.getMetadata().getName());
        }

        Optional<Map<String, KafkaTopic>> topicMap(KafkaTopic topic) {
            String namespace = topic.getMetadata().getNamespace();
            String clusterName = topic.getMetadata().getLabels().get(STRIMZI_CLUSTER);

            if (clusterName == null) {
                logger.warnf("KafkaTopic %s/%s is missing label %s and will be ignored",
                        namespace,
                        topic.getMetadata().getName(),
                        STRIMZI_CLUSTER);
                return Optional.empty();
            }

            Map<String, KafkaTopic> map = topics.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(clusterName, k -> new ConcurrentHashMap<>());

            return Optional.of(map);
        }
    }

}
