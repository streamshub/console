package com.github.streamshub.systemtests.utils.resources.kubernetes;

import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.fabric8.kubernetes.api.model.ServiceAccountList;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.skodjob.testframe.resources.KubeResourceManager;

import java.util.List;

public class ServiceAccountUtils {

    // -------------
    // Client
    // -------------
    private static MixedOperation<ServiceAccount, ServiceAccountList, io.fabric8.kubernetes.client.dsl.ServiceAccountResource> serviceAccountClient() {
        return KubeResourceManager.getKubeClient().getClient().serviceAccounts();
    }

    // -------------
    // Get
    // -------------
    public static ServiceAccount getServiceAccount(String namespaceName, String name) {
        return serviceAccountClient().inNamespace(namespaceName).withName(name).get();
    }

    // -------------
    // List
    // -------------
    public static List<ServiceAccount> listServiceAccounts(String namespace) {
        return serviceAccountClient().inNamespace(namespace).list().getItems();
    }
    public static List<ServiceAccount> listServiceAccountByContains(String namespace, String containsName) {
        return listServiceAccounts(namespace).stream().filter(sa -> sa.getMetadata().getName().contains(containsName)).toList();
    }


    // -------------
    // Delete
    // -------------
    public static void deleteServiceAccount(String namespace, String name) {
        serviceAccountClient().inNamespace(namespace).withName(name).delete();
    }
    public static void deleteServiceAccountByContains(String namespace, String containsName) {
        for (ServiceAccount sa : listServiceAccountByContains(namespace, containsName)) {
            deleteServiceAccount(namespace, sa.getMetadata().getName());
        }
    }
}
