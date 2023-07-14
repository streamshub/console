package org.bf2.admin.kafka.admin.handlers;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.bf2.admin.kafka.admin.KafkaAdminConfigRetriever;
import org.bf2.admin.kafka.admin.model.AdminServerException;
import org.bf2.admin.kafka.admin.model.ErrorType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.jboss.logging.Logger;

import jakarta.enterprise.context.RequestScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;

import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@RequestScoped
public class AdminClientFactory {

    protected static final String ADMIN_CLIENT_CONFIG = RestOperations.class.getName() + ".ADMIN_CLIENT_CONFIG";
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

    /**
     * Route handler common to all Kafka resource routes. Responsible for creating
     * the map of properties used to configure the Kafka Admin Client. When OAuth
     * has been enabled via the environment, the access token will be retrieved from
     * the authenticated user principal present in the context (created by Vert.x
     * handler when a valid JWT was presented by the client). The configuration property
     * map will be placed in the context under the key identified by the
     * {@link #ADMIN_CLIENT_CONFIG} constant.
     */
    public CompletionStage<AdminClient> createAdminClient() {
        Map<String, Object> acConfig = config.getAcConfig();

        if (config.isOauthEnabled()) {
            if (token.isResolvable()) {
                final String accessToken = token.get().getRawToken();
                if (accessToken == null) {
                    throw new AdminServerException(ErrorType.NOT_AUTHENTICATED);
                }
                acConfig.put(SaslConfigs.SASL_JAAS_CONFIG, String.format(SASL_OAUTH_CONFIG_TEMPLATE, accessToken));
            } else {
                log.warn("OAuth is enabled, but there is no JWT principal");
            }
        } else if (config.isBasicEnabled()) {
            extractCredentials(Optional.ofNullable(headers.get().getHeaderString(HttpHeaders.AUTHORIZATION)))
                .ifPresentOrElse(credentials -> acConfig.put(SaslConfigs.SASL_JAAS_CONFIG, credentials),
                    () -> {
                        throw new AdminServerException(ErrorType.NOT_AUTHENTICATED);
                    });
        } else {
            log.debug("OAuth is disabled - no attempt to set access token in Admin Client config");
        }

        return CompletableFuture.supplyAsync(() -> AdminClient.create(acConfig));
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

    public Consumer<byte[], byte[]> createConsumer(Integer limit) {
        Map<String, Object> props = config.getConsumerConfig();

        if (config.isOauthEnabled()) {
            if (token.isResolvable()) {
                final String accessToken = token.get().getRawToken();
                props.put(SaslConfigs.SASL_JAAS_CONFIG, String.format(SASL_OAUTH_CONFIG_TEMPLATE, accessToken));
            } else {
                log.warn("OAuth is enabled, but there is no JWT principal");
            }
        } else if (config.isBasicEnabled()) {
            extractCredentials(Optional.ofNullable(headers.get().getHeaderString(HttpHeaders.AUTHORIZATION)))
                .ifPresentOrElse(credentials -> props.put(SaslConfigs.SASL_JAAS_CONFIG, credentials),
                    () -> {
                        throw new AdminServerException(ErrorType.NOT_AUTHENTICATED);
                    });
        } else {
            log.debug("OAuth is disabled - no attempt to set access token in Admin Client config");
        }

        //props.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 50_000);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        if (limit != null) {
            props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(limit));
        }

        return new KafkaConsumer<>(props);
    }

    public Producer<String, String> createProducer() {
        Map<String, Object> props = config.getProducerConfig();

        if (config.isOauthEnabled()) {
            if (token.isResolvable()) {
                final String accessToken = token.get().getRawToken();
                props.put(SaslConfigs.SASL_JAAS_CONFIG, String.format(SASL_OAUTH_CONFIG_TEMPLATE, accessToken));
            } else {
                log.warn("OAuth is enabled, but there is no JWT principal");
            }
        } else if (config.isBasicEnabled()) {
            extractCredentials(Optional.ofNullable(headers.get().getHeaderString(HttpHeaders.AUTHORIZATION)))
                .ifPresentOrElse(credentials -> props.put(SaslConfigs.SASL_JAAS_CONFIG, credentials),
                    () -> {
                        throw new AdminServerException(ErrorType.NOT_AUTHENTICATED);
                    });
        } else {
            log.debug("OAuth is disabled - no attempt to set access token in Admin Client config");
        }

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 5000);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false);
        props.put(ProducerConfig.RETRIES_CONFIG, 0);

        return new KafkaProducer<>(props);
    }
}
