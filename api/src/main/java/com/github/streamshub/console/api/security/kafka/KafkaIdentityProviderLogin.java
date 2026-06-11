package com.github.streamshub.console.api.security.kafka;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;

import org.apache.kafka.clients.admin.Admin;

import com.github.streamshub.console.api.ClientFactory;
import com.github.streamshub.console.api.security.IdentitySupport;
import com.github.streamshub.console.api.security.SaslJaasConfigCredential;
import com.github.streamshub.console.api.support.KafkaContext;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.UsernamePasswordAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.HttpSecurityUtils;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class KafkaIdentityProviderLogin implements IdentityProvider<UsernamePasswordAuthenticationRequest> {

    @Inject
    IdentitySupport identities;

    @Inject
    ClientFactory clientFactory;

    @Inject
    UnaryOperator<Admin> filter;

    @Inject
    Function<Map<String, Object>, Admin> adminBuilder;

    @Override
    public Class<UsernamePasswordAuthenticationRequest> getRequestType() {
        return UsernamePasswordAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(UsernamePasswordAuthenticationRequest request,
            AuthenticationRequestContext context) {

        var routingContext = HttpSecurityUtils.getRoutingContextAttribute(request);
        var kafkaContext = (KafkaContext) routingContext.get("kafka.context");

        return identities.createCredential(kafkaContext, request)
            .onItem()
            .transformToUni(credential -> authenticate(kafkaContext, request.getUsername(), credential));
    }

    private Uni<SecurityIdentity> authenticate(
            KafkaContext kafkaContext,
            String principalName,
            SaslJaasConfigCredential credential) {

        var promise = new CompletableFuture<SecurityIdentity>();

        try (var admin = clientFactory.createAdmin(filter, adminBuilder, kafkaContext, credential)) {
            admin.describeCluster()
                    .clusterId()
                    .toCompletionStage()
                    .thenApply(clusterId -> {
                        var principalJson = Json.createObjectBuilder()
                                .add("name", principalName)
                                .add("sasl.jaas.config", credential.value())
                                .build();

                        return QuarkusSecurityIdentity.builder()
                                .setPrincipal(new QuarkusPrincipal(principalJson.toString()))
                                .addAttribute("principal.name", principalName)
                                .addCredential(credential)
                                .build();
                    })
                    .thenApply(promise::complete)
                    .exceptionally(promise::completeExceptionally);
        }

        return Uni.createFrom().completionStage(promise);
    }
}
