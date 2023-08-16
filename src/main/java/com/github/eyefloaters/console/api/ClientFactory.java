package com.github.eyefloaters.console.api;

import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriInfo;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.security.auth.SecurityProtocol;
import org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule;
import org.apache.kafka.common.security.plain.PlainLoginModule;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.service.KafkaClusterService;
import com.github.eyefloaters.console.legacy.model.AdminServerException;
import com.github.eyefloaters.console.legacy.model.ErrorType;

import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.strimzi.api.kafka.model.Kafka;
import io.strimzi.api.kafka.model.status.ListenerStatus;

@RequestScoped
public class ClientFactory {

    private static final String SASL_PLAIN_CONFIG_TEMPLATE = PlainLoginModule.class.getName()
            + " required"
            + " username=\"%s\""
            + " password=\"%s\";";

    private static final String SASL_OAUTH_CONFIG_TEMPLATE = OAuthBearerLoginModule.class.getName()
            + " required"
            + " oauth.access.token=\"%s\";";

    @Inject
    Logger log;

    @Inject
    SharedIndexInformer<Kafka> kafkaInformer;

    @Inject
    Instance<JsonWebToken> token;

    @Inject
    Instance<HttpHeaders> headers;

    @Inject
    UriInfo requestUri;

    @Produces
    @RequestScoped
    public Supplier<Admin> adminClientSupplier() {
        String clusterId = requestUri.getPathParameters().getFirst("clusterId");

        if (clusterId == null) {
            return () -> null;
        }

        Supplier<NotFoundException> noSuchKafka =
            () -> new NotFoundException("No such Kafka cluster: " + clusterId);

        Kafka cluster = KafkaClusterService.findCluster(kafkaInformer, clusterId)
            .orElseThrow(noSuchKafka);

        Map<String, Object> config = KafkaClusterService.externalListeners(cluster)
            .findFirst()
            .map(l -> buildConfiguration(cluster, l))
            .orElseThrow(noSuchKafka);

        log.debug("AdminClient configuration:");
        config.entrySet().forEach(entry -> log.debugf("\t%s = %s", entry.getKey(), entry.getValue()));

        Admin client = Admin.create(config); // NOSONAR - client is closed in #adminClientDisposer
        return () -> client;
    }

    public void adminClientDisposer(@Disposes Supplier<Admin> client) {
        client.get().close();
    }

    Map<String, Object> buildConfiguration(Kafka cluster, ListenerStatus listenerStatus) {
        Map<String, Object> config = new HashMap<>();
        String authType = KafkaClusterService.getAuthType(cluster, listenerStatus).orElse("");
        boolean saslEnabled;

        switch (authType) {
            case "oauth":
                log.info("OAuth enabled");
                saslEnabled = true;
                config.put(SaslConfigs.SASL_MECHANISM, "OAUTHBEARER");
                config.put(SaslConfigs.SASL_LOGIN_CALLBACK_HANDLER_CLASS, "io.strimzi.kafka.oauth.client.JaasClientOauthLoginCallbackHandler");
                // Do not attempt token refresh ahead of expiration (ExpiringCredentialRefreshingLogin)
                // May still cause warnings to be logged when token will expired in less than SASL_LOGIN_REFRESH_MIN_PERIOD_SECONDS.
                config.put(SaslConfigs.SASL_LOGIN_REFRESH_BUFFER_SECONDS, "0");

                if (token.isResolvable()) {
                    final String accessToken = token.get().getRawToken();
                    if (accessToken == null) {
                        throw new AdminServerException(ErrorType.NOT_AUTHENTICATED);
                    }
                    config.put(SaslConfigs.SASL_JAAS_CONFIG, String.format(SASL_OAUTH_CONFIG_TEMPLATE, accessToken));
                } else {
                    log.warn("OAuth is enabled, but there is no JWT principal");
                }

                break;
            case "plain":
                log.info("SASL/PLAIN from HTTP Basic authentication enabled");
                saslEnabled = true;
                config.put(SaslConfigs.SASL_MECHANISM, "PLAIN");

                extractCredentials(Optional.ofNullable(headers.get().getHeaderString(HttpHeaders.AUTHORIZATION)))
                    .ifPresentOrElse(
                            credentials -> config.put(SaslConfigs.SASL_JAAS_CONFIG, credentials),
                            () -> {
                                throw new AdminServerException(ErrorType.NOT_AUTHENTICATED);
                            });

                break;
            default:
                log.info("Broker authentication/SASL disabled");
                saslEnabled = false;
                break;
        }

        StringBuilder protocol = new StringBuilder();

        if (saslEnabled) {
            protocol.append("SASL_");
        }

        List<String> certificates = Optional.ofNullable(listenerStatus.getCertificates()).orElseGet(Collections::emptyList);

        if (!certificates.isEmpty()) {
            protocol.append(SecurityProtocol.SSL.name);
            config.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
            config.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, String.join("\n", certificates).trim());
        } else {
            protocol.append(SecurityProtocol.PLAINTEXT.name);
        }

        config.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, listenerStatus.getBootstrapServers());
        config.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, protocol.toString());
        config.put(AdminClientConfig.METADATA_MAX_AGE_CONFIG, "30000");
        config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
        config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, "10000");

        return config;
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
