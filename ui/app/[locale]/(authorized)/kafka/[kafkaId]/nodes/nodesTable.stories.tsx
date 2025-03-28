import type { Meta, StoryObj } from "@storybook/react";
import { NodesTable } from "./NodesTable";

const nodePoolData = {
  "pool-1": ["controller", "broker"],
  "pool-2": ["broker"],
  "pool-3": ["controller"],
};

const sampleNodesData = [
  {
    id: "0",
    type: "nodes",
    attributes: {
      host: "kafka1-kafka-secure-0-eyefloaters-dev.mycluster-us-east-107719-da779aef12eee96bd4161f4e402b70ec-0000.us-east.containers.appdomain.cloud",
      port: 9092,
      rack: "us-east-1",
      nodePool: "pool-1",
      kafkaVersion: "3.9.0",
      roles: ["controller", "broker"],
      metadataState: {
        status: "leader",
        logEndOffset: 1000,
        lag: 0,
      },
      broker: {
        status: "Running",
        replicaCount: 10,
        leaderCount: 1,
      },
      controller: {
        status: "Quorum Leader",
      },
      storageUsed: 3,
      storageCapacity: 97.45,
    },
  },
  {
    id: "1",
    type: "nodes",
    attributes: {
      host: "kafka1-kafka-secure-1-eyefloaters-dev.amq-streams-public-demo-da779aef12eee96bd4161f4e402b70ec-i000.us-east.containers.appdomain.cloud",
      port: 9092,
      rack: "us-east-2",
      nodePool: "pool-1",
      kafkaVersion: "3.9.0",
      roles: ["controller", "broker"],
      metadataState: {
        status: "follower",
        logEndOffset: 950,
        lag: 50,
      },
      broker: {
        status: "Running",
        replicaCount: 10,
        leaderCount: 1,
      },
      controller: {
        status: "Quorum Follower",
      },
      storageUsed: 3,
      storageCapacity: 97.45,
    },
  },
];

export default {
  component: NodesTable,
  args: {
    nodeList: sampleNodesData,
    nodePoolList: nodePoolData,
  },
} as Meta<typeof NodesTable>;

type Story = StoryObj<typeof NodesTable>;

export const BrokersTable: Story = {};
