package com.github.eyefloaters.console.kafka.systemtest.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.lifecycle.Startable;

import com.github.eyefloaters.console.kafka.systemtest.Environment;

@SuppressWarnings("resource")
public class DeploymentManager {

    protected static final Logger LOGGER = Logger.getLogger(DeploymentManager.class);
    static final Map<String, String> TEST_CONTAINER_LABELS =
            Collections.singletonMap("test-ident", Environment.TEST_CONTAINER_LABEL);

    public enum UserType {
        OWNER("alice"),
        USER("susan"),
        OTHER("bob"),
        INVALID(null);

        String username;

        private UserType(String username) {
            this.username = username;
        }

        public String getUsername() {
            return username;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface InjectDeploymentManager {
    }

    private final boolean oauthEnabled;
    private final Network testNetwork;

    private GenericContainer<?> keycloakContainer;
    private KafkaContainer kafkaContainer;

    public static DeploymentManager newInstance(boolean oauthEnabled) {
        return new DeploymentManager(oauthEnabled);
    }

    private DeploymentManager(boolean oauthEnabled) {
        this.oauthEnabled = oauthEnabled;
        this.testNetwork = Network.newNetwork();
    }

    private static String name(String prefix) {
        return prefix + '-' + UUID.randomUUID().toString();
    }

    public boolean isOauthEnabled() {
        return oauthEnabled;
    }

    public void shutdown() {
        stopAll(kafkaContainer, keycloakContainer);
    }

    private void stopAll(Startable... containers) {
        for (var container : containers) {
            if (container != null) {
                container.stop();
            }
        }
    }

    public GenericContainer<?> getKeycloakContainer() {
        if (keycloakContainer == null) {
            keycloakContainer = deployKeycloak();
        }

        return keycloakContainer;
    }

    public void stopKeycloakContainer() {
        if (keycloakContainer != null) {
            keycloakContainer.stop();
            keycloakContainer = null;
        }
    }

    public KafkaContainer getKafkaContainer() {
        if (kafkaContainer == null) {
            if (oauthEnabled) {
                kafkaContainer = deployKafka();
            } else {
                kafkaContainer = deployStrimziKafka();
            }
        }

        return kafkaContainer;
    }

    public void stopKafkaContainer() {
        if (kafkaContainer != null) {
            kafkaContainer.stop();
            kafkaContainer = null;
        }
    }

    public String getExternalBootstrapServers() {
        if (kafkaContainer != null) {
            return this.kafkaContainer.getBootstrapServers();
        }

        return null;
    }

    public GenericContainer<?> deployKeycloak() {
        LOGGER.info("Deploying keycloak container");
        String imageName = System.getProperty("keycloak.image");

        GenericContainer<?> container = new GenericContainer<>(imageName)
                .withLabels(TEST_CONTAINER_LABELS)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("systemtests.keycloak"), true))
                .withCreateContainerCmdModifier(cmd -> cmd.withName(name("keycloak")))
                .withNetwork(testNetwork)
                .withNetworkAliases("keycloak")
                .withExposedPorts(8080)
                .withEnv(Map.of("KEYCLOAK_ADMIN", "admin",
                        "KEYCLOAK_ADMIN_PASSWORD", "admin",
                        "PROXY_ADDRESS_FORWARDING", "true"))
                .withClasspathResourceMapping("/keycloak/authz-realm.json", "/opt/keycloak/data/import/authz-realm.json", BindMode.READ_WRITE, SelinuxContext.SHARED)
                .withCommand("start", "--hostname=keycloak", "--hostname-strict-https=false", "--http-enabled=true", "--import-realm")
                .waitingFor(Wait.forHttp("/realms/kafka-authz").withStartupTimeout(Duration.ofMinutes(5)));

        LOGGER.info("Waiting for keycloak container");
        container.start();
        return container;
    }

    private KafkaContainer deployKafka() {
        LOGGER.info("Deploying Kafka container");

        Map<String, String> env = new HashMap<>();

        try (InputStream stream = getClass().getResourceAsStream("/kafka-oauth/env.properties")) {
            Properties envProps = new Properties();
            envProps.load(stream);
            envProps.keySet()
                .stream()
                .map(Object::toString)
                .forEach(key -> env.put(key, envProps.getProperty(key)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        String imageTag = System.getProperty("strimzi-kafka.tag");

        var container = (KafkaContainer) new KeycloakSecuredKafkaContainer(imageTag)
                .withLabels(TEST_CONTAINER_LABELS)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("systemtests.oauth-kafka"), true))
                .withCreateContainerCmdModifier(cmd -> cmd.withName(name("oauth-kafka")))
                .withEnv(env)
                .withNetwork(testNetwork)
                .withClasspathResourceMapping("/kafka-oauth/config/", "/opt/kafka/config/strimzi/", BindMode.READ_WRITE, SelinuxContext.SHARED)
                .withClasspathResourceMapping("/kafka-oauth/scripts/functions.sh", "/opt/kafka/functions.sh", BindMode.READ_WRITE, SelinuxContext.SHARED)
                .withClasspathResourceMapping("/kafka-oauth/scripts/simple_kafka_config.sh", "/opt/kafka/simple_kafka_config.sh", BindMode.READ_WRITE, SelinuxContext.SHARED)
                .withClasspathResourceMapping("/kafka-oauth/scripts/start.sh", "/opt/kafka/start.sh", BindMode.READ_WRITE, SelinuxContext.SHARED)
                .withCommand("sh", "/opt/kafka/start.sh");

        container.start();
        return container;
    }

    private KafkaContainer deployStrimziKafka() {
        LOGGER.info("Deploying Strimzi Kafka container");
        String imageTag = System.getProperty("strimzi-kafka.tag");

        var container = (KafkaContainer) new KafkaContainer(imageTag)
                .withLabels(TEST_CONTAINER_LABELS)
                .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("systemtests.plain-kafka"), true))
                .withCreateContainerCmdModifier(cmd -> cmd.withName(name("plain-kafka")))
                .withKafkaConfigurationMap(Map.of("auto.create.topics.enable", "false"))
                .withNetwork(testNetwork);

        container.start();
        return container;
    }
}
