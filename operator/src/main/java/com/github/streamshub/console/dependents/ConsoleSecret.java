package com.github.streamshub.console.dependents;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.KafkaCluster;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListener;
import io.strimzi.api.kafka.model.kafka.listener.ListenerStatus;

@ApplicationScoped
@KubernetesDependent(labelSelector = ConsoleResource.MANAGEMENT_SELECTOR)
public class ConsoleSecret extends CRUDKubernetesDependentResource<Secret, Console> implements ConsoleResource {

    public static final String NAME = "console-secret";
    private static final Random RANDOM = new Random();

    @Inject
    ObjectMapper objectMapper;

    public ConsoleSecret() {
        super(Secret.class);
    }

    @Override
    public String resourceName() {
        return NAME;
    }

    @Override
    protected Secret desired(Console primary, Context<Console> context) {
        var nextAuth = context.getSecondaryResource(Secret.class).map(s -> s.getData().get("NEXTAUTH_SECRET"));
        Map<String, String> data = new LinkedHashMap<>(2);

        var nextAuthSecret = nextAuth.orElseGet(() -> encodeString(base64String(32)));
        data.put("NEXTAUTH_SECRET", nextAuthSecret);

        var consoleConfig = buildConfig(primary, context);

        try {
            data.put("console-config.yaml", encodeString(objectMapper.writeValueAsString(consoleConfig)));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }

        updateDigest(context, "console-digest", data);

        //console-config.yaml

        return new SecretBuilder()
                .withNewMetadata()
                    .withName(instanceName(primary))
                    .withNamespace(primary.getMetadata().getNamespace())
                    .withLabels(commonLabels("console"))
                .endMetadata()
                .withData(data)
                .build();
    }

    private static String base64String(int length) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        OutputStream out = Base64.getEncoder().wrap(buffer);

        RANDOM.ints().limit(length).forEach(value -> {
            try {
                out.write(value);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        return new String(buffer.toByteArray()).substring(0, length);
    }

    private ConsoleConfig buildConfig(Console primary, Context<Console> context) {
        ConsoleConfig config = new ConsoleConfig();

        for (var kafkaRef : primary.getSpec().getKafkaClusters()) {
            addConfig(context, config, kafkaRef);
        }

        return config;
    }

    private void addConfig(Context<Console> context, ConsoleConfig config, KafkaCluster kafkaRef) {
        KafkaClusterConfig kcConfig = new KafkaClusterConfig();
        kcConfig.setNamespace(kafkaRef.getNamespace());
        kcConfig.setName(kafkaRef.getName());
        kcConfig.setListener(kafkaRef.getListener());
        config.getKafka().getClusters().add(kcConfig);

        // TODO: add informer for Kafka CRs
        String listenerName = kafkaRef.getListener();

        Kafka kafka = context.getClient()
                .resources(Kafka.class)
                .inNamespace(kafkaRef.getNamespace())
                .withName(kafkaRef.getName())
                .get();

        if (kafka != null && listenerName != null) {
            GenericKafkaListener listenerSpec = kafka.getSpec()
                    .getKafka()
                    .getListeners()
                    .stream()
                    .filter(l -> l.getName().equals(listenerName))
                    .findFirst()
                    .orElseThrow();

            StringBuilder protocol = new StringBuilder();
            String mechanism = null;

            if (listenerSpec.getAuth() != null) {
                protocol.append("SASL_");

                var auth = listenerSpec.getAuth();
                switch (auth.getType()) {
                case "oauth":
                    mechanism = "OAUTHBEARER";
                    break;
                case "scram-sha-512":
                    mechanism = "SCRAM-SHA-512";
                    break;
                case "tls", "custom":
                default:
                    // Nothing yet
                    break;
                }
            }

            if (listenerSpec.isTls()) {
                protocol.append("SSL");
            } else {
                protocol.append("PLAINTEXT");
            }

            kcConfig.getProperties().put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, protocol.toString());

            if (mechanism != null) {
                kcConfig.getProperties().put(SaslConfigs.SASL_MECHANISM, mechanism);
            }

            ListenerStatus listenerStatus = kafka.getStatus()
                    .getListeners()
                    .stream()
                    .filter(l -> l.getName().equals(listenerName))
                    .findFirst()
                    .orElse(null);

            Optional.ofNullable(listenerStatus)
                .map(ListenerStatus::getBootstrapServers)
                .or(() -> Optional.ofNullable(listenerSpec.getConfiguration().getBootstrap().getHost()))
                .ifPresent(bootstrapServers -> kcConfig.getProperties().put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers));

            if (kafkaRef.getKafkaUserName() != null) {
                // TODO: add informer for KafkaUser CRs
                Secret userSecret = context.getClient().secrets()
                    .inNamespace(kafkaRef.getNamespace())
                    .withName(kafkaRef.getKafkaUserName())
                    .get();

                kcConfig.getProperties().put(SaslConfigs.SASL_JAAS_CONFIG, decodeString(userSecret.getData().get("sasl.jaas.config")));
            }

        }
    }
}
