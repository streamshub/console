package com.github.streamshub.console.api.security.kafka;

import java.io.StringReader;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.Json;
import jakarta.json.JsonObject;

import com.github.streamshub.console.api.security.SaslJaasConfigCredential;

import io.quarkus.security.identity.AuthenticationRequestContext;
import io.quarkus.security.identity.IdentityProvider;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.TrustedAuthenticationRequest;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class KafkaIdentityProviderTrusted implements IdentityProvider<TrustedAuthenticationRequest> {

    @Override
    public Class<TrustedAuthenticationRequest> getRequestType() {
        return TrustedAuthenticationRequest.class;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(TrustedAuthenticationRequest request,
            AuthenticationRequestContext context) {

        var principal = request.getPrincipal();
        JsonObject principalJson;

        try (var reader = Json.createReader(new StringReader(principal))) {
            principalJson = reader.readObject();

            var identity = QuarkusSecurityIdentity.builder()
                    .setPrincipal(new QuarkusPrincipal(principal))
                    .addAttribute("principal.name", principalJson.getString("name"))
                    .addCredential(SaslJaasConfigCredential.fromValue(principalJson.getString("sasl.jaas.config")))
                    .build();

            return Uni.createFrom().item(identity);
        } catch (Exception e) {
            return Uni.createFrom().failure(e);
        }
    }
}
