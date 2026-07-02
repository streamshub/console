package com.github.streamshub.console.kafka.systemtest.deployment;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.api.kafka.Crds;
import io.strimzi.api.kafka.model.common.Constants;

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

        apply(k8s, Crds.kafka());
        apply(k8s, Crds.kafkaNodePool());
        apply(k8s, Crds.kafkaRebalance());
        apply(k8s, Crds.kafkaTopic());
        apply(k8s, Crds.kafkaConnect());
        apply(k8s, Crds.kafkaConnector());
        apply(k8s, Crds.kafkaMirrorMaker2());
        apply(k8s, Crds.kafkaUser());

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

    @Override
    public void stop() {
        // No-op
    }
}
