package com.github.eyefloaters.console.api.support;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Startup;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.strimzi.api.kafka.model.Kafka;

@ApplicationScoped
public class InformerFactory {

    @Inject
    KubernetesClient k8s;

    @Produces
    @ApplicationScoped
    @Named("KafkaInformer")
    SharedIndexInformer<Kafka> kafkaInformer;

    /**
     * Initialize CDI beans produced by this factory. Executed on application startup.
     *
     * @param event CDI startup event
     */
    void onStartup(@Observes Startup event) {
        kafkaInformer = k8s.resources(Kafka.class).inAnyNamespace().inform();
    }

    public void disposeKafkaInformer(@Disposes @Named("KafkaInformer") SharedIndexInformer<Kafka> informer) {
        informer.close();
    }
}
