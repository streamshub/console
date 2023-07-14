package org.bf2.admin.kafka.admin.model;

import java.util.Comparator;

public class ConsumerGroupComparator implements Comparator<Types.ConsumerGroup> {

    private final Types.ConsumerGroupOrderKey key;
    public ConsumerGroupComparator(Types.ConsumerGroupOrderKey key) {
        this.key = key;
    }

    public ConsumerGroupComparator() {
        this.key = Types.ConsumerGroupOrderKey.NAME;
    }

    @Override
    public int compare(Types.ConsumerGroup firstConsumerGroup, Types.ConsumerGroup secondConsumerGroup) {
        if (Types.ConsumerGroupOrderKey.NAME.equals(key)) {
            if (firstConsumerGroup == null || firstConsumerGroup.getGroupId() == null
                || secondConsumerGroup == null || secondConsumerGroup.getGroupId() == null) {
                return 0;
            } else {
                return firstConsumerGroup.getGroupId().compareToIgnoreCase(secondConsumerGroup.getGroupId());
            }
        }
        return 0;
    }
}