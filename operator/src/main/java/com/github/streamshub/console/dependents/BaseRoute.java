package com.github.streamshub.console.dependents;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;

import jakarta.inject.Inject;

import com.github.streamshub.console.ReconciliationException;
import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.spec.Tls;
import com.github.streamshub.console.api.v1alpha1.spec.Value;
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

            // When re-encrypting, the user's cert/key (if provided) become the edge
            // certificate presented to the client browser.
            String edgeCert = getValue(primary, context, Tls::getCertificate);
            String edgeKey = getValue(primary, context, Tls::getKey);

            // Service CA cert is available: use reencrypt so the router opens a new
            // TLS connection to the backend pod using the cluster CA. If the user
            // also provided a cert/key, use it as the edge certificate for the client.
            return new TLSConfigBuilder()
                    .withTermination("reencrypt")
                    .withDestinationCACertificate(caBundlePem)
                    .withCertificate(edgeCert)
                    .withKey(edgeKey)
                    .withInsecureEdgeTerminationPolicy("Redirect")
                    .build();
        }
    }

    private String getValue(Console primary, Context<Console> context, Function<Tls, Value> source) {
        var specTls = primary.getSpec().getTls();

        if (specTls != null) {
            var value = ConfigSupport.getValue(context, primary.getMetadata().getNamespace(), source.apply(specTls));

            if (value != null) {
                return new String(value, StandardCharsets.UTF_8);
            }
        }

        return null;
    }
}
