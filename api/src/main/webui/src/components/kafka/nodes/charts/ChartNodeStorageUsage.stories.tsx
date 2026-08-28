import type { Meta, StoryObj } from '@storybook/react';
import { ChartNodeStorageUsage } from './ChartNodeStorageUsage';
import { Node } from '@/api/types';

const GB = 1024 ** 3;

const storageNodes: Node[] = [
  {
    id: '1',
    type: 'nodes',
    attributes: { storageUsed: 30 * GB, storageCapacity: 100 * GB },
  },
  {
    id: '2',
    type: 'nodes',
    attributes: { storageUsed: 55 * GB, storageCapacity: 100 * GB },
  },
  {
    id: '3',
    type: 'nodes',
    attributes: { storageUsed: 80 * GB, storageCapacity: 100 * GB },
  },
];

/** 500 nodes with storage + 9 controller-only nodes (no storage attributes, filtered out). */
const largeClusterNodes: Node[] = [
  ...Array.from({ length: 500 }, (_, i) => ({
    id: String(i + 1),
    type: 'nodes' as const,
    attributes: {
      // Vary used storage across the range 10–90 GB to create a realistic spread
      storageUsed: (10 + ((i * 41) % 80)) * GB,
      storageCapacity: 100 * GB,
    },
  })),
  ...Array.from({ length: 9 }, (_, i) => ({
    id: String(501 + i),
    type: 'nodes' as const,
    // No storage attributes — these are filtered out by the component
    attributes: {},
  })),
];

const meta: Meta<typeof ChartNodeStorageUsage> = {
  component: ChartNodeStorageUsage,
  title: 'Kafka/Nodes/Charts/ChartNodeStorageUsage',
};

export default meta;
type Story = StoryObj<typeof ChartNodeStorageUsage>;

export const Default: Story = {
  args: { nodes: storageNodes },
};

/** Renders the warning alert when no nodes have storage attributes. */
export const Empty: Story = {
  args: { nodes: [] },
};

/** Nodes that lack storageUsed / storageCapacity are filtered out — same result as Empty. */
export const NoStorageAttributes: Story = {
  args: {
    nodes: storageNodes.map((n) => ({ ...n, attributes: {} })),
  },
};

/** Single node — verifies chart dimensions with minimal data. */
export const SingleNode: Story = {
  args: {
    nodes: [storageNodes[0]],
  },
};

/** 500 storage nodes + 9 controller-only nodes — exercises chart height scaling and sort controls. */
export const LargeCluster: Story = {
  args: { nodes: largeClusterNodes },
};
