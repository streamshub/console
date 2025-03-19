package com.github.streamshub.console.dependents;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Validator;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.streamshub.console.ReconciliationException;
import com.github.streamshub.console.api.v1alpha1.Console;
import com.github.streamshub.console.api.v1alpha1.spec.Credentials;
import com.github.streamshub.console.api.v1alpha1.spec.KafkaCluster;
import com.github.streamshub.console.api.v1alpha1.spec.SchemaRegistry;
import com.github.streamshub.console.api.v1alpha1.spec.TrustStore;
import com.github.streamshub.console.api.v1alpha1.spec.metrics.MetricsSource;
import com.github.streamshub.console.api.v1alpha1.spec.metrics.MetricsSource.Type;
import com.github.streamshub.console.api.v1alpha1.spec.security.GlobalSecurity;
import com.github.streamshub.console.api.v1alpha1.spec.security.Oidc;
import com.github.streamshub.console.api.v1alpha1.spec.security.Security;
import com.github.streamshub.console.api.v1alpha1.status.Condition.Reasons;
import com.github.streamshub.console.api.v1alpha1.status.Condition.Types;
import com.github.streamshub.console.api.v1alpha1.status.ConditionBuilder;
import com.github.streamshub.console.api.v1alpha1.status.ConsoleStatus;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.config.PrometheusConfig;
import com.github.streamshub.console.config.SchemaRegistryConfig;
import com.github.streamshub.console.config.TrustStoreConfig;
import com.github.streamshub.console.config.TrustStoreConfigBuilder;
import com.github.streamshub.console.config.ValueBuilder;
import com.github.streamshub.console.config.security.AuditConfigBuilder;
import com.github.streamshub.console.config.security.Decision;
import com.github.streamshub.console.config.security.OidcConfigBuilder;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.config.security.RoleConfigBuilder;
import com.github.streamshub.console.config.security.RuleConfigBuilder;
import com.github.streamshub.console.config.security.SecurityConfig;
import com.github.streamshub.console.config.security.SubjectConfigBuilder;
import com.github.streamshub.console.dependents.support.ConfigSupport;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteIngress;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.api.reconciler.dependent.ReconcileResult;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaStatus;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListener;
import io.strimzi.api.kafka.model.kafka.listener.ListenerStatus;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserStatus;

import static com.github.streamshub.console.dependents.support.ConfigSupport.getResource;
import static com.github.streamshub.console.dependents.support.ConfigSupport.getValue;
import static com.github.streamshub.console.dependents.support.ConfigSupport.setConfigVars;

/**
 * Virtual resource that is a dependency of all other resources (directly or
 * indirectly). This resource handles the processing of the configuration stored
 * in the Console custom resource, mapping it to the corresponding console
 * application configurations or other Kubernetes resources (e.g. Volumes and
 * VolumeMounts) that will be used later in the reconciliation process.
 */
@ApplicationScoped
public class ConfigurationProcessor implements DependentResource<HasMetadata, Console>, ConsoleResource<HasMetadata> {

    public static final String NAME = "ConfigurationProcessor";

    private static final Logger LOGGER = Logger.getLogger(ConfigurationProcessor.class);
    private static final String EMBEDDED_METRICS_NAME = "streamshub.console.embedded-prometheus";
    private static final Random RANDOM = new SecureRandom();

    @Inject
    Validator validator;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    PrometheusService prometheusService;

    public static class Postcondition implements Condition<HasMetadata, Console> {
        @Override
        public boolean isMet(DependentResource<HasMetadata, Console> dependentResource,
                Console primary,
                Context<Console> context) {

            return context.managedDependentResourceContext().getMandatory(NAME, Boolean.class);
        }
    }

    @Override
    public ReconcileResult<HasMetadata> reconcile(Console primary, Context<Console> context) {
        if (buildSecretData(primary, context)) {
            LOGGER.debugf("Validation gate passed: %s", primary.getMetadata().getName());
            context.managedDependentResourceContext().put(NAME, Boolean.TRUE);
        } else {
            LOGGER.debugf("Validation gate failed: %s; %s",
                    primary.getMetadata().getName(),
                    primary.getStatus().getCondition(Types.ERROR).getMessage());
            context.managedDependentResourceContext().put(NAME, Boolean.FALSE);
        }

        return ReconcileResult.noOperation(primary);
    }

    @Override
    public Class<HasMetadata> resourceType() {
        return HasMetadata.class;
    }

    @Override
    public String resourceName() {
        return NAME;
    }

    private boolean buildSecretData(Console primary, Context<Console> context) {
        ConsoleStatus status = primary.getOrCreateStatus();
        Map<String, String> data = new LinkedHashMap<>();

        var nextAuth = context.getSecondaryResource(Secret.class).map(s -> s.getData().get("NEXTAUTH_SECRET"));
        var nextAuthSecret = nextAuth.orElseGet(() -> encodeString(generateRandomBase64EncodedSecret(32)));
        data.put("NEXTAUTH_SECRET", nextAuthSecret);

        try {
            buildConsoleConfig(primary, context, data);
            buildTrustStores(primary, context, data);
        } catch (Exception e) {
            if (!(e instanceof ReconciliationException)) {
                LOGGER.warnf(e, "Exception processing console configuration from %s/%s", primary.getMetadata().getNamespace(), primary.getMetadata().getName());
            }
            status.updateCondition(new ConditionBuilder()
                    .withType(Types.ERROR)
                    .withStatus("True")
                    .withLastTransitionTime(Instant.now().toString())
                    .withReason(Reasons.RECONCILIATION_EXCEPTION)
                    .withMessage(e.getMessage())
                    .build());
        }

        context.managedDependentResourceContext().put("ConsoleSecretData", data);
        return !status.hasCondition(Types.ERROR);
    }

    private void buildConsoleConfig(Console primary, Context<Console> context,
            Map<String, String> data) {

        var consoleConfig = buildConfig(primary, context);
        var violations = validator.validate(consoleConfig);

        if (!violations.isEmpty()) {
            for (var violation : violations) {
                StringBuilder message = new StringBuilder();
                if (!violation.getPropertyPath().toString().isBlank()) {
                    message.append(violation.getPropertyPath().toString());
                    message.append(' ');
                }
                message.append(violation.getMessage());

                primary.getStatus().updateCondition(new ConditionBuilder()
                        .withType(Types.ERROR)
                        .withStatus("True")
                        .withLastTransitionTime(Instant.now().toString())
                        .withReason(Reasons.INVALID_CONFIGURATION)
                        .withMessage(message.toString())
                        .build());
            }

            return;
        }

        try {
            var yaml = objectMapper.copyWith(new YAMLFactory());
            data.put("console-config.yaml", encodeString(yaml.writeValueAsString(consoleConfig)));
        } catch (JsonProcessingException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Generate additional entries in the secret for metric source trust stores. Also, this
     * method will add to the context the resources to be added to the console deployment to
     * access the secret entries.
     */
    private void buildTrustStores(Console primary, Context<Console> context, Map<String, String> data) {
        Optional.ofNullable(primary.getSpec().getSecurity())
            .map(GlobalSecurity::getOidc)
            .map(Oidc::getTrustStore)
            .ifPresent(trustStore -> buildTrustStore(
                    context,
                    primary.getMetadata().getNamespace(),
                    "oidc-provider",
                    trustStore,
                    data
            ));

        coalesce(primary.getSpec().getMetricsSources(), Collections::emptyList).stream()
            .filter(source -> Objects.nonNull(source.getTrustStore()))
            .map(source -> Map.entry(source.getName(), source.getTrustStore()))
            .forEach(trustStore -> buildTrustStore(
                    context,
                    primary.getMetadata().getNamespace(),
                    "metrics-source-" + trustStore.getKey(),
                    trustStore.getValue(),
                    data
            ));

        coalesce(primary.getSpec().getSchemaRegistries(), Collections::emptyList).stream()
            .filter(source -> Objects.nonNull(source.getTrustStore()))
            .map(source -> Map.entry(source.getName(), source.getTrustStore()))
            .forEach(trustStore -> buildTrustStore(
                    context,
                    primary.getMetadata().getNamespace(),
                    "schema-registry-" + trustStore.getKey(),
                    trustStore.getValue(),
                    data
            ));
    }

    private void buildTrustStore(Context<Console> context, String namespace, String name, TrustStore trustStore, Map<String, String> data) {
        byte[] password = getValue(context, namespace, trustStore.getPassword());
        byte[] content = getValue(context, namespace, trustStore.getContent());
        TrustStore.Type type = trustStore.getType();

        if ("oidc-provider".equals(name) && type != TrustStore.Type.PEM) {
            // OIDC CAs must be PEM encoded for use by the UI container
            content = pemEncode(name, trustStore, password, content);
            type = TrustStore.Type.PEM;
            password = null;
        }

        String key = "truststore-%s-content.%s".formatted(name, type);
        data.put(key, ConfigSupport.encodeBytes(content));

        if (password != null) {
            data.put("truststore-%s-password.txt".formatted(name), ConfigSupport.encodeBytes(content));
        }
    }

    private byte[] pemEncode(String name, TrustStore trustStore, byte[] password, byte[] content) {
        KeyStore keystore;

        try (InputStream in = new ByteArrayInputStream(content)) {
            keystore = KeyStore.getInstance(trustStore.getType().toString());
            char[] secret = password != null
                    ? new String(password, StandardCharsets.UTF_8).toCharArray() : null;

            keystore.load(in, secret);
        } catch (Exception e) {
            throw new ReconciliationException("Truststore %s could not be loaded. %s"
                    .formatted(name, e.getMessage()));
        }

        String alias = trustStore.getAlias();
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try {
            if (alias != null) {
                var certificate = keystore.getCertificate(alias);
                encodeCertificate(buffer, certificate);
            } else {
                for (var aliases = keystore.aliases().asIterator(); aliases.hasNext(); ) {
                    alias = aliases.next();

                    if (keystore.isCertificateEntry(alias)) {
                        var certificate = keystore.getCertificate(alias);
                        encodeCertificate(buffer, certificate);
                    }
                }
            }
        } catch (KeyStoreException | IOException | CertificateEncodingException e) {
            throw new ReconciliationException("Truststore %s could not be loaded. %s"
                    .formatted(name, e.getMessage()));
        }

        return buffer.toByteArray();
    }

    private void encodeCertificate(OutputStream buffer, Certificate certificate) throws IOException, CertificateEncodingException {
        buffer.write("-----BEGIN CERTIFICATE-----\n".getBytes(StandardCharsets.UTF_8));
        buffer.write(Base64.getMimeEncoder(80, new byte[] {'\n'}).encode(certificate.getEncoded()));
        buffer.write("\n-----END CERTIFICATE-----\n".getBytes(StandardCharsets.UTF_8));
    }

    private static String generateRandomBase64EncodedSecret(int length) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        RANDOM.ints().limit(length).forEach(value -> {
            try (OutputStream out = Base64.getEncoder().wrap(buffer)) {
                out.write(value);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });

        return new String(buffer.toByteArray()).substring(0, length);
    }

    private ConsoleConfig buildConfig(Console primary, Context<Console> context) {
        ConsoleConfig config = new ConsoleConfig();

        addSecurity(primary, config, context);
        addMetricsSources(primary, config, context);
        addSchemaRegistries(primary, config);

        for (var kafkaRef : primary.getSpec().getKafkaClusters()) {
            var kafkaConfig = addConfig(primary, context, config, kafkaRef);
            addSecurity(kafkaRef.getSecurity(), kafkaConfig.getSecurity());
        }

        return config;
    }

    private void addSecurity(Console primary, ConsoleConfig config, Context<Console> context) {
        var security = primary.getSpec().getSecurity();

        if (security == null) {
            return;
        }

        var securityConfig = config.getSecurity();
        var oidc = security.getOidc();

        if (oidc != null) {
            String clientSecret = Optional
                    .ofNullable(getValue(context, primary.getMetadata().getNamespace(), oidc.getClientSecret()))
                    .map(String::new)
                    .orElse(null);

            securityConfig.setOidc(new OidcConfigBuilder()
                    .withAuthServerUrl(oidc.getAuthServerUrl())
                    .withIssuer(oidc.getIssuer())
                    .withClientId(oidc.getClientId())
                    .withClientSecret(clientSecret)
                    .withTrustStore(buildTrustStoreConfig(oidc.getTrustStore(), "oidc-provider"))
                    .build());
        }

        addSecurity(security, securityConfig);
    }

    private TrustStoreConfig buildTrustStoreConfig(TrustStore trustStore, String name) {
        TrustStoreConfig trustStoreConfig = null;

        if (trustStore != null) {
            TrustStore.Type type;
            String alias;
            com.github.streamshub.console.config.Value password;

            if ("oidc-provider".equals(name)) {
                // OIDC CAs are always converted to PEM to allow use by the UI container
                type = TrustStore.Type.PEM;
                alias = null;
                password = null;
            } else {
                type = trustStore.getType();
                alias = trustStore.getAlias();
                password = Optional
                    .ofNullable(trustStore.getPassword())
                    .map(p -> new ValueBuilder()
                            .withValueFrom("/deployments/config/truststore-%s-password.txt".formatted(name))
                            .build())
                    .orElse(null);
            }

            trustStoreConfig = new TrustStoreConfigBuilder()
                    .withType(TrustStoreConfig.Type.valueOf(type.name()))
                    .withNewContent()
                        .withValueFrom("/deployments/config/truststore-%s-content.%s"
                                .formatted(name, type.toString()))
                    .endContent()
                    .withPassword(password)
                    .withAlias(alias)
                    .build();
        }

        return trustStoreConfig;
    }

    private void addSecurity(Security source, SecurityConfig target) {
        if (source == null) {
            return;
        }

        var subjects = coalesce(source.getSubjects(), Collections::emptyList);

        for (var subject : subjects) {
            target.getSubjects().add(new SubjectConfigBuilder()
                    .withClaim(subject.getClaim())
                    .withInclude(subject.getInclude())
                    .withRoleNames(subject.getRoleNames())
                    .build());
        }

        var roles = coalesce(source.getRoles(), Collections::emptyList);

        for (var role : roles) {
            var rules = coalesce(role.getRules(), Collections::emptyList)
                    .stream()
                    .map(rule -> new RuleConfigBuilder()
                        .withResources(rule.getResources())
                        .withResourceNames(rule.getResourceNames())
                        .withPrivileges(rule.getPrivileges()
                                .stream()
                                .map(Enum::name)
                                .map(Privilege::valueOf)
                                .toList())
                        .build())
                    .toList();

            target.getRoles().add(new RoleConfigBuilder()
                    .withName(role.getName())
                    .withRules(rules)
                    .build());
        }

        var audits = coalesce(source.getAudit(), Collections::emptyList);

        for (var audit : audits) {
            target.getAudit().add(new AuditConfigBuilder()
                    .withDecision(Decision.valueOf(audit.getDecision().name()))
                    .withResources(audit.getResources())
                    .withResourceNames(audit.getResourceNames())
                    .withPrivileges(audit.getPrivileges()
                            .stream()
                            .map(Enum::name)
                            .map(Privilege::valueOf)
                            .toList())
                    .build());
        }
    }

    private void addMetricsSources(Console primary, ConsoleConfig config, Context<Console> context) {
        var metricsSources = coalesce(primary.getSpec().getMetricsSources(), Collections::emptyList);

        if (metricsSources.isEmpty()) {
            var prometheusConfig = new PrometheusConfig();
            prometheusConfig.setName(EMBEDDED_METRICS_NAME);
            prometheusConfig.setUrl(prometheusService.getUrl(primary, context));
            config.getMetricsSources().add(prometheusConfig);
            return;
        }

        for (MetricsSource metricsSource : metricsSources) {
            String name = metricsSource.getName();
            var prometheusConfig = new PrometheusConfig();
            prometheusConfig.setName(name);

            if (metricsSource.getType() == Type.OPENSHIFT_MONITORING) {
                prometheusConfig.setType(PrometheusConfig.Type.OPENSHIFT_MONITORING);
                prometheusConfig.setUrl(getOpenShiftMonitoringUrl(context));
            } else {
                // embedded Prometheus used like standalone by console
                prometheusConfig.setType(PrometheusConfig.Type.STANDALONE);

                if (metricsSource.getType() == Type.EMBEDDED) {
                    prometheusConfig.setUrl(prometheusService.getUrl(primary, context));
                } else {
                    prometheusConfig.setUrl(metricsSource.getUrl());
                }
            }

            var metricsAuthn = metricsSource.getAuthentication();

            if (metricsAuthn != null) {
                if (metricsAuthn.getToken() == null) {
                    var basicConfig = new PrometheusConfig.Basic();
                    basicConfig.setUsername(metricsAuthn.getUsername());
                    basicConfig.setPassword(metricsAuthn.getPassword());
                    prometheusConfig.setAuthentication(basicConfig);
                } else {
                    var bearerConfig = new PrometheusConfig.Bearer();
                    bearerConfig.setToken(metricsAuthn.getToken());
                    prometheusConfig.setAuthentication(bearerConfig);
                }
            }

            prometheusConfig.setTrustStore(buildTrustStoreConfig(
                    metricsSource.getTrustStore(),
                    "metrics-source-" + name
            ));

            config.getMetricsSources().add(prometheusConfig);
        }
    }

    private String getOpenShiftMonitoringUrl(Context<Console> context) {
        Route thanosQuerier = getResource(context, Route.class, "openshift-monitoring", "thanos-querier");

        String host = thanosQuerier.getStatus()
                .getIngress()
                .stream()
                .map(RouteIngress::getHost)
                .findFirst()
                .orElseThrow(() -> new ReconciliationException(
                        "Ingress host not found on openshift-monitoring/thanos-querier route"));

        return "https://" + host;
    }

    private void addSchemaRegistries(Console primary, ConsoleConfig config) {
        for (SchemaRegistry registry : coalesce(primary.getSpec().getSchemaRegistries(), Collections::emptyList)) {
            String name = registry.getName();
            var registryConfig = new SchemaRegistryConfig();
            registryConfig.setName(name);
            registryConfig.setUrl(registry.getUrl());
            registryConfig.setTrustStore(buildTrustStoreConfig(
                    registry.getTrustStore(),
                    "schema-registry-" + name
            ));

            config.getSchemaRegistries().add(registryConfig);
        }
    }

    private KafkaClusterConfig addConfig(Console primary, Context<Console> context, ConsoleConfig config, KafkaCluster kafkaRef) {
        String namespace = kafkaRef.getNamespace();
        String name = kafkaRef.getName();
        String listenerName = kafkaRef.getListener();

        KafkaClusterConfig kcConfig = new KafkaClusterConfig();
        kcConfig.setId(kafkaRef.getId());
        kcConfig.setNamespace(namespace);
        kcConfig.setName(name);
        kcConfig.setListener(listenerName);
        kcConfig.setSchemaRegistry(kafkaRef.getSchemaRegistry());

        if (kafkaRef.getMetricsSource() == null) {
            if (config.getMetricsSources().stream().anyMatch(src -> src.getName().equals(EMBEDDED_METRICS_NAME))) {
                kcConfig.setMetricsSource(EMBEDDED_METRICS_NAME);
            }
        } else {
            kcConfig.setMetricsSource(kafkaRef.getMetricsSource());
        }

        config.getKubernetes().setEnabled(Objects.nonNull(namespace));
        config.getKafka().getClusters().add(kcConfig);

        setConfigVars(primary, context, kcConfig.getProperties(), kafkaRef.getProperties());
        setConfigVars(primary, context, kcConfig.getAdminProperties(), kafkaRef.getAdminProperties());
        setConfigVars(primary, context, kcConfig.getConsumerProperties(), kafkaRef.getConsumerProperties());
        setConfigVars(primary, context, kcConfig.getProducerProperties(), kafkaRef.getProducerProperties());

        if (namespace != null && listenerName != null) {
            // Changes in the Kafka resource picked up during periodic reconciliation
            Kafka kafka = getResource(context, Kafka.class, namespace, name);
            setListenerConfig(kcConfig.getProperties(), kafka, listenerName);
        }

        if (!kcConfig.getProperties().containsKey(SaslConfigs.SASL_JAAS_CONFIG)) {
            Optional.ofNullable(kafkaRef.getCredentials())
                .map(Credentials::getKafkaUser)
                .ifPresent(user -> {
                    String userNs = Optional.ofNullable(user.getNamespace()).orElse(namespace);
                    setKafkaUserConfig(
                            context,
                            getResource(context, KafkaUser.class, userNs, user.getName()),
                            kcConfig.getProperties());
                });
        }

        return kcConfig;
    }

    void setListenerConfig(Map<String, String> properties, Kafka kafka, String listenerName) {
        GenericKafkaListener listenerSpec = kafka.getSpec()
                .getKafka()
                .getListeners()
                .stream()
                .filter(l -> l.getName().equals(listenerName))
                .findFirst()
                .orElseThrow(() -> new ReconciliationException("Listener '%s' not found on Kafka %s/%s"
                        .formatted(listenerName, kafka.getMetadata().getNamespace(), kafka.getMetadata().getName())));

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

        properties.putIfAbsent(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, protocol.toString());

        if (mechanism != null) {
            properties.putIfAbsent(SaslConfigs.SASL_MECHANISM, mechanism);
        }

        Optional<ListenerStatus> listenerStatus = Optional.ofNullable(kafka.getStatus())
                .map(KafkaStatus::getListeners)
                .orElseGet(Collections::emptyList)
                .stream()
                .filter(l -> l.getName().equals(listenerName))
                .findFirst();

        properties.computeIfAbsent(
                CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG,
                key -> listenerStatus.map(ListenerStatus::getBootstrapServers)
                    .orElseThrow(() -> new ReconciliationException("""
                            Bootstrap servers could not be found for listener '%s' on Kafka %s/%s \
                            and no configuration was given in the Console resource"""
                            .formatted(listenerName, kafka.getMetadata().getNamespace(), kafka.getMetadata().getName()))));

        if (!properties.containsKey(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG)
                && !properties.containsKey(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG)) {
            listenerStatus.map(ListenerStatus::getCertificates)
                    .filter(Objects::nonNull)
                    .filter(Predicate.not(Collection::isEmpty))
                    .map(certificates -> String.join("\n", certificates).trim())
                    .ifPresent(certificates -> {
                        properties.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
                        properties.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, certificates);
                    });
        }
    }

    void setKafkaUserConfig(Context<Console> context, KafkaUser user, Map<String, String> properties) {
        // Changes in the KafkaUser resource and referenced Secret picked up during periodic reconciliation
        var secretName = Optional.ofNullable(user.getStatus())
                .map(KafkaUserStatus::getSecret)
                .orElseThrow(() -> new ReconciliationException("KafkaUser %s/%s missing .status.secret"
                        .formatted(user.getMetadata().getNamespace(), user.getMetadata().getName())));

        String secretNs = user.getMetadata().getNamespace();
        Secret userSecret = getResource(context, Secret.class, secretNs, secretName);
        String jaasConfig = userSecret.getData().get(SaslConfigs.SASL_JAAS_CONFIG);

        if (jaasConfig == null) {
            throw new ReconciliationException("Secret %s/%s missing key '%s'"
                    .formatted(secretNs, secretName, SaslConfigs.SASL_JAAS_CONFIG));
        }

        properties.put(SaslConfigs.SASL_JAAS_CONFIG, decodeString(jaasConfig));
    }

}
