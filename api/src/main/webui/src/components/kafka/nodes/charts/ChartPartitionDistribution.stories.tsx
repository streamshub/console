import type { Meta, StoryObj } from '@storybook/react';
import { ChartPartitionDistribution } from './ChartPartitionDistribution';
import { Node } from '@/api/types';

const brokerNodes: Node[] = [
  {
    id: '1',
    type: 'nodes',
    attributes: { broker: { status: 'Running', replicaCount: 80, leaderCount: 20 } },
  },
  {
    id: '2',
    type: 'nodes',
    attributes: { broker: { status: 'Running', replicaCount: 60, leaderCount: 30 } },
  },
  {
    id: '3',
    type: 'nodes',
    attributes: { broker: { status: 'Running', replicaCount: 65, leaderCount: 45 } },
  },
];

/** 500 brokers + 9 controllers. Controllers have no broker attribute and are filtered out. */
const largeClusterNodes: Node[] = [
  ...Array.from({ length: 500 }, (_, i) => ({
    id: String(i + 1),
    type: 'nodes' as const,
    attributes: {
      broker: {
        status: 'Running' as const,
        // Vary replica and leader counts to create a realistic spread
        replicaCount: 50 + ((i * 37) % 150),
        leaderCount: 10 + ((i * 13) % 50),
      },
    },
  })),
  ...Array.from({ length: 9 }, (_, i) => ({
    id: String(501 + i),
    type: 'nodes' as const,
    attributes: { controller: { status: 'QuorumFollower' as const } },
  })),
];

const meta: Meta<typeof ChartPartitionDistribution> = {
  component: ChartPartitionDistribution,
  title: 'Kafka/Nodes/Charts/ChartPartitionDistribution',
};

export default meta;
type Story = StoryObj<typeof ChartPartitionDistribution>;

export const Default: Story = {
  args: { nodes: brokerNodes },
};

/** Renders the warning alert when no broker nodes are present. */
export const Empty: Story = {
  args: { nodes: [] },
};

/** Non-broker nodes (controllers only) are filtered out — same result as Empty. */
export const ControllerOnlyNodes: Story = {
  args: {
    nodes: [
      {
        id: '10',
        type: 'nodes',
        attributes: { controller: { status: 'QuorumLeader' } },
      },
    ],
  },
};

/** 500 brokers + 9 controllers — exercises chart height scaling and axis tick readability. */
export const LargeCluster: Story = {
  args: { nodes: largeClusterNodes },
};
