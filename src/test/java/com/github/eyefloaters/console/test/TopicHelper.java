package com.github.eyefloaters.console.test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonObject;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicListing;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import com.github.eyefloaters.console.api.BlockingSupplier;
import com.github.eyefloaters.console.kafka.systemtest.utils.ClientsConfig;

import static org.junit.jupiter.api.Assertions.fail;

public class TopicHelper {

    static final Logger log = Logger.getLogger(TopicHelper.class);
    final URI bootstrapServers;
    final Config config;
    final String token;
    final Properties adminConfig;

    public TopicHelper(URI bootstrapServers, Config config, String token) {
        this.bootstrapServers = bootstrapServers;
        this.config = config;
        this.token = token;

        adminConfig = token != null ?
            ClientsConfig.getAdminConfigOauth(config, token) :
            ClientsConfig.getAdminConfig(config);

        adminConfig.setProperty(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers.toString());
    }

    public void deleteAllTopics() {
        // Tests assume a clean slate - remove any existing topics
        try (Admin admin = Admin.create(adminConfig)) {
            admin.listTopics()
                .listings()
                .toCompletionStage()
                .thenApply(topics -> topics.stream().map(TopicListing::name).collect(Collectors.toList()))
                .thenComposeAsync(topicNames -> {
                    log.infof("Deleting topics: %s", topicNames);
                    return admin.deleteTopics(topicNames).all().toCompletionStage();
                })
                .toCompletableFuture()
                .get(20, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Process interruptted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail(e);
        }
    }

    public Map<String, String> createTopics(String clusterId, List<String> names, int numPartitions) {
        return createTopics(clusterId, names, numPartitions, null);
    }

    public Map<String, String> createTopics(String clusterId, List<String> names, int numPartitions, Map<String, String> configs) {
        Map<String, String> topicIds = null;

        try (Admin admin = Admin.create(adminConfig)) {
            var result = admin.createTopics(names.stream()
                    .map(name ->  new NewTopic(name, Optional.of(numPartitions), Optional.empty())
                            .configs(configs))
                    .toList());

            result.all()
                .toCompletionStage()
                .thenRun(() -> log.infof("Topics created:", names))
                .toCompletableFuture()
                .get(20, TimeUnit.SECONDS);

            topicIds = names.stream().collect(Collectors.toMap(Function.identity(), name -> {
                return BlockingSupplier.get(() -> result.topicId(name)).toString();
            }));
        } catch (InterruptedException e) {
            log.warn("Process interruptted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            fail(e);
        }

        return topicIds;
    }

    public void produceRecord(String topicName, Integer partition, Instant instant, Map<String, Object> headers, String key, String value) {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers.toString());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        Long timestamp = Optional.ofNullable(instant).map(Instant::toEpochMilli).orElse(null);

        try (Producer<String, String> producer = new KafkaProducer<>(props)) {
            var record = new ProducerRecord<String, String>(topicName, partition, timestamp, key, value);
            headers.forEach((k, v) -> record.headers().add(k, v.toString().getBytes(StandardCharsets.UTF_8)));
            producer.send(record);
        } catch (Exception e) {
            fail(e);
        }
    }

    public static JsonObject buildNewTopicRequest(String name, int numPartitions, Map<String, String> config) {
        JsonArrayBuilder configBuilder = Json.createArrayBuilder();
        config.forEach((key, value) -> configBuilder.add(Json.createObjectBuilder().add(key, value)));

        return Json.createObjectBuilder()
            .add("name", name)
            .add("settings", Json.createObjectBuilder()
                 .add("numPartitions", numPartitions)
                 .add("configs", configBuilder))
            .build();
    }

    public static JsonObject buildUpdateTopicRequest(String name, int numPartitions, Map<String, String> config) {
        JsonArrayBuilder configBuilder = Json.createArrayBuilder();

        config.forEach((key, value) ->
            configBuilder.add(Json.createObjectBuilder()
                              .add("key", key)
                              .add("value", value)));

        return Json.createObjectBuilder()
            .add("name", name)
            .add("numPartitions", numPartitions)
            .add("config", configBuilder)
            .build();
    }
}
