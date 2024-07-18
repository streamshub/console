package com.github.streamshub.console.kafka.systemtest.deployment;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.strimzi.api.kafka.Crds;

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

    Optional<String> get(String key) {
        return get(key, Function.identity());
    }

    <T> Optional<T> get(String key, Function<String, T> mapper) {
        return Optional.ofNullable(devConfig.get(PREFIX + key))
            .map(mapper);
    }

    @Override
    public Map<String, String> start() {
        Config base = Config.autoConfigure(null);

        var k8s = new KubernetesClientBuilder()
            .editOrNewConfig()
                .withTrustCerts(get("trust-certs", Boolean::parseBoolean).orElseGet(base::isTrustCerts))
                .withWatchReconnectLimit(get("watch-reconnect-limit", Integer::parseInt).orElseGet(base::getWatchReconnectLimit))
                .withWatchReconnectInterval((int) get("watch-reconnect-interval", Duration::parse)
                        .orElse(Duration.ofMillis(base.getWatchReconnectInterval())).toMillis())
                .withConnectionTimeout((int) get("connection-timeout", Duration::parse)
                        .orElse(Duration.ofMillis(base.getConnectionTimeout())).toMillis())
                .withRequestTimeout((int) get("request-timeout", Duration::parse)
                        .orElse(Duration.ofMillis(base.getRequestTimeout())).toMillis())
                .withMasterUrl(get("api-server-url").or(() -> get("master-url")).orElse(base.getMasterUrl()))
                .withNamespace(get("namespace").orElseGet(base::getNamespace))
                .withUsername(get("username").orElse(base.getUsername()))
                .withPassword(get("password").orElse(base.getPassword()))
                .withCaCertFile(get("ca-cert-file").orElse(base.getCaCertFile()))
                .withCaCertData(get("ca-cert-data").orElse(base.getCaCertData()))
                .withClientCertFile(get("client-cert-file").orElse(base.getClientCertFile()))
                .withClientCertData(get("client-cert-data").orElse(base.getClientCertData()))
                .withClientKeyFile(get("client-key-file").orElse(base.getClientKeyFile()))
                .withClientKeyData(get("client-key-data").orElse(base.getClientKeyData()))
                .withClientKeyPassphrase(get("client-key-passphrase").orElse(base.getClientKeyPassphrase()))
                .withClientKeyAlgo(get("client-key-algo").orElse(base.getClientKeyAlgo()))
                .withHttpProxy(get("http-proxy").orElse(base.getHttpProxy()))
                .withHttpsProxy(get("https-proxy").orElse(base.getHttpsProxy()))
                .withProxyUsername(get("proxy-username").orElse(base.getProxyUsername()))
                .withProxyPassword(get("proxy-password").orElse(base.getProxyPassword()))
                .withNoProxy(get("no-proxy", s -> s.split(",")).orElse(base.getNoProxy()))
            .endConfig()
            .build();

        k8s.resource(Crds.kafka()).serverSideApply();
        k8s.resource(Crds.kafkaTopic()).serverSideApply();

        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        // No-op
    }
}
