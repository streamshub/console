package com.github.streamshub.console.kafka.systemtest.deployment;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersion;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionVersionBuilder;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceSubresourceStatus;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.CustomResource;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.api.kafka.model.common.Constants;
import io.strimzi.api.kafka.model.connect.KafkaConnect;
import io.strimzi.api.kafka.model.connector.KafkaConnector;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.mirrormaker2.KafkaMirrorMaker2;
import io.strimzi.api.kafka.model.nodepool.KafkaNodePool;
import io.strimzi.api.kafka.model.rebalance.KafkaRebalance;
import io.strimzi.api.kafka.model.topic.KafkaTopic;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.crdgenerator.annotations.Crd;

/**
 * This manager creates the Strimzi CRDs needed by the application prior to the test
 * instance of the application being started. It is provided with the Kubernetes API
 * connection properties for the Quarkus devservices instance of the K8s API.
 */
public class StrimziCrdResourceManager implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

    private static final String PREFIX = "quarkus.kubernetes-client.";

    DevServicesContext context;
    Map<String, String> devConfig;

    @Override
    public void setIntegrationTestContext(DevServicesContext context) {
        this.context = context;
        devConfig = context.devServicesProperties();
    }

    <T> Optional<T> get(String key, Function<String, T> mapper) {
        return Optional.ofNullable(devConfig.get(PREFIX + key))
            .map(mapper);
    }

    Optional<String> get(String key) {
        return get(key, Function.identity());
    }

    <T> T get(String key, Function<String, T> mapper, Supplier<T> defaultValue) {
        return get(key, mapper).orElseGet(defaultValue);
    }

    String get(String key, Supplier<String> defaultValue) {
        return get(key, Function.identity())
                .orElseGet(defaultValue);
    }

    Integer durationMs(String key, Supplier<Integer> defaultValue) {
        return get(key, Duration::parse)
                .map(Duration::toMillis)
                .map(Integer.class::cast)
                .orElseGet(defaultValue);
    }

    @Override
    public void stop() {
        // No-op
    }

    @Override
    public Map<String, String> start() {
        Config base = Config.autoConfigure(null);

        var k8s = new KubernetesClientBuilder()
            .editOrNewConfig()
                .withTrustCerts(get("trust-certs", Boolean::parseBoolean, base::isTrustCerts))
                .withWatchReconnectLimit(get("watch-reconnect-limit", Integer::parseInt, base::getWatchReconnectLimit))
                .withWatchReconnectInterval(durationMs("watch-reconnect-interval", base::getWatchReconnectInterval))
                .withConnectionTimeout(durationMs("connection-timeout", base::getConnectionTimeout))
                .withRequestTimeout(durationMs("request-timeout", base::getRequestTimeout))
                .withMasterUrl(get("api-server-url").or(() -> get("master-url")).orElseGet(base::getMasterUrl))
                .withNamespace(get("namespace", base::getNamespace))
                .withUsername(get("username", base::getUsername))
                .withPassword(get("password", base::getPassword))
                .withCaCertFile(get("ca-cert-file", base::getCaCertFile))
                .withCaCertData(get("ca-cert-data", base::getCaCertData))
                .withClientCertFile(get("client-cert-file", base::getClientCertFile))
                .withClientCertData(get("client-cert-data", base::getClientCertData))
                .withClientKeyFile(get("client-key-file", base::getClientKeyFile))
                .withClientKeyData(get("client-key-data", base::getClientKeyData))
                .withClientKeyPassphrase(get("client-key-passphrase", base::getClientKeyPassphrase))
                .withClientKeyAlgo(get("client-key-algo", base::getClientKeyAlgo))
                .withHttpProxy(get("http-proxy", base::getHttpProxy))
                .withHttpsProxy(get("https-proxy", base::getHttpsProxy))
                .withProxyUsername(get("proxy-username", base::getProxyUsername))
                .withProxyPassword(get("proxy-password", base::getProxyPassword))
                .withNoProxy(get("no-proxy", s -> s.split(",")).orElseGet(base::getNoProxy))
            .endConfig()
            .build();

        apply(k8s, crd(Kafka.class));
        apply(k8s, crd(KafkaNodePool.class));
        apply(k8s, crd(KafkaRebalance.class));
        apply(k8s, crd(KafkaTopic.class));
        apply(k8s, crd(KafkaConnect.class));
        apply(k8s, crd(KafkaConnector.class));
        apply(k8s, crd(KafkaMirrorMaker2.class));
        apply(k8s, crd(KafkaUser.class));

        return Collections.emptyMap();
    }

    static void apply(KubernetesClient k8s, CustomResourceDefinition crd) {
        crd.getSpec().getVersions().forEach(v -> {
            // Temporary work-around for https://github.com/strimzi/strimzi-kafka-operator/issues/12896 
            if (Constants.V1.equals(v.getName())) {
                v.setStorage(true);
            }
        });
        k8s.resource(crd).serverSideApply();
    }

    // Replace with load of CRDs from Strimzi API module when added in a future release
    private static CustomResourceDefinition crd(Class<? extends CustomResource<?, ?>> cls) {
        Crd.Spec spec = getCrdMeta(cls).spec();
        CustomResourceSubresourceStatus status = new CustomResourceSubresourceStatus();

        List<CustomResourceDefinitionVersion> crVersions = new ArrayList<>(spec.versions().length);

        for (Crd.Spec.Version apiVersion : spec.versions())  {
            crVersions.add(new CustomResourceDefinitionVersionBuilder()
                    .withName(apiVersion.name())
                    .withNewSubresources()
                        .withStatus(status)
                    .endSubresources()
                    .withNewSchema()
                        .withNewOpenAPIV3Schema()
                            .withType("object")
                            .withXKubernetesPreserveUnknownFields(true)
                        .endOpenAPIV3Schema()
                    .endSchema()
                    .withStorage(apiVersion.storage())
                    .withServed(apiVersion.served())
                    .build());
        }

        String group = spec.group();
        String kind = spec.names().kind();
        String listKind = Optional.of(spec.names().listKind())
                .filter(Predicate.not(String::isBlank))
                .orElse(kind + "List");
        String plural = spec.names().plural();
        String singular = Optional.of(spec.names().singular())
                .filter(Predicate.not(String::isBlank))
                .orElseGet(kind::toLowerCase);

        return new CustomResourceDefinitionBuilder()
                .withNewMetadata()
                    .withName(plural + "." + group)
                .endMetadata()
                .withNewSpec()
                    .withScope(spec.scope())
                    .withGroup(group)
                    .withVersions(crVersions)
                    .withNewNames()
                        .withSingular(singular)
                        .withPlural(plural)
                        .withShortNames(spec.names().shortNames())
                        .withKind(kind)
                        .withListKind(listKind)
                    .endNames()
                .endSpec()
                .build();
    }

    private static Crd getCrdMeta(Class<?> cls) {
        var crd = cls.getAnnotation(Crd.class);

        if (crd == null) {
            throw new IllegalArgumentException(cls.getName() + " is not a known Strimzi CRD type");
        }

        return crd;
    }
}
