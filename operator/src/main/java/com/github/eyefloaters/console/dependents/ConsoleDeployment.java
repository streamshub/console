package com.github.eyefloaters.console.dependents;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.eyefloaters.console.api.v1alpha1.Console;

import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ResourceDiscriminator;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.CRUDKubernetesDependentResource;
import io.javaoperatorsdk.operator.processing.dependent.kubernetes.KubernetesDependent;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListener;
import io.strimzi.api.kafka.model.kafka.listener.ListenerStatus;

@KubernetesDependent(
        labelSelector = ConsoleResource.MANAGEMENT_SELECTOR,
        resourceDiscriminator = ConsoleDeployment.Discriminator.class)
public class ConsoleDeployment extends CRUDKubernetesDependentResource<Deployment, Console> implements ConsoleResource {

    public static class Discriminator implements ResourceDiscriminator<Deployment, Console> {
        @Override
        public Optional<Deployment> distinguish(Class<Deployment> resourceType, Console primary, Context<Console> context) {
            return context.getSecondaryResourcesAsStream(resourceType)
                .filter(d -> "console".equals(d.getMetadata().getLabels().get(NAME_LABEL)))
                .findFirst();
        }
    }

    public ConsoleDeployment() {
        super(Deployment.class);
    }

    @Override
    protected Deployment desired(Console primary, Context<Console> context) {
        Deployment desired = load(context, "console.deployment.yaml", Deployment.class);
        String name = name(primary);

        AtomicInteger k = new AtomicInteger(0);
        List<EnvVar> vars = new ArrayList<>();

        for (var kafkaRef : primary.getSpec().getKafkaClusters()) {
            Kafka kafka = context.getClient()
                    .resources(Kafka.class)
                    .inNamespace(kafkaRef.getNamespace())
                    .withName(kafkaRef.getName())
                    .get();

            String listenerName = kafkaRef.getListener();
            GenericKafkaListener listenerSpec = kafka.getSpec()
                    .getKafka()
                    .getListeners()
                    .stream()
                    .filter(l -> l.getName().equals(listenerName))
                    .findFirst()
                    .orElseThrow();
            ListenerStatus listenerStatus = kafka.getStatus()
                    .getListeners()
                    .stream()
                    .filter(l -> l.getName().equals(listenerName))
                    .findFirst()
                    .orElse(null);

            if (listenerStatus != null) {
                String varBase = "CONSOLE_KAFKA_K%03d".formatted(k.getAndIncrement());

                vars.add(new EnvVarBuilder()
                        .withName(varBase)
                        .withValue("%s/%s".formatted(kafkaRef.getNamespace(), kafkaRef.getName()))
                        .build());
                vars.add(new EnvVarBuilder()
                        .withName(varBase + "_BOOTSTRAP_SERVERS")
                        .withValue(listenerStatus.getBootstrapServers())
                        .build());

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
                    case "tls":
                    case "custom":
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

                vars.add(new EnvVarBuilder()
                        .withName(varBase + "_SECURITY_PROTOCOL")
                        .withValue(protocol.toString())
                        .build());

                if (mechanism != null) {
                    vars.add(new EnvVarBuilder()
                            .withName(varBase + "_SASL_MECHANISM")
                            .withValue(mechanism)
                            .build());
                }

                if (kafkaRef.getKafkaUserName() != null) {
                    vars.add(new EnvVarBuilder()
                            .withName(varBase + "_SASL_JAAS_CONFIG")
                            .withNewValueFrom()
                                .withNewSecretKeyRef()
                                    .withName(kafkaRef.getKafkaUserName())
                                    .withKey("sasl.jaas.config")
                                .endSecretKeyRef()
                            .endValueFrom()
                            .build());
                }
            }
        }

        return desired.edit()
            .editMetadata()
                .withName(name)
                .withNamespace(primary.getMetadata().getNamespace())
                .withLabels(MANAGEMENT_LABEL)
                .addToLabels(NAME_LABEL, "console")
            .endMetadata()
            .editSpec()
                .editSelector()
                    .withMatchLabels(Map.of("app", name))
                .endSelector()
                .editTemplate()
                    .editMetadata()
                        .addToLabels(Map.of("app", name))
                    .endMetadata()
                    .editSpec()
                        .withServiceAccountName(ConsoleServiceAccount.name(primary))
                        .editMatchingContainer(c -> "console-api".equals(c.getName()))
                            .addToEnv(vars.toArray(EnvVar[]::new))
                        .endContainer()
                        .editMatchingContainer(c -> "console-ui".equals(c.getName()))
                            .editMatchingEnv(env -> "CONSOLE_METRICS_PROMETHEUS_URL".equals(env.getName()))
                                .withValue("http://" + PrometheusService.host(primary) + ":9090")
                            .endEnv()
                            .editMatchingEnv(env -> "NEXTAUTH_URL".equals(env.getName()))
                                .withValue("https://" + ConsoleIngress.host(primary))
                            .endEnv()
                            .editMatchingEnv(env -> "NEXTAUTH_SECRET".equals(env.getName()))
                                .editValueFrom()
                                    .editSecretKeyRef()
                                        .withName(ConsoleSecret.name(primary))
                                    .endSecretKeyRef()
                                .endValueFrom()
                            .endEnv()
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();
    }

    public static String name(Console primary) {
        return primary.getMetadata().getName() + "-console";
    }
}