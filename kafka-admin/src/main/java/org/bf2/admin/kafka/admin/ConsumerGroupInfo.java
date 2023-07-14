package org.bf2.admin.kafka.admin;

import io.vertx.kafka.admin.ConsumerGroupDescription;
import io.vertx.kafka.client.common.TopicPartition;
import io.vertx.kafka.client.consumer.OffsetAndMetadata;

import java.util.Map;

class ConsumerGroupInfo {

    ConsumerGroupDescription description;
    Map<TopicPartition, OffsetAndMetadata> offsets;

    ConsumerGroupInfo(ConsumerGroupDescription description, Map<TopicPartition, OffsetAndMetadata> offsets) {
        super();
        this.description = description;
        this.offsets = offsets;
    }

    ConsumerGroupDescription getDescription() {
        return description;
    }

    Map<TopicPartition, OffsetAndMetadata> getOffsets() {
        return offsets;
    }

}
