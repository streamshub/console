package io.strimzi.kafka.instance;

import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.common.config.SaslConfigs;
import org.bf2.admin.kafka.admin.KafkaAdminConfigRetriever;
import org.bf2.admin.kafka.admin.model.AdminServerException;
import org.bf2.admin.kafka.admin.model.ErrorType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

@RequestScoped
public class ClientFactory {

    private static final String SASL_PLAIN_CONFIG_TEMPLATE = "org.apache.kafka.common.security.plain.PlainLoginModule "
            + "required "
            + "username=\"%s\" "
            + "password=\"%s\";";
    private static final String SASL_OAUTH_CONFIG_TEMPLATE = "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required oauth.access.token=\"%s\";";

    @Inject
    Logger log;

    @Inject
    KafkaAdminConfigRetriever config;

    @Inject
    Instance<JsonWebToken> token;

    @Inject
    Instance<HttpHeaders> headers;

    @Inject
    @ConfigProperty(name = "io.strimzi.kafka.instance-api.oauth", defaultValue = "false")
    boolean oauthEnabled;

    @Inject
    @ConfigProperty(name = "io.strimzi.kafka.instance-api.basic", defaultValue = "true")
    boolean basicEnabled;

    @Produces
    @RequestScoped
    public Supplier<Admin> adminClientSupplier() {
        Map<String, Object> acConfig = config.getAcConfig();

        if (oauthEnabled) {
            if (token.isResolvable()) {
                final String accessToken = token.get().getRawToken();
                if (accessToken == null) {
                    throw new AdminServerException(ErrorType.NOT_AUTHENTICATED);
                }
                acConfig.put(SaslConfigs.SASL_JAAS_CONFIG, String.format(SASL_OAUTH_CONFIG_TEMPLATE, accessToken));
            } else {
                log.warn("OAuth is enabled, but there is no JWT principal");
            }
        } else if (basicEnabled) {
            extractCredentials(Optional.ofNullable(headers.get().getHeaderString(HttpHeaders.AUTHORIZATION)))
                .ifPresentOrElse(
                        credentials -> acConfig.put(SaslConfigs.SASL_JAAS_CONFIG, credentials),
                        () -> {
                            throw new AdminServerException(ErrorType.NOT_AUTHENTICATED);
                        });
        }

        Admin client = Admin.create(acConfig); // NOSONAR - client is closed in #adminClientDisposer
        return () -> client;
    }

    public void adminClientDisposer(@Disposes Supplier<Admin> client) {
        client.get().close();
    }

    Optional<String> extractCredentials(Optional<String> authorizationHeader) {
        return authorizationHeader
                .filter(Objects::nonNull)
                .filter(authn -> authn.startsWith("Basic "))
                .map(authn -> authn.substring("Basic ".length()))
                .map(Base64.getDecoder()::decode)
                .map(String::new)
                .filter(authn -> authn.indexOf(':') >= 0)
                .map(authn -> new String[] {
                    authn.substring(0, authn.indexOf(':')),
                    authn.substring(authn.indexOf(':') + 1)
                })
                .filter(credentials -> !credentials[0].isEmpty() && !credentials[1].isEmpty())
                .map(credentials -> String.format(SASL_PLAIN_CONFIG_TEMPLATE, credentials[0], credentials[1]));
    }

}
