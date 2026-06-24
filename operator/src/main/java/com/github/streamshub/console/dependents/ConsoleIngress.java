package com.github.streamshub.console.dependents;

import java.util.Collection;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import com.github.streamshub.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.IngressLoadBalancerStatus;
import io.fabric8.kubernetes.api.model.networking.v1.IngressStatus;
import io.fabric8.openshift.api.model.Route;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

import static com.github.streamshub.console.dependents.support.ConfigSupport.resourceSupported;

@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class ConsoleIngress extends CRUDKubernetesDependentResource<Ingress, Console> implements ConsoleResource<Ingress> {

    public static final String NAME = "console-ingress";

    @Inject
    ConsoleService service;

    public ConsoleIngress() {
        super(Ingress.class);
    }

    @Override
    public String resourceName() {
        return NAME;
    }

    @Override
    protected Ingress desired(Console primary, Context<Console> context) {
        boolean tls = primary.getSpec().getTls() != null;
        String backendProtocol = tls ? "HTTPS" : "HTTP";
        int servicePort = tls ? 443 : 80;
        String serviceName = service.instanceName(primary);

        return load(context, "console.ingress.yaml", Ingress.class)
            .edit()
            .editMetadata()
                .withName(instanceName(primary))
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(commonLabels("console"))
                .addToAnnotations("nginx.ingress.kubernetes.io/backend-protocol", backendProtocol)
            .endMetadata()
            .editSpec()
                // Plain Kubernetes (non-OCP) clusters don't need a class name; the
                // default ingress controller picks it up automatically.
                .withIngressClassName(null)
                .editDefaultBackend()
                    .editService()
                        .withName(serviceName)
                        .editPort()
                            .withNumber(servicePort)
                        .endPort()
                    .endService()
                .endDefaultBackend()
                .editFirstRule()
                    .withHost(primary.getSpec().getHostname())
                    .editHttp()
                        .editFirstPath()
                            .editBackend()
                                .editService()
                                    .withName(serviceName)
                                    .editPort()
                                        .withNumber(servicePort)
                                    .endPort()
                                .endService()
                            .endBackend()
                        .endPath()
                    .endHttp()
                .endRule()
            .endSpec()
            .build();
    }

    /**
     * Only create the plain Ingress on clusters that do NOT support OpenShift
     * Routes. On OpenShift / MicroShift, {@link ConsoleRoute} is used instead.
     */
    public static class Precondition implements Condition<Ingress, Console> {
        @Override
        public boolean isMet(DependentResource<Ingress, Console> dependentResource, Console primary, Context<Console> context) {
            return !resourceSupported(context, Route.class);
        }
    }

    public static class Postcondition implements Condition<Ingress, Console> {
        private static final Logger LOGGER = Logger.getLogger(Postcondition.class);

        @Override
        public boolean isMet(DependentResource<Ingress, Console> dependentResource,
                Console primary,
                Context<Console> context) {

            boolean ready = dependentResource.getSecondaryResource(primary, context).map(this::isReady).orElse(false);
            context.managedWorkflowAndDependentResourceContext().put(NAME, ready);
            return ready;
        }

        private boolean isReady(Ingress ingress) {
            Boolean ready = Optional.ofNullable(ingress.getStatus())
                .map(IngressStatus::getLoadBalancer)
                .map(IngressLoadBalancerStatus::getIngress)
                .map(Collection::isEmpty)
                .map(Boolean.FALSE::equals)
                .orElse(false);

            LOGGER.debugf("Ingress %s ready: %s", ingress.getMetadata().getName(), ready);
            return ready;
        }
    }
}
