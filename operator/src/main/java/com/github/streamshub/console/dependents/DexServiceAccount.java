package com.github.streamshub.console.dependents;

import jakarta.enterprise.context.ApplicationScoped;

import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.dependents.support.ConfigSupport;

import io.fabric8.kubernetes.api.model.ServiceAccount;
import io.javaoperatorsdk.operator.api.config.informer.Informer;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;

@ApplicationScoped
@KubernetesDependent(informer = @Informer(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR))
public class DexServiceAccount extends BaseServiceAccount {

    public static final String NAME = "dex-serviceaccount";
    private static final String REDIRECT_ANNOTATION = "serviceaccounts.openshift.io/oauth-redirecturi.dex";

    public DexServiceAccount() {
        super("dex", "dex.serviceaccount.yaml", NAME);
    }

    @Override
    protected ServiceAccount desired(Console primary, Context<Console> context) {
        ServiceAccount sa = super.desired(primary, context);

        if (ConfigSupport.dexOpenshiftIntegrationEnabled(primary)) {
            // Annotate the ServiceAccount so OpenShift registers it as an OAuth client.
            // The redirect URI points at Dex's callback endpoint on the console hostname.
            String host = getAttribute(context, "host", String.class);
            String redirectUri = "https://" + host + "/dex/callback";
            sa.getMetadata().getAnnotations().put(REDIRECT_ANNOTATION, redirectUri);
        }

        return sa;
    }
}
