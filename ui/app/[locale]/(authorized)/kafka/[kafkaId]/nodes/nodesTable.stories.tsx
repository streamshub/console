import type { Meta, StoryObj } from "@storybook/react";
import { NodesTable } from './NodesTable';
const sampleNodesData = [
  {
    id: "0",
    nodePool: "pool-1",
    roles: [ "controller", "broker" ],
    isLeader: true,
    brokerStatus: {
      stable: true,
      description: "Running",
    },
    controllerStatus: {
      stable: true,
      description: "Quorum Leader",
    },
    followers: 10,
    leaders: 1,
    rack: 'us-east-1',
    hostname: 'kafka1-kafka-secure-0-eyefloaters-dev.mycluster-us-east-107719-da779aef12eee96bd4161f4e402b70ec-0000.us-east.containers.appdomain.cloud',
    diskCapacity: 97.450,
    diskUsage: 3,
    kafkaVersion: "3.9.0",
  },
  {
    id: "1",
    nodePool: "pool-1",
    roles: [ "controller", "broker" ],
    isLeader: false,
    brokerStatus: {
      stable: true,
      description: "Running",
    },
    controllerStatus: {
      stable: true,
      description: "Quorum Follower",
    },
    followers: 10,
    leaders: 1,
    rack: 'us-east-2',
    hostname: 'kafka1-kafka-secure-1-eyefloaters-dev.amq-streams-public-demo-da779aef12eee96bd4161f4e402b70ec-i000.us-east.containers.appdomain.cloud',
    diskCapacity: 97.45,
    diskUsage: 3,
    kafkaVersion: "3.9.0",
  },
];

export default {
  component: NodesTable,
  args: {
    nodes: sampleNodesData
  },
} as Meta<typeof NodesTable>;
type Story = StoryObj<typeof NodesTable>;

export const BrokersTable: Story = {};
