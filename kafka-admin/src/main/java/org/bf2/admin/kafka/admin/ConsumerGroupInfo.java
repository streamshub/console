package org.bf2.admin.kafka.admin;

import org.apache.kafka.clients.admin.ConsumerGroupDescription;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

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
