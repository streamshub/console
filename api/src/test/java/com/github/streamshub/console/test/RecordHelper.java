package com.github.streamshub.console.test;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import com.github.streamshub.console.kafka.systemtest.utils.ClientsConfig;

public class RecordHelper {

    public static final String RECORDS_PATH = "/api/v1/topics/{topicName}/records";

    static final Logger log = Logger.getLogger(RecordHelper.class);
    final URI bootstrapServers;
    final Config config;
    final Properties producerConfig;

    public RecordHelper(URI bootstrapServers, Config config) {
        this.bootstrapServers = bootstrapServers;
        this.config = config;
        this.producerConfig = ClientsConfig.getProducerConfig(config);
    }

    public void produceRecord(String topicName, Instant timestamp, Map<String, Object> headers, String key, String value) {
        produceRecord(producerConfig, topicName, null, timestamp, headers, key, value);
    }

    public void produceRecord(String topicName, Integer partition, Instant timestamp, Map<String, Object> headers, String key, String value) {
        produceRecord(producerConfig, topicName, partition, timestamp, headers, key, value);
    }

    public void produceRecord(String topicName, Instant timestamp, Map<String, Object> headers, byte[] key, byte[] value) {
        Properties cfg = new Properties();
        cfg.putAll(this.producerConfig);
        cfg.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        cfg.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        produceRecord(cfg, topicName, null, timestamp, headers, key, value);
    }

    static <K, V> void produceRecord(Properties config, String topicName, Integer partition, Instant timestamp, Map<String, Object> headers, K key, V value) {
        try (Producer<K, V> producer = new KafkaProducer<>(config)) {
            Long timestampMs = timestamp != null ? timestamp.toEpochMilli() : null;
            ProducerRecord<K, V> rec = new ProducerRecord<>(topicName, partition, timestampMs, key, value);
            if (headers != null) {
                headers.forEach((k, v) -> rec.headers().add(k, String.valueOf(v).getBytes()));
            }

            CompletableFuture<Void> promise = new CompletableFuture<>();

            producer.send(rec, (metadata, error) -> {
                if (error != null) {
                    promise.completeExceptionally(error);
                } else {
                    log.infof("Record sent: { topic=%s, partition=%d, offset=%d, timestamp=%s }",
                            metadata.topic(),
                            metadata.partition(),
                            metadata.offset(),
                            metadata.hasTimestamp() ? Instant.ofEpochMilli(metadata.timestamp()) : "null");
                    promise.complete(null);
                }
            });

            promise.join();
        }
    }
}
