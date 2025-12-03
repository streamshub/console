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

import com.github.streamshub.console.api.service.StrimziResourceService;
import com.github.streamshub.console.config.ConsoleConfig;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.client.KubernetesClientException;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import io.strimzi.api.kafka.model.topic.KafkaTopicSpec;

@ApplicationScoped
public class InformerFactory {

    private static final String STRIMZI_CLUSTER = "strimzi.io/cluster";
    private static final Logger LOGGER = Logger.getLogger(InformerFactory.class);

    @Inject
    ConsoleConfig consoleConfig;

    @Inject
    StrimziResourceService strimziService;

    @Produces
    @ApplicationScoped
    @Named("KafkaInformer")
    Holder<SharedIndexInformer<Kafka>> kafkaInformer = Holder.empty();

    @Produces
    @ApplicationScoped
    @Named("KafkaNodePools")
    // Keys: namespace -> cluster name -> pool name
    Map<String, Map<String, Map<String, KafkaNodePool>>> nodePools = new ConcurrentHashMap<>();

    @Produces
    @ApplicationScoped
    @Named("KafkaTopics")
    // Keys: namespace -> cluster name -> topic name
    Map<String, Map<String, Map<String, KafkaTopic>>> topics = new ConcurrentHashMap<>();

    SharedIndexInformer<KafkaNodePool> kafkaNodePoolInformer;
    SharedIndexInformer<? extends KafkaTopic> topicInformer;

    /**
     * Initialize CDI beans produced by this factory. Executed on application startup.
     *
     * @param event CDI startup event
     */
    void onStartup(@Observes Startup event) {
        if (consoleConfig.getKubernetes().isEnabled()) {
            try {
                kafkaInformer = Holder.of(strimziService.informKafkas());
            } catch (KubernetesClientException e) {
                LOGGER.warnf("Failed to create Strimzi Kafka informer: %s", e.getMessage());
            }

            try {
                kafkaNodePoolInformer = strimziService.informKafkaNodePools();
                kafkaNodePoolInformer.addEventHandler(new KafkaNodePoolEventHandler(nodePools));
            } catch (KubernetesClientException e) {
                LOGGER.warnf("Failed to create Strimzi KafkaNodePool informer: %s", e.getMessage());
            }

            try {
                topicInformer = strimziService.informKafkaTopics();
                topicInformer.addEventHandler(new KafkaTopicEventHandler(topics));
            } catch (KubernetesClientException e) {
                LOGGER.warnf("Failed to create Strimzi KafkaTopic informer: %s", e.getMessage());
            }
        } else {
            LOGGER.warn("Kubernetes client connection is disabled. Custom resource information will not be available.");
        }
    }

    void disposeKafkaInformer(@Disposes @Named("KafkaInformer") Holder<SharedIndexInformer<Kafka>> informer) {
        informer.ifPresent(SharedIndexInformer::close);
    }

    /**
     * Close the KafkaNodePool informer used to update the node pool map being disposed.
     *
     * @param nodePools map of KafkaNodePools being disposed.
     */
    void disposeKafkaNodePools(@Disposes Map<String, Map<String, Map<String, KafkaNodePool>>> nodePools) {
        if (kafkaNodePoolInformer != null) {
            kafkaNodePoolInformer.close();
        }
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

    private abstract static class EventHandler<T extends HasMetadata> implements ResourceEventHandler<T> {
        Map<String, Map<String, Map<String, T>>> items;

        EventHandler(Map<String, Map<String, Map<String, T>>> items) {
            this.items = items;
        }

        protected String name(T item) {
            return item.getMetadata().getName();
        }

        @Override
        public void onAdd(T item) {
            log(item, "add");
            map(item).ifPresent(map -> map.put(name(item), item));
        }

        @Override
        public void onUpdate(T oldItem, T item) {
            log(item, "update");
            onDelete(oldItem, false);
            onAdd(item);
        }

        @Override
        public void onDelete(T item, boolean deletedFinalStateUnknown) {
            log(item, "delete");
            map(item).ifPresent(map -> map.remove(name(item)));
        }

        void log(T item, String event) {
            LOGGER.debugf("Event '%s' for %s resource %s/%s",
                    event,
                    item.getClass().getSimpleName(),
                    item.getMetadata().getNamespace(),
                    item.getMetadata().getName());
        }

        Optional<Map<String, T>> map(T item) {
            String namespace = item.getMetadata().getNamespace();
            String clusterName = item.getMetadata().getLabels().get(STRIMZI_CLUSTER);

            if (clusterName == null) {
                LOGGER.warnf("%s %s/%s is missing label %s and will be ignored",
                        item.getClass().getSimpleName(),
                        namespace,
                        item.getMetadata().getName(),
                        STRIMZI_CLUSTER);
                return Optional.empty();
            }

            Map<String, T> map = items.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(clusterName, k -> new ConcurrentHashMap<>());

            return Optional.of(map);
        }
    }

    private static class KafkaNodePoolEventHandler extends EventHandler<KafkaNodePool> {
        public KafkaNodePoolEventHandler(Map<String, Map<String, Map<String, KafkaNodePool>>> nodePools) {
            super(nodePools);
        }
    }

    private static class KafkaTopicEventHandler extends EventHandler<KafkaTopic> {
        public KafkaTopicEventHandler(Map<String, Map<String, Map<String, KafkaTopic>>> topics) {
            super(topics);
        }

        @Override
        protected String name(KafkaTopic topic) {
            return Optional.ofNullable(topic.getSpec())
                    .map(KafkaTopicSpec::getTopicName)
                    .orElseGet(() -> super.name(topic));
        }
    }
}
