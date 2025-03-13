package com.github.streamshub.systemtests.utils.resources.openshift;

import io.fabric8.kubernetes.api.model.DeletionPropagation;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.Subscription;
import io.fabric8.openshift.api.model.operatorhub.v1alpha1.SubscriptionList;
import io.fabric8.openshift.client.OpenShiftClient;
import io.skodjob.testframe.resources.KubeResourceManager;

public class SubscriptionResource {

    // -------------
    // Client
    // -------------
    private static MixedOperation<Subscription, SubscriptionList, Resource<Subscription>> subscriptionClient() {
        return KubeResourceManager.getKubeClient().getClient().adapt(OpenShiftClient.class).operatorHub().subscriptions();
    }

    // -------------
    // Get
    // -------------
    public static Subscription getSubscription(String namespace, String name) {
        return subscriptionClient().inNamespace(namespace).withName(name).get();
    }

    // -------------
    // Delete
    // -------------
    public static void delete(String namespace, String subscriptionName) {
        subscriptionClient().inNamespace(namespace).withName(subscriptionName).withPropagationPolicy(DeletionPropagation.FOREGROUND).delete();
    }

    // -------------
    // Update
    // -------------
    public static void updateSubscription(Subscription resource) {
        subscriptionClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).update();
    }

    // -------------
    // Create
    // -------------
    public static void createSubscription(Subscription resource) {
        subscriptionClient().inNamespace(resource.getMetadata().getNamespace()).resource(resource).create();
    }
}
