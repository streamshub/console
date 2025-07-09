import { Meta, StoryObj } from "@storybook/nextjs";
import { PartitionsTable } from "./PartitionsTable";

const sampleTopicWithData = {
  type: "topics",
  id: "sample_topic",
  attributes: {
    status: "FullyReplicated",
    name: "sample_topic",
    configs: {
      retentionMs: "604800000",
      cleanupPolicy: "delete",
    },
    partitions: [
      {
        partition: 0,
        status: "FullyReplicated",
        leaderId: 2,
        replicas: [
          { nodeId: 2, inSync: true },
          { nodeId: 0, inSync: true },
          { nodeId: 1, inSync: true },
        ],
        leaderLocalStorage: 1024,
      },
      {
        partition: 1,
        status: "UnderReplicated",
        leaderId: 1,
        replicas: [
          { nodeId: 1, inSync: true },
          { nodeId: 2, inSync: true },
          { nodeId: 0, inSync: true },
        ],
        leaderLocalStorage: 2048,
      },
      {
        partition: 2,
        status: "FullyReplicated",
        leaderId: 0,
        replicas: [
          { nodeId: 0, inSync: true },
          { nodeId: 1, inSync: true },
          { nodeId: 2, inSync: true },
        ],
        leaderLocalStorage: 0,
      },
    ],
  },
};

const emptyPartitionsTopic = {
  type: "topics",
  id: "empty_partitions_topic",
  attributes: {
    status: "FullyReplicated",
    name: "empty_partitions_topic",
    partitions: [],
  }
};


export default {
  component: PartitionsTable,
} as Meta<typeof PartitionsTable>;

type Story = StoryObj<typeof PartitionsTable>;

export const Default: Story = {
  args: {
    topic: sampleTopicWithData,
  },
};

export const EmptyState: Story = {
  args: {
    topic: emptyPartitionsTopic,
  },
};
