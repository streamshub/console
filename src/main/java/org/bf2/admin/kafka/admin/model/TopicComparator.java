package org.bf2.admin.kafka.admin.model;

import java.util.Comparator;

public class TopicComparator implements Comparator<Types.Topic> {

    private final Types.TopicOrderKey key;
    public TopicComparator(Types.TopicOrderKey key) {
        this.key = key;
    }

    public TopicComparator() {
        this.key = Types.TopicOrderKey.NAME;
    }
    @Override
    public int compare(Types.Topic firstTopic, Types.Topic secondTopic) {
        switch (key) {
            case NAME:
                return firstTopic.getName().compareToIgnoreCase(secondTopic.getName());

            case PARTITIONS:
                return firstTopic.getPartitions().size() - secondTopic.getPartitions().size();

            case RETENTION_BYTES:
            case RETENTION_MS:
                String keyValue = key.getValue();
                Types.ConfigEntry first = firstTopic.getConfig().stream().filter(entry -> entry.getKey().equals(keyValue)).findFirst().orElseGet(() -> null);
                Types.ConfigEntry second = secondTopic.getConfig().stream().filter(entry -> entry.getKey().equals(keyValue)).findFirst().orElseGet(() -> null);

                if (first == null || second == null || first.getValue() == null || second.getValue() == null) {
                    return 0;
                } else {
                    return Long.compare(first.getValue().equals("-1") ? Long.MAX_VALUE : Long.parseLong(first.getValue()), second.getValue().equals("-1") ? Long.MAX_VALUE : Long.parseLong(second.getValue()));
                }

            default:
                return 0;
        }
    }
}