package com.github.eyefloaters.console.kafka.systemtest.utils;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.microprofile.config.Config;

import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConsumerUtils {

    final Config config;
    final String token;

    public ConsumerUtils(Config config, String token) {
        this.config = config;
        this.token = token;
    }

    public ConsumerRequest request() {
        return new ConsumerRequest();
    }

    public class ConsumerRequest {
        String groupId;
        String clientId;
        List<NewTopic> topics = new ArrayList<>();
        boolean createTopic = true;
        int messagesPerTopic = 0;
        Function<Integer, Object> messageSupplier = i -> "message-" + i;
        String keySerializer = StringSerializer.class.getName();
        String valueSerializer = StringSerializer.class.getName();
        int consumeMessages = 0;
        boolean autoClose = false;

        public ConsumerRequest topic(String topicName, int numPartitions) {
            this.topics.add(new NewTopic(topicName, numPartitions, (short) 1));
            return this;
        }

        public ConsumerRequest topic(String topicName) {
            this.topics.add(new NewTopic(topicName, 1, (short) 1));
            return this;
        }

        public ConsumerRequest createTopic(boolean createTopic) {
            this.createTopic = createTopic;
            return this;
        }

        public ConsumerRequest groupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public ConsumerRequest clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public ConsumerRequest messagesPerTopic(int messagesPerTopic) {
            this.messagesPerTopic = messagesPerTopic;
            return this;
        }

        public ConsumerRequest messageSupplier(Function<Integer, Object> messageSupplier) {
            this.messageSupplier = messageSupplier;
            return this;
        }

        public ConsumerRequest valueSerializer(String valueSerializer) {
            this.valueSerializer = valueSerializer;
            return this;
        }

        public ConsumerRequest consumeMessages(int consumeMessages) {
            this.consumeMessages = consumeMessages;
            return this;
        }

        public ConsumerRequest autoClose(boolean autoClose) {
            this.autoClose = autoClose;
            return this;
        }

        public ConsumerResponse consume() {
            return ConsumerUtils.this.consume(this, autoClose);
        }
    }

    public class ConsumerResponse implements Closeable {
        Consumer<String, String> consumer;
        List<ConsumerRecord<String, String>> records = new ArrayList<>();

        @Override
        public void close() {
            if (consumer != null) {
                consumer.close();
            }
        }

        public Consumer<String, String> consumer() {
            return consumer;
        }

        public List<ConsumerRecord<String, String>> records() {
            return records;
        }
    }

    @SuppressWarnings("resource")
    public Consumer<String, String> consume(String groupId, String topicName, String clientId, int numPartitions, boolean autoClose) {
        return request()
                .groupId(groupId)
                .topic(topicName, numPartitions)
                .clientId(clientId)
                .messagesPerTopic(1)
                .autoClose(autoClose)
                .consume()
                .consumer;
    }

    ConsumerResponse consume(ConsumerRequest consumerRequest, boolean autoClose) {
        Properties adminConfig = token != null ?
            ClientsConfig.getAdminConfigOauth(config, token) :
            ClientsConfig.getAdminConfig(config);

        ConsumerResponse response = new ConsumerResponse();

        try (Admin admin = Admin.create(adminConfig)) {
            CompletionStage<Void> initial;

            if (consumerRequest.createTopic) {
                initial = admin.createTopics(consumerRequest.topics)
                    .all()
                    .toCompletionStage();
            } else {
                initial = CompletableFuture.completedStage(null);
            }

            initial
                .thenRun(() -> produceMessages(consumerRequest))
                .thenRun(() -> consumeMessages(consumerRequest, response))
                .toCompletableFuture()
                .get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            response.close();
            throw new RuntimeException(e);
        }

        if (autoClose) {
            response.close();
        }

        return response;
    }

    void produceMessages(ConsumerRequest consumerRequest) {
        if (consumerRequest.messagesPerTopic < 1) {
            return;
        }

        Properties producerConfig = token != null ?
            ClientsConfig.getProducerConfigOauth(config, token) :
            ClientsConfig.getProducerConfig(config, consumerRequest.keySerializer, consumerRequest.valueSerializer);

        try (var producer = new KafkaProducer<String, Object>(producerConfig)) {
            for (int i = 0; i < consumerRequest.messagesPerTopic; i++) {
                for (NewTopic topic : consumerRequest.topics) {
                    producer.send(new ProducerRecord<>(topic.name(), consumerRequest.messageSupplier.apply(i))).get();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    void consumeMessages(ConsumerRequest consumerRequest, ConsumerResponse response) {
        Properties consumerConfig = token != null ?
            ClientsConfig.getConsumerConfigOauth(config, consumerRequest.groupId, token) :
            ClientsConfig.getConsumerConfig(config, consumerRequest.groupId);

        consumerConfig.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerConfig.put(CommonClientConfigs.CLIENT_ID_CONFIG, consumerRequest.clientId);

        if (consumerRequest.consumeMessages > 0) {
            consumerConfig.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, String.valueOf(consumerRequest.consumeMessages));
        }

        response.consumer = new KafkaConsumer<>(consumerConfig);

        try {
            response.consumer.subscribe(consumerRequest.topics.stream().map(NewTopic::name).collect(Collectors.toList()));

            if (consumerRequest.consumeMessages < 1 && consumerRequest.messagesPerTopic < 1) {
                var records = response.consumer.poll(Duration.ofSeconds(5));
                records.forEach(response.records::add);
            } else {
                int pollCount = 0;
                int fetchCount = consumerRequest.consumeMessages > 0 ?
                    consumerRequest.consumeMessages :
                    (consumerRequest.messagesPerTopic * consumerRequest.topics.size());

                while (response.records.size() < fetchCount && pollCount++ < 10) {
                    var records = response.consumer.poll(Duration.ofSeconds(1));
                    records.forEach(response.records::add);
                }
            }

            response.consumer.commitSync();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
