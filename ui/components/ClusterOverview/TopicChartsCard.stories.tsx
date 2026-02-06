import type { Meta, StoryObj } from "@storybook/nextjs";
import { TopicChartsCard } from "./TopicChartsCard";

const meta: Meta<typeof TopicChartsCard> = {
  component: TopicChartsCard,
  title: "Components/TopicChartsCard",
};

export default meta;
type Story = StoryObj<typeof TopicChartsCard>;

// Helper to generate some dummy time-series data
const mockTimestamps = {
  "2024-01-01T00:00:00Z": 40,
  "2024-01-01T01:00:00Z": 50,
};

export const WithData: Story = {
  args: {
    isLoading: false,
    isVirtualKafkaCluster: false,
    kafkaId: "mock-kafka-id",
    incoming: mockTimestamps,
    outgoing: mockTimestamps,
    topicList: [
      { id: "1", name: "user-events", managed: false },
      { id: "2", name: "system-logs", managed: true }, // This is internal
      { id: "3", name: "orders-topic", managed: false },
      { id: "4", name: "__consumer_offsets", managed: true }, // This is internal
    ],
  },
};

export const Loading: Story = {
  args: {
    isLoading: true,
  },
};

export const EmptyMetrics: Story = {
  args: {
    isLoading: false,
    isVirtualKafkaCluster: false,
    kafkaId: "mock-kafka-id",
    incoming: {},
    outgoing: {},
    topicList: [],
  },
};

export const VirtualCluster: Story = {
  args: {
    ...WithData.args,
    isLoading: false,
    isVirtualKafkaCluster: true,
  },
};
