package com.github.streamshub.console.dependents;

import java.util.Optional;

import jakarta.inject.Inject;

import com.github.streamshub.console.ReconciliationException;
import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.dependents.support.ConfigSupport;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.TLSConfig;
import io.fabric8.openshift.api.model.TLSConfigBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;

abstract class BaseRoute extends CRUDKubernetesDependentResource<Route, Console>
        implements ConsoleResource<Route> {

    private final String resourceName;

    @Inject
    ConsoleTrustBundle trustBundle;

    @Inject
    ConsoleTlsSecret tlsSecret;

    protected BaseRoute(String resourceName) {
        super(Route.class);
        this.resourceName = resourceName;
    }

    @Override
    public String resourceName() {
        return resourceName;
    }

    @Override
    public Optional<Route> getSecondaryResource(Console primary, Context<Console> context) {
        return ConsoleResource.super.getSecondaryResource(primary, context);
    }

    protected TLSConfig buildTlsConfig(Console primary, Context<Console> context) {
        if (ConfigSupport.userProvidedPassthroughTLS(primary)) {
            // The console terminates TLS itself; the router passes through the connection
            return new TLSConfigBuilder()
                    .withTermination("passthrough")
                    .build();
        } else {
            // Routes only exist where the Route API is available (OpenShift), so the service
            // CA is always available: re-encrypt rather than terminating TLS at the edge with
            // plain HTTP to the backend.
            // Read the CA bundle injected by the service CA controller for reencrypt termination
            String caBundlePem = trustBundle.getSecondaryResource(primary, context)
                    .map(ConfigMap::getData)
                    .map(data -> data.get(ConsoleTrustBundle.CA_BUNDLE_KEY))
                    .orElseThrow(() -> new ReconciliationException(
                            "Trust bundle or %s entry is missing".formatted(ConsoleTrustBundle.CA_BUNDLE_KEY)
                    ));

            var tlsBuilder = new TLSConfigBuilder()
                    .withTermination("reencrypt")
                    .withDestinationCACertificate(caBundlePem)
                    .withInsecureEdgeTerminationPolicy("Redirect");

            if (ConsoleTlsSecret.required(primary, context)) {
                tlsBuilder.withNewExternalCertificate()
                        .withName(tlsSecret.instanceName(primary))
                        .endExternalCertificate();
            }

            return tlsBuilder.build();
        }
    }
}
