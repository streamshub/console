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
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
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
import com.github.streamshub.console.api.v1alpha1.spec.KafkaConnect;
import com.github.streamshub.console.api.v1alpha1.spec.SchemaRegistry;
import com.github.streamshub.console.api.v1alpha1.spec.TrustStore;
import com.github.streamshub.console.api.v1alpha1.spec.Value;
import com.github.streamshub.console.api.v1alpha1.spec.authentication.Authentication;
import com.github.streamshub.console.api.v1alpha1.spec.metrics.MetricsSource;
import com.github.streamshub.console.api.v1alpha1.spec.metrics.MetricsSource.Type;
import com.github.streamshub.console.api.v1alpha1.spec.metrics.MetricsSourceAuthentication;
import com.github.streamshub.console.api.v1alpha1.spec.security.GlobalSecurity;
import com.github.streamshub.console.api.v1alpha1.spec.security.Oidc;
import com.github.streamshub.console.api.v1alpha1.spec.security.Security;
import com.github.streamshub.console.api.v1alpha1.status.Condition.Reasons;
import com.github.streamshub.console.api.v1alpha1.status.Condition.Types;
import com.github.streamshub.console.api.v1alpha1.status.ConditionBuilder;
import com.github.streamshub.console.api.v1alpha1.status.ConsoleStatus;
import com.github.streamshub.console.config.ConsoleConfig;
import com.github.streamshub.console.config.KafkaClusterConfig;
import com.github.streamshub.console.config.KafkaConnectConfig;
import com.github.streamshub.console.config.PrometheusConfig;
import com.github.streamshub.console.config.SchemaRegistryConfig;
import com.github.streamshub.console.config.TrustStoreConfig;
import com.github.streamshub.console.config.TrustStoreConfigBuilder;
import com.github.streamshub.console.config.ValueBuilder;
import com.github.streamshub.console.config.authentication.Authenticated;
import com.github.streamshub.console.config.authentication.AuthenticationConfigBuilder;
import com.github.streamshub.console.config.authentication.OIDC;
import com.github.streamshub.console.config.security.AuditConfigBuilder;
import com.github.streamshub.console.config.security.Decision;
import com.github.streamshub.console.config.security.OidcConfigBuilder;
import com.github.streamshub.console.config.security.Privilege;
import com.github.streamshub.console.config.security.RoleConfigBuilder;
import com.github.streamshub.console.config.security.RuleConfigBuilder;
import com.github.streamshub.console.config.security.SecurityConfig;
import com.github.streamshub.console.config.security.SubjectConfigBuilder;
import com.github.streamshub.console.dependents.support.ConfigSupport;
import com.github.streamshub.console.support.KafkaConfigs;

import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.openshift.api.model.Route;
import io.fabric8.openshift.api.model.RouteIngress;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.api.reconciler.dependent.NameSetter;
import io.javaoperatorsdk.operator.api.reconciler.dependent.ReconcileResult;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;
import io.strimzi.api.kafka.model.kafka.Kafka;
import io.strimzi.api.kafka.model.kafka.KafkaStatus;
import io.strimzi.api.kafka.model.kafka.listener.GenericKafkaListener;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthentication;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationCustom;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationScramSha512;
import io.strimzi.api.kafka.model.kafka.listener.KafkaListenerAuthenticationTls;
import io.strimzi.api.kafka.model.kafka.listener.ListenerStatus;
import io.strimzi.api.kafka.model.user.KafkaUser;
import io.strimzi.api.kafka.model.user.KafkaUserAuthentication;
import io.strimzi.api.kafka.model.user.KafkaUserScramSha512ClientAuthentication;
import io.strimzi.api.kafka.model.user.KafkaUserSpec;
import io.strimzi.api.kafka.model.user.KafkaUserStatus;
import io.strimzi.api.kafka.model.user.KafkaUserTlsClientAuthentication;

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
public class ConfigurationProcessor implements DependentResource<HasMetadata, Console>, NameSetter, ConsoleResource<HasMetadata> {

    public static final String NAME = "ConfigurationProcessor"; // NOSONAR

    private static final Logger LOGGER = Logger.getLogger(ConfigurationProcessor.class);
    private static final String EMBEDDED_METRICS_NAME = "streamshub.console.embedded-prometheus";
    private static final String OIDC_PROVIDER_TRUST_NAME = "oidc-provider";
    private static final Random RANDOM = new SecureRandom();

    @Inject
    Validator validator;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    PrometheusService prometheusService;

    private String instanceName;

    public static class Postcondition implements Condition<HasMetadata, Console> {
        @Override
        public boolean isMet(DependentResource<HasMetadata, Console> dependentResource,
                Console primary,
                Context<Console> context) {

            return context.managedWorkflowAndDependentResourceContext().getMandatory(NAME, Boolean.class);
        }
    }

    @Override
    public String name() {
        return instanceName;
    }

    @Override
    public void setName(String name) {
        this.instanceName = name;
    }

    @Override
    public ReconcileResult<HasMetadata> reconcile(Console primary, Context<Console> context) {
        if (buildSecretData(primary, context)) {
            LOGGER.debugf("Validation gate passed: %s/%s",
                    primary.getMetadata().getNamespace(),
                    primary.getMetadata().getName());
            context.managedWorkflowAndDependentResourceContext().put(NAME, Boolean.TRUE);
        } else {
            LOGGER.debugf("Validation gate failed: %s/%s; %s",
                    primary.getMetadata().getNamespace(),
                    primary.getMetadata().getName(),
                    primary.getStatus().getCondition(Types.ERROR).getMessage());
            context.managedWorkflowAndDependentResourceContext().put(NAME, Boolean.FALSE);
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
            buildClientSecrets(primary, context, data);
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

        context.managedWorkflowAndDependentResourceContext().put("ConsoleSecretData", data);
        return !status.hasActiveCondition(Types.ERROR);
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
            var yaml = objectMapper.copyWith(YAMLFactory.builder().build());
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
    private void buildClientSecrets(Console primary, Context<Console> context, Map<String, String> data) {
        Optional.ofNullable(primary.getSpec().getSecurity())
            .map(GlobalSecurity::getOidc)
            .map(Oidc::getTrustStore)
            .ifPresent(trustStore -> buildTrustStore(
                    context,
                    primary.getMetadata().getNamespace(),
                    OIDC_PROVIDER_TRUST_NAME,
                    trustStore,
                    data
            ));

        coalesce(primary.getSpec().getMetricsSources(), Collections::emptyList).stream()
            .forEach(source -> buildClientSecrets(
                context,
                primary.getMetadata().getNamespace(),
                "metrics-source-" + source.getName(),
                source.getAuthentication(),
                source.getTrustStore(),
                data
            ));

        coalesce(primary.getSpec().getSchemaRegistries(), Collections::emptyList).stream()
            .forEach(source -> buildClientSecrets(
                context,
                primary.getMetadata().getNamespace(),
                "schema-registry-" + source.getName(),
                source.getAuthentication(),
                source.getTrustStore(),
                data
            ));

        coalesce(primary.getSpec().getKafkaConnectClusters(), Collections::emptyList).stream()
            .forEach(connect -> buildClientSecrets(
                context,
                primary.getMetadata().getNamespace(),
                "kafka-connect-" + connect.getName(),
                connect.getAuthentication(),
                connect.getTrustStore(),
                data
            ));
    }

    private void buildClientSecrets(Context<Console> context, String namespace, String name, Authentication authentication, TrustStore trustStore, Map<String, String> data) {
        if (authentication != null) {
            buildAuthSecrets(
                context,
                namespace,
                name,
                authentication,
                data
            );
        }
        if (trustStore != null) {
            buildTrustStore(
                context,
                namespace,
                name,
                trustStore,
                data
            );
        }
    }

    private void buildAuthSecrets(Context<Console> context, String namespace, String name, Authentication authentication, Map<String, String> data) {
        if (authentication.hasBasic()) {
            var basic = authentication.getBasic();
            byte[] password = getValue(context, namespace, basic.getPassword());
            maybePutSecretEntry(data, name + "-basic-password.txt", password);
        } else if (authentication.hasBearer()) {
            var bearer = authentication.getBearer();
            byte[] token = getValue(context, namespace, bearer.getToken());
            maybePutSecretEntry(data, name + "-bearer-token.txt", token);
        } else if (authentication.hasOIDC()) {
            var oidc = authentication.getOidc();
            byte[] clientSecret = getValue(context, namespace, oidc.getClientSecret());
            maybePutSecretEntry(data, name + "-oidc-clientsecret.txt", clientSecret);

            var trustStore = oidc.getTrustStore();

            if (trustStore != null) {
                buildTrustStore(context, namespace, name + "-oidc", trustStore, data);
            }
        }
    }

    private void buildTrustStore(Context<Console> context, String namespace, String name, TrustStore trustStore, Map<String, String> data) {
        byte[] password = getValue(context, namespace, trustStore.getPassword());
        byte[] content = getValue(context, namespace, trustStore.getContent());
        TrustStore.Type type = trustStore.getType();

        if (OIDC_PROVIDER_TRUST_NAME.equals(name) && type != TrustStore.Type.PEM) {
            // OIDC CAs must be PEM encoded for use by the UI container
            content = pemEncode(name, trustStore, password, content);
            type = TrustStore.Type.PEM;
            password = null;
        }

        String key = "truststore-%s-content.%s".formatted(name, type);
        data.put(key, ConfigSupport.encodeBytes(content));
        maybePutSecretEntry(data, "truststore-%s-password.txt".formatted(name), password);
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

    private void maybePutSecretEntry(Map<String, String> data, String key, byte[] value) {
        if (value != null) {
            data.put(key, ConfigSupport.encodeBytes(value));
        }
    }

    private ConsoleConfig buildConfig(Console primary, Context<Console> context) {
        ConsoleConfig config = new ConsoleConfig();

        addSecurity(primary, config, context);
        addMetricsSources(primary, config, context);
        addSchemaRegistries(primary, config);
        addKafkaConnectClusters(primary, config);

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
                    .withScopes(oidc.getScopes())
                    .withTrustStore(buildTrustStoreConfig(oidc.getTrustStore(), OIDC_PROVIDER_TRUST_NAME))
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

            if (OIDC_PROVIDER_TRUST_NAME.equals(name)) {
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
            String qualifiedName = "metrics-source-" + name;
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
                addAuthentication(metricsAuthn, prometheusConfig, qualifiedName);
            }

            prometheusConfig.setTrustStore(buildTrustStoreConfig(
                    metricsSource.getTrustStore(),
                    qualifiedName
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

    private void addAuthentication(MetricsSourceAuthentication metricsAuthn, PrometheusConfig prometheusConfig, String name) {
        if (!addAuthentication((Authentication) metricsAuthn, prometheusConfig, name)) {
            if (metricsAuthn.getToken() != null) {
                prometheusConfig.setAuthentication(new AuthenticationConfigBuilder()
                    .withNewBearer()
                        .withToken(mapValue(new com.github.streamshub.console.api.v1alpha1.spec.ValueBuilder()
                            .withValue(metricsAuthn.getToken())
                            .build(), name))
                    .endBearer()
                    .build());
            } else {
                prometheusConfig.setAuthentication(new AuthenticationConfigBuilder()
                    .withNewBasic()
                        .withUsername(metricsAuthn.getUsername())
                        .withPassword(mapValue(new com.github.streamshub.console.api.v1alpha1.spec.ValueBuilder()
                            .withValue(metricsAuthn.getPassword())
                            .build(), name))
                    .endBasic()
                    .build());
            }
        }
    }

    private void addSchemaRegistries(Console primary, ConsoleConfig config) {
        for (SchemaRegistry registry : coalesce(primary.getSpec().getSchemaRegistries(), Collections::emptyList)) {
            String name = registry.getName();
            String qualifiedName = "schema-registry-" + name;
            var registryConfig = new SchemaRegistryConfig();
            registryConfig.setName(name);
            registryConfig.setUrl(registry.getUrl());
            registryConfig.setTrustStore(buildTrustStoreConfig(
                    registry.getTrustStore(),
                    qualifiedName
            ));

            var authentication = registry.getAuthentication();

            if (authentication != null) {
                addAuthentication(authentication, registryConfig, qualifiedName);
            }

            config.getSchemaRegistries().add(registryConfig);
        }
    }

    private boolean addAuthentication(Authentication authn, Authenticated authenticated, String name) {
        if (authn.hasBasic()) {
            authenticated.setAuthentication(new AuthenticationConfigBuilder()
                .withNewBasic()
                    .withUsername(authn.getBasic().getUsername())
                    .withPassword(mapValue(authn.getBasic().getPassword(), name + "-basic-password"))
                .endBasic()
                .build());
            return true;
        } else if (authn.hasBearer()) {
            authenticated.setAuthentication(new AuthenticationConfigBuilder()
                .withNewBearer()
                    .withToken(mapValue(authn.getBearer().getToken(), name + "-bearer-token"))
                .endBearer()
                .build());
            return true;
        } else if (authn.hasOIDC()) {
            var oidc = authn.getOidc();
            authenticated.setAuthentication(new AuthenticationConfigBuilder()
                .withNewOidc()
                    .withAuthServerUrl(oidc.getAuthServerUrl())
                    .withTokenPath(oidc.getTokenPath())
                    .withClientId(oidc.getClientId())
                    .withClientSecret(mapValue(oidc.getClientSecret(), name + "-oidc-clientsecret"))
                    .withMethod(mapEnumByName(oidc.getMethod(), OIDC.Method::valueOf, null))
                    .withScopes(oidc.getScopes())
                    .withAbsoluteExpiresIn(oidc.isAbsoluteExpiresIn())
                    .withGrantType(mapEnumByName(oidc.getGrantType(), OIDC.GrantType::valueOf, OIDC.GrantType.CLIENT))
                    .withGrantOptions(oidc.getGrantOptions())
                    .withTrustStore(buildTrustStoreConfig(oidc.getTrustStore(), name + "-oidc"))
                .endOidc()
                .build());
            return true;
        }

        return false;
    }

    private com.github.streamshub.console.config.Value mapValue(Value value, String name) {
        if (value != null) {
            return new com.github.streamshub.console.config.ValueBuilder()
                .withValueFrom("/deployments/config/%s.txt".formatted(name))
                .build();
        }
        return null;
    }

    private <E extends Enum<E>> E mapEnumByName(Enum<?> source, Function<String, E> mapper, E defaultValue) {
        if (source != null) {
            return mapper.apply(source.name());
        }
        return defaultValue;
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

        Optional.ofNullable(kafkaRef.getCredentials())
            .map(Credentials::getKafkaUser)
            .ifPresent(user -> {
                String userNs = Optional.ofNullable(user.getNamespace()).orElse(namespace);
                setKafkaUserConfig(
                        context,
                        getResource(context, KafkaUser.class, userNs, user.getName()),
                        kcConfig.getProperties());
            });

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

        String authenticationType = Optional.ofNullable(listenerSpec.getAuth())
                .map(KafkaListenerAuthentication::getType)
                .orElse("");

        switch (authenticationType) {
            case KafkaConfigs.TYPE_OAUTH: // deprecated type
                properties.putIfAbsent(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, saslType(listenerSpec));
                properties.putIfAbsent(SaslConfigs.SASL_MECHANISM, "OAUTHBEARER");
                break;
            case KafkaListenerAuthenticationScramSha512.SCRAM_SHA_512:
                properties.putIfAbsent(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, saslType(listenerSpec));
                properties.putIfAbsent(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512");
                break;
            case KafkaListenerAuthenticationTls.TYPE_TLS:
                properties.putIfAbsent(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
                break;
            case KafkaListenerAuthenticationCustom.TYPE_CUSTOM:
                KafkaListenerAuthenticationCustom custom = (KafkaListenerAuthenticationCustom) listenerSpec.getAuth();
                String saslMechanism = KafkaConfigs.saslMechanism(custom.getListenerConfig());

                if (KafkaConfigs.MECHANISM_SCRAM_SHA512.equals(saslMechanism)) {
                    properties.putIfAbsent(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, saslType(listenerSpec));
                    properties.putIfAbsent(SaslConfigs.SASL_MECHANISM, "SCRAM-SHA-512");
                } else if (KafkaConfigs.MECHANISM_OAUTHBEARER.equals(saslMechanism)) {
                    properties.putIfAbsent(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, saslType(listenerSpec));
                    properties.putIfAbsent(SaslConfigs.SASL_MECHANISM, "OAUTHBEARER");
                }

                break;
            default:
                // Nothing yet
                break;
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

        listenerStatus.map(ListenerStatus::getCertificates)
                .filter(Objects::nonNull)
                .filter(Predicate.not(Collection::isEmpty))
                .map(certificates -> String.join("\n", certificates).trim())
                .ifPresent(certificates -> maybeAddTrustedCertificates(properties, certificates));
    }

    private String saslType(GenericKafkaListener listenerSpec) {
        if (listenerSpec.isTls()) {
            return "SASL_SSL";
        }
        return "SASL_PLAINTEXT";
    }

    void maybeAddTrustedCertificates(Map<String, String> properties, String certificates) {
        if (hasNone(properties, SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG)) {
            properties.put(SslConfigs.SSL_TRUSTSTORE_CERTIFICATES_CONFIG, certificates);
            properties.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "PEM");
        }
    }

    void setKafkaUserConfig(Context<Console> context, KafkaUser user, Map<String, String> properties) {
        var authenticationType = Optional.ofNullable(user.getSpec())
                .map(KafkaUserSpec::getAuthentication)
                .map(KafkaUserAuthentication::getType)
                .orElse("");

        // Changes in the KafkaUser resource and referenced Secret picked up during periodic reconciliation

        switch (authenticationType) {
            case KafkaUserScramSha512ClientAuthentication.TYPE_SCRAM_SHA_512: {
                String secretNs = user.getMetadata().getNamespace();
                String secretName = getSecretName(user);
                var secretData = getResource(context, Secret.class, secretNs, secretName).getData();
                String jaasConfig = getDataEntry(secretData, SaslConfigs.SASL_JAAS_CONFIG, secretNs, secretName);
                properties.putIfAbsent(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig);
                break;
            }

            case KafkaUserTlsClientAuthentication.TYPE_TLS: {
                if (hasNone(properties, SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG,
                        SslConfigs.SSL_KEYSTORE_TYPE_CONFIG,
                        SslConfigs.SSL_KEYSTORE_KEY_CONFIG)) {
                    String secretNs = user.getMetadata().getNamespace();
                    String secretName = getSecretName(user);
                    var secretData = getResource(context, Secret.class, secretNs, secretName).getData();
                    String truststore = getDataEntry(secretData, "ca.crt", secretNs, secretName);
                    String userKey = getDataEntry(secretData, "user.key", secretNs, secretName);
                    String userCertificates = getDataEntry(secretData, "user.crt", secretNs, secretName);

                    maybeAddTrustedCertificates(properties, truststore);

                    properties.put(SslConfigs.SSL_KEYSTORE_KEY_CONFIG, userKey);
                    properties.put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PEM");
                    properties.put(SslConfigs.SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, userCertificates);
                }

                properties.putIfAbsent(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
                break;
            }

            default:
                throw new ReconciliationException("Unsupported authentication type for KafkaUser %s/%s: '%s'"
                        .formatted(user.getMetadata().getNamespace(), user.getMetadata().getName(), authenticationType));
        }
    }

    private String getSecretName(KafkaUser user) {
        return Optional.ofNullable(user.getStatus())
            .map(KafkaUserStatus::getSecret)
            .orElseThrow(() -> new ReconciliationException("KafkaUser %s/%s missing .status.secret"
                    .formatted(user.getMetadata().getNamespace(), user.getMetadata().getName())));
    }

    private String getDataEntry(Map<String, String> secretData, String key, String secretNs, String secretName) {
        String value = secretData.get(key);

        if (value == null) {
            throw new ReconciliationException("Secret %s/%s missing key '%s'"
                    .formatted(secretNs, secretName, key));
        }

        return decodeString(value);
    }

    private static boolean hasNone(Map<String, String> properties, String... keys) {
        return Arrays.stream(keys).noneMatch(properties::containsKey);
    }

    private void addKafkaConnectClusters(Console primary, ConsoleConfig config) {
        for (KafkaConnect connect : coalesce(primary.getSpec().getKafkaConnectClusters(), Collections::emptyList)) {
            String name = connect.getName();
            String qualifiedName = "kafka-connect-" + name;
            var connectConfig = new KafkaConnectConfig();
            connectConfig.setName(name);
            connectConfig.setNamespace(connect.getNamespace());
            connectConfig.setUrl(connect.getUrl());
            connectConfig.setMirrorMaker(Optional.ofNullable(connect.getMirrorMaker()).orElse(Boolean.FALSE));
            connectConfig.setKafkaClusters(coalesce(connect.getKafkaClusters(), Collections::emptyList));
            connectConfig.setTrustStore(buildTrustStoreConfig(connect.getTrustStore(), qualifiedName));

            var authentication = connect.getAuthentication();
            if (authentication != null) {
                addAuthentication(authentication, connectConfig, qualifiedName);
            }

            config.getKafkaConnectClusters().add(connectConfig);
        }
    }
}