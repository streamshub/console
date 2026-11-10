package com.github.streamshub.console.dependents.conditions;

import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.dependents.ConsoleResource;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.informers.cache.Cache;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/**
 * Ready postcondition for Services. When the Service is annotated with
 * {@code service.beta.openshift.io/serving-cert-secret-name}, blocks downstream
 * dependents until the OpenShift service CA controller has populated the named
 * Secret with non-blank {@code tls.crt} and {@code tls.key} entries. A Service without the
 * annotation (i.e. not running on OpenShift) is ready as soon as it exists.
 */
public class ServingCertReadyCondition implements Condition<Service, Console> {

    public static final String ANNOTATION = "service.beta.openshift.io/serving-cert-secret-name";

    private static final Logger LOGGER = Logger.getLogger(ServingCertReadyCondition.class);

    @Override
    public boolean isMet(DependentResource<Service, Console> dependentResource,
            Console primary,
            Context<Console> context) {

        return dependentResource.getSecondaryResource(primary, context)
                .map(svc -> isServingCertReady(dependentResource, svc, primary, context))
                .orElse(false);
    }

    private boolean isServingCertReady(DependentResource<Service, Console> dependentResource,
            Service service, Console primary, Context<Console> context) {

        Map<String, String> annotations = service.getMetadata().getAnnotations();
        String secretName = annotations != null ? annotations.get(ANNOTATION) : null;

        if (secretName == null) {
            LOGGER.debugf("Service %s without annotation %s is ready", Cache.metaNamespaceKeyFunc(service), ANNOTATION);
            return true;
        }

        Secret tlsSecret = context.getClient().secrets()
                .inNamespace(primary.getMetadata().getNamespace())
                .withName(secretName)
                .get();

        var data = Optional.ofNullable(tlsSecret).map(Secret::getData);
        var certificate = data
                .map(d -> d.get("tls.crt"))
                .filter(cert -> !cert.isBlank());
        // Both entries are mounted into the pod, so wait for the key as well
        var keyPresent = data
                .map(d -> d.get("tls.key"))
                .filter(key -> !key.isBlank())
                .isPresent();
        var certificateReady = certificate.isPresent() && keyPresent;

        if (certificateReady && dependentResource instanceof ConsoleResource<Service> consoleService) {
            // Only the public certificate is digested, never the private key. The key is
            // reissued together with the certificate, so the certificate identifies a change.
            consoleService.putDigest(context, consoleService.digestName(), Map.of("tls.crt", certificate.get()));
        }

        LOGGER.debugf(
                "Serving certificate '%s' for service '%s' is ready: %s",
                Cache.metaNamespaceKeyFunc(tlsSecret),
                Cache.metaNamespaceKeyFunc(service),
                certificateReady
        );

        return certificateReady;
    }
}
