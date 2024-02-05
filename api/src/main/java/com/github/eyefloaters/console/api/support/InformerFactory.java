package com.github.eyefloaters.console.api.support;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.KafkaTopic;

@ApplicationScoped
public class InformerFactory {

    @Inject
    KubernetesClient k8s;

    @Produces
    @ApplicationScoped
    @Named("KafkaInformer")
    SharedIndexInformer<Kafka> kafkaInformer;

    @Produces
    @ApplicationScoped
    @Named("KafkaTopicInformer")
    SharedIndexInformer<KafkaTopic> topicInformer;

    @Produces
    @ApplicationScoped
    @Named("KafkaTopics")
    // Keys: namespace -> cluster name -> topic name
    Map<String, Map<String, Map<String, KafkaTopic>>> topics = new ConcurrentHashMap<>();

    /**
     * Initialize CDI beans produced by this factory. Executed on application startup.
     *
     * @param event CDI startup event
     */
    void onStartup(@Observes Startup event) {
        kafkaInformer = k8s.resources(Kafka.class).inAnyNamespace().inform();
        topicInformer = k8s.resources(KafkaTopic.class).inAnyNamespace().inform();
        topicInformer.addEventHandler(new ResourceEventHandler<KafkaTopic>() {
            @Override
            public void onAdd(KafkaTopic topic) {
                topicMap(topic).put(topic.getSpec().getTopicName(), topic);
            }

            @Override
            public void onUpdate(KafkaTopic oldTopic, KafkaTopic topic) {
                onAdd(topic);
            }

            @Override
            public void onDelete(KafkaTopic topic, boolean deletedFinalStateUnknown) {
                topicMap(topic).remove(topic.getSpec().getTopicName());
            }

            Map<String, KafkaTopic> topicMap(KafkaTopic topic) {
                String namespace = topic.getMetadata().getNamespace();
                String clusterName = topic.getMetadata().getAnnotations().get("strimzi.io/cluster");

                return topics.computeIfAbsent(namespace, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(clusterName, k -> new ConcurrentHashMap<>());
            }
        });
    }

    public void disposeKafkaInformer(@Disposes @Named("KafkaInformer") SharedIndexInformer<Kafka> informer) {
        informer.close();
    }

    public void disposeTopicInformer(@Disposes @Named("KafkaTopicInformer") SharedIndexInformer<KafkaTopic> informer) {
        informer.close();
    }
}
