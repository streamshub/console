package com.github.streamshub.systemtests.resourcetypes.prometheus;

import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.KubernetesResourceList;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.dsl.MixedOperation;
import io.fabric8.kubernetes.client.dsl.NonNamespaceOperation;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.openshift.api.model.monitoring.v1.ServiceMonitor;
import io.skodjob.kubetest4j.interfaces.ResourceType;

import java.util.function.Consumer;

public class ServiceMonitorType implements ResourceType<ServiceMonitor> {

    public static MixedOperation<ServiceMonitor, KubernetesResourceList<ServiceMonitor>, Resource<ServiceMonitor>> serviceMonitorClient() {
        return ResourceUtils.getKubeResourceClient(ServiceMonitor.class);
    }

    @Override
    public String getKind() {
        return HasMetadata.getKind(ServiceMonitor.class);
    }

    @Override
    public NonNamespaceOperation<?, ?, ?> getClient() {
        return serviceMonitorClient();
    }

    @Override
    public void create(ServiceMonitor serviceMonitor) {
        serviceMonitorClient().resource(serviceMonitor).create();
    }

    @Override
    public void update(ServiceMonitor serviceMonitor) {
        serviceMonitorClient().resource(serviceMonitor).update();
    }

    @Override
    public void delete(ServiceMonitor serviceMonitor) {
        serviceMonitorClient().resource(serviceMonitor).delete();
    }

    @Override
    public void replace(ServiceMonitor serviceMonitor, Consumer<ServiceMonitor> consumer) {
        ServiceMonitor toBeReplaced = serviceMonitorClient().withName(serviceMonitor.getMetadata().getName()).get();
        consumer.accept(toBeReplaced);
        update(toBeReplaced);
    }

    @Override
    public boolean isReady(ServiceMonitor serviceMonitor) {
        return serviceMonitorClient().inNamespace(serviceMonitor.getMetadata().getNamespace()).withName(serviceMonitor.getMetadata().getName()).get() != null;
    }

    @Override
    public boolean isDeleted(ServiceMonitor serviceMonitor) {
        return serviceMonitorClient().inNamespace(serviceMonitor.getMetadata().getNamespace()).withName(serviceMonitor.getMetadata().getName()).get() == null;
    }

    public static boolean isAvailable() {
        return ResourceUtils.getKubeResource(CustomResourceDefinition.class, HasMetadata.getPlural(ServiceMonitor.class) + "." + HasMetadata.getGroup(ServiceMonitor.class)) != null;
    }
}