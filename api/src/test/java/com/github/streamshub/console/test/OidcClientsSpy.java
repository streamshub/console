package com.github.streamshub.console.test;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkus.oidc.client.OidcClient;
import io.quarkus.oidc.client.OidcClients;
import io.quarkus.oidc.client.runtime.OidcClientConfig;
import io.quarkus.oidc.client.runtime.OidcClientsImpl;
import io.quarkus.test.Mock;
import io.smallrye.mutiny.Uni;

/**
 * Spy/wrapper class for OidcClients that will proxy method calls to the
 * default OidcClients instance provided by Quarkus. This class can be mocked
 * in tests in order to provide a mocked client which may provide test-case
 * values for access tokens, etc. 
 */
@Mock
@ApplicationScoped
public class OidcClientsSpy implements OidcClients {

    @Inject
    Instance<OidcClientsImpl> target;

    @Override
    public void close() throws IOException {
        target.get().close();
    }

    @Override
    public OidcClient getClient() {
        return target.get().getClient();
    }

    @Override
    public OidcClient getClient(String id) {
        return target.get().getClient(id);
    }

    @Override
    public Uni<OidcClient> newClient(OidcClientConfig clientConfig) {
        return Uni.createFrom()
            .completionStage(CompletableFuture.supplyAsync(target::get))
            .onItem()
            .transformToUni(clients -> clients.newClient(clientConfig));
    }
}
