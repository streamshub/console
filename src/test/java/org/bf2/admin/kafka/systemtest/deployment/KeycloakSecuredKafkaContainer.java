package org.bf2.admin.kafka.systemtest.deployment;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ContainerNetwork;
import io.strimzi.test.container.StrimziZookeeperContainer;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.jboss.logging.Logger;
import org.testcontainers.images.builder.Transferable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.UUID;

class KeycloakSecuredKafkaContainer extends KafkaContainer {

    protected static final Logger LOGGER = Logger.getLogger(KeycloakSecuredKafkaContainer.class);

    private int kafkaExposedPort;
    String caCertificate;
    byte[] keystore;
    byte[] truststore;

    KeycloakSecuredKafkaContainer(String imageName) {
        super(imageName);
        withExposedPorts(KAFKA_PORT);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public void addFixedExposedPort(int hostPort, int containerPort) {
        super.addFixedExposedPort(hostPort, containerPort);
    }

    @Override
    protected void doStart() {
        final String passString = new String(PASSPHRASE);
        getEnvMap().entrySet().forEach(env -> {
            if ("${GENERATED_PASSPHRASE}".equals(env.getValue())) {
                env.setValue(passString);
            }
        });
        super.doStart();
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo, boolean reused) {
        kafkaExposedPort = getMappedPort(KAFKA_PORT);

        LOGGER.infof("This is mapped port %s", kafkaExposedPort);

        StringBuilder advertisedListeners = new StringBuilder(getBootstrapServers());

        Collection<ContainerNetwork> cns = containerInfo.getNetworkSettings().getNetworks().values();

        for (ContainerNetwork cn : cns) {
            advertisedListeners.append("," + "REPLICATION://").append(cn.getIpAddress()).append(":9091");
        }

        LOGGER.infof("This is all advertised listeners for Kafka %s", advertisedListeners);

        String command = "#!/bin/bash \n";
        command += "bin/zookeeper-server-start.sh ./config/zookeeper.properties &\n";
        command += "/bin/bash /opt/kafka/start.sh"
                + " --override listeners=REPLICATION://0.0.0.0:9091,SECURE://0.0.0.0:" + KAFKA_PORT
                + " --override advertised.listeners=" + advertisedListeners.toString()
                + " --override zookeeper.connect=localhost:" + StrimziZookeeperContainer.ZOOKEEPER_PORT
                + " --override listener.security.protocol.map=REPLICATION:SSL,SECURE:SASL_SSL"
                + " --override inter.broker.listener.name=REPLICATION\n";

        LOGGER.info("Copying command to 'STARTER_SCRIPT' script.");

        try {
            generateCertificates();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        copyFileToContainer(Transferable.of(keystore, 700), "/opt/kafka/certs/cluster.keystore.p12");
        copyFileToContainer(Transferable.of(truststore, 700), "/opt/kafka/certs/cluster.truststore.jks");
        copyFileToContainer(Transferable.of(command.getBytes(StandardCharsets.UTF_8), 700), STARTER_SCRIPT);
    }

    @Override
    public String getBootstrapServers() {
        return String.format("SECURE://%s:%s", getHost(), kafkaExposedPort);
    }

    @Override
    String getCACertificate() {
        return this.caCertificate;
    }

    private static final String BC_PROVIDER = "BC";
    private static final String KEY_ALGORITHM = "RSA";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String PKCS12 = "PKCS12";
    private static final char[] PASSPHRASE = UUID.randomUUID().toString().toCharArray();

    void generateCertificates() throws Exception {
        // Add the BouncyCastle Provider
        Security.addProvider(new BouncyCastleProvider());

        // Initialize a new KeyPair generator
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM, BC_PROVIDER);
        keyPairGenerator.initialize(2048);

        LocalDateTime now = LocalDateTime.now();
        Date notBefore = Date.from(now.minusDays(1).toInstant(ZoneOffset.UTC));
        Date notAfter = Date.from(now.plusYears(1).toInstant(ZoneOffset.UTC));

        KeyPair rootKeyPair = keyPairGenerator.generateKeyPair();
        X509Certificate rootCert = buildCACertificate(rootKeyPair, notBefore, notAfter);

        KeyPair issuedCertKeyPair = keyPairGenerator.generateKeyPair();
        Certificate issuedCert = buildServerCertificate(issuedCertKeyPair, rootKeyPair, rootCert, notBefore, notAfter);

        KeyStore sslTrustStore = KeyStore.getInstance("JKS");
        sslTrustStore.load(null, PASSPHRASE);
        sslTrustStore.setCertificateEntry("CACert", rootCert);
        this.truststore = toByteArray(sslTrustStore, PASSPHRASE);

        KeyStore sslKeyStore = KeyStore.getInstance(PKCS12, BC_PROVIDER);
        sslKeyStore.load(null, PASSPHRASE);
        sslKeyStore.setKeyEntry("localhost", issuedCertKeyPair.getPrivate(), null, new Certificate[] {
            issuedCert,
            rootCert
        });
        sslKeyStore.setCertificateEntry("CACert", rootCert);
        this.keystore = toByteArray(sslKeyStore, PASSPHRASE);

        this.caCertificate = pemEncodeCertificate(rootCert);
    }

    private X509Certificate buildCACertificate(KeyPair keyPair, Date notBefore, Date notAfter)
            throws OperatorCreationException, IOException, GeneralSecurityException {

        BigInteger rootSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()));

        // Issued By and Issued To same for root certificate
        X500Name rootCertIssuer = new X500Name("CN=root-cert");
        X500Name rootCertSubject = rootCertIssuer;
        ContentSigner rootCertContentSigner = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(BC_PROVIDER)
                .build(keyPair.getPrivate());
        X509v3CertificateBuilder rootCertBuilder = new JcaX509v3CertificateBuilder(rootCertIssuer, rootSerialNum,
                notBefore, notAfter, rootCertSubject, keyPair.getPublic());

        // Add Extensions
        // A BasicConstraint to mark root certificate as CA certificate
        JcaX509ExtensionUtils rootCertExtUtils = new JcaX509ExtensionUtils();
        rootCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        rootCertBuilder.addExtension(Extension.subjectKeyIdentifier, false,
                rootCertExtUtils.createSubjectKeyIdentifier(keyPair.getPublic()));

        // Create a cert holder and export to X509Certificate
        X509CertificateHolder rootCertHolder = rootCertBuilder.build(rootCertContentSigner);
        return new JcaX509CertificateConverter().setProvider(BC_PROVIDER)
                .getCertificate(rootCertHolder);
    }

    private Certificate buildServerCertificate(KeyPair keyPair, KeyPair signerKeyPair, X509Certificate signerCert, Date notBefore, Date notAfter)
            throws GeneralSecurityException, IOException, OperatorCreationException {

        // Generate a new KeyPair and sign it using the Root Cert Private Key
        // by generating a CSR (Certificate Signing Request)
        X500Name issuedCertSubject = new X500Name("CN=kafka-broker,O=dev.bf2");
        BigInteger issuedCertSerialNum = new BigInteger(Long.toString(new SecureRandom().nextLong()));

        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(issuedCertSubject,
                keyPair.getPublic());
        JcaContentSignerBuilder csrBuilder = new JcaContentSignerBuilder(SIGNATURE_ALGORITHM).setProvider(BC_PROVIDER);

        // Sign the new KeyPair with the root cert Private Key
        ContentSigner csrContentSigner = csrBuilder.build(signerKeyPair.getPrivate());
        PKCS10CertificationRequest csr = p10Builder.build(csrContentSigner);

        // Use the Signed KeyPair and CSR to generate an issued Certificate
        // Here serial number is randomly generated. In general, CAs use
        // a sequence to generate Serial number and avoid collisions
        X509v3CertificateBuilder issuedCertBuilder = new X509v3CertificateBuilder(new X500Name(signerCert.getSubjectX500Principal().getName()), issuedCertSerialNum,
                notBefore, notAfter, csr.getSubject(), csr.getSubjectPublicKeyInfo());

        JcaX509ExtensionUtils issuedCertExtUtils = new JcaX509ExtensionUtils();

        // Add Extensions
        // Use BasicConstraints to say that this Cert is not a CA
        issuedCertBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));

        // Add Issuer cert identifier as Extension
        issuedCertBuilder.addExtension(Extension.authorityKeyIdentifier, false, issuedCertExtUtils.createAuthorityKeyIdentifier(signerCert));

        // Add intended key usage extension if needed
        issuedCertBuilder.addExtension(Extension.keyUsage, false, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));

        // Add DNS name is cert is to used for SSL
        GeneralNames subjectAltName = new GeneralNames(new GeneralName[] {
            new GeneralName(GeneralName.dNSName, "kafka"),
            new GeneralName(GeneralName.dNSName, "localhost")
        });
        issuedCertBuilder.addExtension(Extension.subjectAlternativeName, false, subjectAltName);

        X509CertificateHolder issuedCertHolder = issuedCertBuilder.build(csrContentSigner);
        X509Certificate issuedCert = new JcaX509CertificateConverter().setProvider(BC_PROVIDER)
                .getCertificate(issuedCertHolder);

        // Verify the issued cert signature against the root (issuer) cert
        issuedCert.verify(signerCert.getPublicKey(), BC_PROVIDER);
        return issuedCert;
    }

    private byte[] toByteArray(KeyStore store, char[] passphrase) throws GeneralSecurityException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        store.store(buffer, passphrase);
        return buffer.toByteArray();
    }

    private String pemEncodeCertificate(Certificate certificate) throws IOException, CertificateEncodingException {
        ByteArrayOutputStream certificateOut = new ByteArrayOutputStream();
        certificateOut.write("-----BEGIN CERTIFICATE-----\n".getBytes(StandardCharsets.UTF_8));
        certificateOut.write(Base64.getMimeEncoder(80, new byte[] {'\n'}).encode(certificate.getEncoded()));
        certificateOut.write("-----END CERTIFICATE-----\n".getBytes(StandardCharsets.UTF_8));
        certificateOut.close();
        return new String(certificateOut.toByteArray(), StandardCharsets.UTF_8);
    }
}
