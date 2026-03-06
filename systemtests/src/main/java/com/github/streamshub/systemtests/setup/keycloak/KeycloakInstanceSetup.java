package com.github.streamshub.systemtests.setup.keycloak;

import com.github.streamshub.console.utils.Certificates;
import com.github.streamshub.systemtests.Environment;
import com.github.streamshub.systemtests.constants.Constants;
import com.github.streamshub.systemtests.constants.Labels;
import com.github.streamshub.systemtests.constants.TimeConstants;
import com.github.streamshub.systemtests.exceptions.SetupException;
import com.github.streamshub.systemtests.logs.LogWrapper;
import com.github.streamshub.systemtests.utils.FileUtils;
import com.github.streamshub.systemtests.utils.Utils;
import com.github.streamshub.systemtests.utils.WaitUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ClusterUtils;
import com.github.streamshub.systemtests.utils.resourceutils.ResourceUtils;
import com.github.streamshub.systemtests.utils.resourceutils.keycloak.KeycloakUtils;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.api.model.SecretBuilder;
import io.fabric8.kubernetes.api.model.ServiceBuilder;
import io.fabric8.kubernetes.api.model.ServicePortBuilder;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.networking.v1.Ingress;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicy;
import io.fabric8.kubernetes.api.model.networking.v1.NetworkPolicyBuilder;
import io.skodjob.testframe.TestFrameEnv;
import io.skodjob.testframe.resources.KubeResourceManager;
import io.skodjob.testframe.resources.ResourceItem;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.Map;

import static com.github.streamshub.systemtests.utils.resourceutils.keycloak.KeycloakUtils.createTrustStorePasswordAndConfigmap;
import static com.github.streamshub.systemtests.utils.resourceutils.keycloak.KeycloakUtils.getKeycloakHostname;

/**
 * Manages the lifecycle of a Keycloak <em>instance</em> and its dependencies.
 *
 * <p>This class assumes the Keycloak Operator is already installed (by
 * {@link KeycloakOperatorSetup}) and is responsible for everything beneath it:
 * <ul>
 *   <li>PostgreSQL deployment, service, config, and credentials secret</li>
 *   <li>NetworkPolicies between Keycloak and PostgreSQL</li>
 *   <li>The Keycloak CR itself ({@code keycloak-instance.yaml})</li>
 *   <li>Ingress TLS patching on non-OCP clusters</li>
 *   <li>Truststore preparation for HTTPS communication</li>
 * </ul>
 *
 * <p>Typical usage:
 * <pre>{@code
 * new KeycloakOperatorSetup(namespace, hostname, tlsSecret).setup();
 * KeycloakConfig config = new KeycloakInstanceSetup(namespace).setup();
 * }</pre>
 */
public class KeycloakInstanceSetup {

    private static final Logger LOGGER = LogWrapper.getLogger(KeycloakInstanceSetup.class);

    public static final String KEYCLOAK_RESOURCES_PATH =
        TestFrameEnv.USER_PATH + "/src/main/java/com/github/streamshub/systemtests/setup/keycloak/";

    // File paths
    private static final String POSTGRES_DEPLOYMENT_FILE_PATH = KEYCLOAK_RESOURCES_PATH + "postgres.yaml";
    private static final String KEYCLOAK_INSTANCE_FILE_PATH = KEYCLOAK_RESOURCES_PATH + "keycloak-instance.yaml";

    // Keycloak
    private static final String KEYCLOAK_INGRESS_NAME = "keycloak-ingress";
    private static final String KEYCLOAK_SECRET_NAME = "keycloak-initial-admin";
    private static final String KEYCLOAK_TLS_SECRET_NAME = "example-tls-secret";
    private static final String TRUST_STORE_PASSWORD = "changeit"; // NOSONAR - test password

    // PostgreSQL
    private static final String POSTGRES = "postgres";
    private static final String POSTGRES_SECRET_NAME = "keycloak-db-secret";
    private static final String POSTGRES_CONFIG_NAME = "postgres-config";
    private static final String POSTGRES_USER_NAME = "testuser";
    private static final String POSTGRES_PASSWORD = "testpasswd";

    private final String namespace;
    private String userName;
    private String userPassword;
    private final String keycloakHostname = KeycloakUtils.getKeycloakHostname(false);

    public KeycloakInstanceSetup(String namespace) {
        this.namespace = namespace;
    }

    public void setup(String secretName, String configMapName) {
        LOGGER.info("----------- Install Keycloak Instance -----------");
        createTlsSecret();
        deployPostgres();
        allowNetworkPolicyBetweenKeycloakAndPostgres();
        deployKeycloakInstance();

        KeycloakUtils.allowNetworkPolicyAllIngressForMatchingLabel(
            namespace, Constants.KEYCLOAK + "-allow", Labels.getKeycloakLabelSelector());

        if (!ClusterUtils.isOcp()) {
            patchIngressTls();
        }

        LOGGER.info("Reading Keycloak admin secret '{}'", KEYCLOAK_SECRET_NAME);
        Secret adminSecret = ResourceUtils.getKubeResource(Secret.class, namespace, KEYCLOAK_SECRET_NAME);

        userName = new String(Base64.getDecoder().decode(adminSecret.getData().get("username")), StandardCharsets.UTF_8);
        userPassword = new String(Base64.getDecoder().decode(adminSecret.getData().get("password")), StandardCharsets.UTF_8);

        LOGGER.info("Preparing truststore for HTTPS communication with Keycloak");
        prepareTrustStore();
        createTrustStorePasswordAndConfigmap(namespace, secretName, configMapName, TRUST_STORE_PASSWORD);
    }

    private void createTlsSecret() {
        LOGGER.info("Creating TLS secret '{}' for hostname '{}' in namespace '{}'",
            KEYCLOAK_TLS_SECRET_NAME, keycloakHostname, namespace);
        try {
            // Generate RSA-2048 key pair — equivalent to: -newkey rsa:2048 -nodes
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();

            // Build self-signed X.509 cert valid for 365 days
            // — equivalent to: openssl req -x509 -days 365 -subj "/CN=…/O=…/C=US"
            X509Certificate cert = generateSelfSignedCertificate(keyPair);

            // Encode private key as PKCS#8 PEM (openssl produces PKCS#8 for -newkey rsa)
            String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(keyPair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";

            // Encode certificate as PEM
            String certPem = "-----BEGIN CERTIFICATE-----\n"
                + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(cert.getEncoded())
                + "\n-----END CERTIFICATE-----\n";

            // Create the Kubernetes TLS Secret
            // — equivalent to: kubectl create secret tls ${TLS_SECRET} --cert … --key …
            Secret tlsSecret = new SecretBuilder()
                .withNewMetadata()
                    .withName(KEYCLOAK_TLS_SECRET_NAME)
                    .withNamespace(namespace)
                .endMetadata()
                .withType("kubernetes.io/tls")
                .addToData("tls.crt",
                    Base64.getEncoder().encodeToString(certPem.getBytes(StandardCharsets.UTF_8)))
                .addToData("tls.key",
                    Base64.getEncoder().encodeToString(privateKeyPem.getBytes(StandardCharsets.UTF_8)))
                .build();

            KubeResourceManager.get().createOrUpdateResourceWithoutWait(tlsSecret);
            LOGGER.info("TLS secret '{}' created for hostname '{}'", KEYCLOAK_TLS_SECRET_NAME, KeycloakUtils.getKeycloakHostname(false));

        } catch (Exception e) {
            throw new SetupException("Failed to generate TLS secret for Keycloak: " + e.getMessage());
        }
    }


    private X509Certificate generateSelfSignedCertificate(KeyPair keyPair) throws Exception {
        X500Name subject = new X500Name("CN=" + KeycloakUtils.getKeycloakHostname(false) + ", O=StreamsHubKeycloak, C=US");
        BigInteger serial = new BigInteger(64, new SecureRandom());
        Date notBefore   = Date.from(Instant.now());
        Date notAfter    = Date.from(Instant.now().plus(365, ChronoUnit.DAYS));

        // issuer is the subject for self-signed
        X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
            subject,
            serial,
            notBefore,
            notAfter,
            subject,
            keyPair.getPublic());

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA")
            .build(keyPair.getPrivate());

        return new JcaX509CertificateConverter()
            .getCertificate(certBuilder.build(signer));
    }

    /**
     * Applies the {@code keycloak-instance.yaml} CR, substituting the hostname
     * placeholder, then waits for the StatefulSet and the admin credentials
     * Secret to become ready.
     */
    private void deployKeycloakInstance() {
        LOGGER.info("Deploying Keycloak instance into namespace '{}'", namespace);
        KubeResourceManager.get().kubeCmdClient().inNamespace(namespace)
            .applyContent(FileUtils.readFile(KEYCLOAK_INSTANCE_FILE_PATH)
                .replace("${HOSTNAME}", getKeycloakHostname(true)));

        WaitUtils.waitForStatefulSetReady(namespace, Constants.KEYCLOAK);
        WaitUtils.waitForSecretReady(namespace, KEYCLOAK_SECRET_NAME);

        LOGGER.info("Keycloak instance and admin Secret are ready");
        KubeResourceManager.get().pushToStack(new ResourceItem<>(this::deleteKeycloakInstance));
    }

    private void deleteKeycloakInstance() {
        LOGGER.info("Deleting Keycloak instance in namespace '{}'", namespace);
        KubeResourceManager.get().kubeCmdClient().inNamespace(namespace).delete(KEYCLOAK_INSTANCE_FILE_PATH);
        Secret s = ResourceUtils.getKubeResource(Secret.class, namespace, KEYCLOAK_SECRET_NAME);
        if (s != null) {
            KubeResourceManager.get().deleteResourceWithWait(s);
        }
    }

    /**
     * Patches the Keycloak Ingress with TLS configuration and waits for nginx to
     * reload the certificate.
     *
     * <p>This is only needed on non-OCP clusters (e.g. Minikube) where the ingress
     * controller does not pick up TLS automatically.</p>
     */
    private void patchIngressTls() {
        LOGGER.info("Non-OCP cluster detected — patching Keycloak Ingress with TLS");
        WaitUtils.waitForIngressToBePresent(namespace, KEYCLOAK_INGRESS_NAME);

        Ingress keycloakIngress = ResourceUtils.getKubeResource(Ingress.class, namespace, KEYCLOAK_INGRESS_NAME);
        KubeResourceManager.get().createOrUpdateResourceWithWait(
            keycloakIngress.edit()
                .editSpec()
                    .addNewTl()
                        .withHosts(Constants.KEYCLOAK_HOSTNAME_PREFIX + "." + ClusterUtils.getClusterDomain())
                        .withSecretName(KEYCLOAK_TLS_SECRET_NAME)
                    .endTl()
                .endSpec()
            .build());

        // Wait for nginx to reload the certificate before proceeding
        Utils.sleepWait(TimeConstants.COMPONENT_LOAD_TIMEOUT);

        String nginxController = ResourceUtils.listKubeResourcesByLabelSelector(Pod.class, Constants.NGINX_INGRESS_NAMESPACE, Labels.getNginxPodLabelSelector())
            .getFirst().getMetadata().getName();

        WaitUtils.waitForLogInPod(Constants.NGINX_INGRESS_NAMESPACE, nginxController,
            Constants.KEYCLOAK_HOSTNAME_PREFIX + "." + ClusterUtils.getClusterDomain());
    }

    /**
     * Deploys PostgreSQL into the current namespace.
     *
     * <p>Loads {@code postgres.yaml}, substitutes the container image, and creates
     * the ConfigMap, Deployment, Service, and credentials Secret in the correct
     * order.</p>
     */
    private void deployPostgres() {
        LOGGER.info("Deploying Postgres into namespace '{}'", namespace);

        Deployment postgresDeployment;
        try {
            postgresDeployment = (Deployment) KubeResourceManager.get()
                .kubeClient()
                .readResourcesFromFile(Path.of(POSTGRES_DEPLOYMENT_FILE_PATH))
                .stream()
                .filter(o -> o instanceof Deployment)
                .findFirst()
                .orElseThrow();
        } catch (IOException e) {
            throw new SetupException("Postgres Deployment YAML could not be loaded", e);
        }

        postgresDeployment = postgresDeployment.edit()
            .editMetadata()
                .withNamespace(namespace)
            .endMetadata()
            .editSpec()
                .editTemplate()
                    .editSpec()
                        .editFirstContainer()
                            .withImage(Environment.POSTGRES_IMAGE)
                        .endContainer()
                    .endSpec()
                .endTemplate()
            .endSpec()
            .build();

        var postgresConfig = new ConfigMapBuilder()
            .withNewMetadata()
            .withNamespace(namespace)
            .withName(POSTGRES_CONFIG_NAME)
            .endMetadata()
            .addToData(Map.of(
                "POSTGRES_DB", Constants.KEYCLOAK,
                "POSTGRES_USER", POSTGRES_USER_NAME,
                "POSTGRES_PASSWORD", POSTGRES_PASSWORD,
                "PGDATA", "/var/lib/postgresql/data/pgdata"))
            .build();

        var postgresService = new ServiceBuilder()
            .withNewMetadata()
                .withName(POSTGRES)
                .withNamespace(namespace)
            .endMetadata()
            .withNewSpec()
                .withSelector(Map.of(Labels.APP, POSTGRES))
                .withPorts(new ServicePortBuilder()
                    .withName(POSTGRES)
                    .withPort(5432)
                    .withNewTargetPort(5432)
                    .build())
            .endSpec()
            .build();

        var postgresSecret = new SecretBuilder()
            .withNewMetadata()
                .withName(POSTGRES_SECRET_NAME)
                .withNamespace(namespace)
            .endMetadata()
            .withType("Opaque")
            .addToData("username",
                Base64.getEncoder().encodeToString(POSTGRES_USER_NAME.getBytes(StandardCharsets.UTF_8)))
            .addToData("password",
                Base64.getEncoder().encodeToString(POSTGRES_PASSWORD.getBytes(StandardCharsets.UTF_8)))
            .build();

        // Order matters: ConfigMap → Deployment → Service → Secret
        KubeResourceManager.get().createResourceWithWait(
            postgresConfig, postgresDeployment, postgresService, postgresSecret);
    }

    // -------------------------------------------------------------------------
    // Network policies
    // -------------------------------------------------------------------------

    /**
     * Applies an ingress NetworkPolicy permitting traffic from Keycloak pods to
     * PostgreSQL pods — only when the environment uses default-deny policies.
     */
    private void allowNetworkPolicyBetweenKeycloakAndPostgres() {
        if (Environment.DEFAULT_TO_DENY_NETWORK_POLICIES) {

            LOGGER.info("Applying NetworkPolicy: Keycloak → Postgres");

            NetworkPolicy networkPolicy = new NetworkPolicyBuilder()
                .withNewMetadata()
                    .withName(Constants.KEYCLOAK + "-" + POSTGRES + "-allow")
                    .withNamespace(namespace)
                .endMetadata()
                .withNewSpec()
                    .addNewIngress()
                        .addNewFrom()
                            .withPodSelector(Labels.getKeycloakLabelSelector())
                        .endFrom()
                    .endIngress()
                    .withNewPodSelector()
                        .addToMatchLabels(Labels.APP, POSTGRES)
                    .endPodSelector()
                    .withPolicyTypes(Ingress.class.getSimpleName())
                .endSpec()
                .build();

            KubeResourceManager.get().createResourceWithWait(networkPolicy);
        }
    }

    // -------------------------------------------------------------------------
    // Truststore
    // -------------------------------------------------------------------------

    /**
     * Retrieves the live HTTPS certificate from the running Keycloak instance and
     * imports it into the truststore at
     * {@link Environment#KEYCLOAK_TRUST_STORE_FILE_PATH}.
     *
     * <p>Uses {@link Certificates#loadFromHost(String, int)} to fetch the
     * certificate — the same utility used by {@code TrustAllCertificateManager}
     * in the common module.</p>
     *
     */
    private void prepareTrustStore() {
        LOGGER.info("Retrieving HTTPS certificate from '{}:443' for truststore", keycloakHostname);

        try {
            String pemCertificate = Certificates.loadFromHost(keycloakHostname, 443);

            if (pemCertificate == null || pemCertificate.isBlank()) {
                throw new SetupException("No certificate retrieved from " + keycloakHostname + ":443");
            }

            Path trustStorePath = Path.of(Environment.KEYCLOAK_TRUST_STORE_FILE_PATH);
            KeyStore trustStore = KeyStore.getInstance("JKS");

            if (Files.exists(trustStorePath)) {
                try (var fis = Files.newInputStream(trustStorePath)) {
                    trustStore.load(fis, TRUST_STORE_PASSWORD.toCharArray());
                }
            } else {
                trustStore.load(null, TRUST_STORE_PASSWORD.toCharArray());
            }

            String alias = "keycloak-ca";
            if (trustStore.containsAlias(alias)) {
                LOGGER.info("Removing existing alias '{}' from truststore", alias);
                trustStore.deleteEntry(alias);
            }

            byte[] derBytes = Base64.getDecoder().decode(
                pemCertificate
                    .replaceAll("-----BEGIN CERTIFICATE-----", "")
                    .replaceAll("-----END CERTIFICATE-----", "")
                    .replaceAll("\\s", ""));

            Certificate cert = CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(derBytes));

            trustStore.setCertificateEntry(alias, cert);

            try (var fos = Files.newOutputStream(trustStorePath)) {
                trustStore.store(fos, TRUST_STORE_PASSWORD.toCharArray());
            }

            LOGGER.info("Certificate imported into truststore as alias '{}'", alias);

        } catch (SetupException e) {
            throw e;
        } catch (Exception e) {
            throw new SetupException("Failed to prepare Keycloak truststore: " + e.getMessage());
        }
    }

    public String getUserName() {
        return userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public String getTrustStorePassword() {
        return TRUST_STORE_PASSWORD;
    }
}