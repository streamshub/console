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
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.eclipse.microprofile.config.Config;
import org.jboss.logging.Logger;

import com.github.streamshub.console.kafka.systemtest.utils.ClientsConfig;

public class RecordHelper implements AutoCloseable {

    public static final String RECORDS_PATH = "/api/v1/topics/{topicName}/records";

    static final Logger log = Logger.getLogger(RecordHelper.class);
    final URI bootstrapServers;
    final Config config;
    final Properties producerConfig;
    private Producer<String, String> stringProducer;
    private Producer<byte[], byte[]> byteProducer;

    public RecordHelper(URI bootstrapServers, Config config) {
        this.bootstrapServers = bootstrapServers;
        this.config = config;
        this.producerConfig = ClientsConfig.getProducerConfig(config);
    }

    private synchronized Producer<String, String> getStringProducer() {
        if (stringProducer == null) {
            stringProducer = new KafkaProducer<>(this.producerConfig);
        }
        return stringProducer;
    }

    private synchronized Producer<byte[], byte[]> getByteProducer() {
        if (byteProducer == null) {
            Properties cfg = new Properties();
            cfg.putAll(this.producerConfig);
            cfg.setProperty(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
            cfg.setProperty(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
            byteProducer = new KafkaProducer<>(cfg);
        }
        return byteProducer;
    }

    public void produceRecord(String topicName, Instant timestamp, Map<String, Object> headers, String key, String value) {
        produceRecord(topicName, null, timestamp, headers, key, value);
    }

    public void produceRecord(String topicName, Integer partition, Instant timestamp, Map<String, Object> headers, String key, String value) {
        send(getStringProducer(), topicName, partition, timestamp, headers, key, value);
    }

    public CompletableFuture<RecordMetadata> produceRecordAsync(String topicName, Instant timestamp, Map<String, Object> headers, String key, String value) {
        return produceRecordAsync(topicName, null, timestamp, headers, key, value);
    }

    public CompletableFuture<RecordMetadata> produceRecordAsync(String topicName, Integer partition, Instant timestamp, Map<String, Object> headers, String key, String value) {
        return sendAsync(getStringProducer(), topicName, partition, timestamp, headers, key, value);
    }

    public void produceRecord(String topicName, Instant timestamp, Map<String, Object> headers, byte[] key, byte[] value) {
        produceRecord(topicName, null, timestamp, headers, key, value);
    }

    public void produceRecord(String topicName, Integer partition, Instant timestamp, Map<String, Object> headers, byte[] key, byte[] value) {
        send(getByteProducer(), topicName, partition, timestamp, headers, key, value);
    }

    public CompletableFuture<RecordMetadata> produceRecordAsync(String topicName, Instant timestamp, Map<String, Object> headers, byte[] key, byte[] value) {
        return produceRecordAsync(topicName, null, timestamp, headers, key, value);
    }

    public CompletableFuture<RecordMetadata> produceRecordAsync(String topicName, Integer partition, Instant timestamp, Map<String, Object> headers, byte[] key, byte[] value) {
        return sendAsync(getByteProducer(), topicName, partition, timestamp, headers, key, value);
    }

    <K, V> CompletableFuture<RecordMetadata> sendAsync(Producer<K, V> producer, String topicName, Integer partition, Instant timestamp, Map<String, Object> headers, K key, V value) {
        Long timestampMs = timestamp != null ? timestamp.toEpochMilli() : null;
        ProducerRecord<K, V> rec = new ProducerRecord<>(topicName, partition, timestampMs, key, value);
        if (headers != null) {
            headers.forEach((k, v) -> rec.headers().add(k, v instanceof byte[] b ? b : String.valueOf(v).getBytes()));
        }

        CompletableFuture<RecordMetadata> promise = new CompletableFuture<>();

        producer.send(rec, (metadata, error) -> {
            if (error != null) {
                promise.completeExceptionally(error);
            } else {
                log.infof("Record sent: { topic=%s, partition=%d, offset=%d, timestamp=%s }",
                        metadata.topic(),
                        metadata.partition(),
                        metadata.offset(),
                        metadata.hasTimestamp() ? Instant.ofEpochMilli(metadata.timestamp()) : "null");
                promise.complete(metadata);
            }
        });

        return promise;
    }

    <K, V> void send(Producer<K, V> producer, String topicName, Integer partition, Instant timestamp, Map<String, Object> headers, K key, V value) {
        sendAsync(producer, topicName, partition, timestamp, headers, key, value).join();
    }

    @Override
    public synchronized void close() {
        if (stringProducer != null) {
            try {
                stringProducer.close();
            } catch (Exception e) {
                log.warn("Error closing string producer", e);
            }
            stringProducer = null;
        }
        if (byteProducer != null) {
            try {
                byteProducer.close();
            } catch (Exception e) {
                log.warn("Error closing byte producer", e);
            }
            byteProducer = null;
        }
    }
}
