import type { Meta, StoryObj } from "@storybook/react";
import { ClustersTable } from "./ClustersTable";
import { ClusterDrawerContext } from "../../../app/[locale]/ClusterDrawerContext"

const meta: Meta<typeof ClustersTable> = {
  component: ClustersTable,
  decorators: [
    (Story) => (
      <ClusterDrawerContext.Provider
        value={{
          open: () => { },
          close: () => { },
          expanded: false,
          clusterId: undefined,
        }}
      >
        <Story />
      </ClusterDrawerContext.Provider>
    ),
  ],
  args: {
    clusters: [],
  },
} as Meta<typeof ClustersTable>;


export default meta;
type Story = StoryObj<typeof ClustersTable>;

const clustersData = [
  {
    id: '1',
    attributes: {
      name: 'Cluster 1',
      kafkaVersion: '2.8.0',
      namespace: 'default',
    },
    meta: {
      configured: true,
    },
    extra: {
      nodes: Promise.resolve({
        online: 3,
        count: 5,
      }),
      consumerGroupsCount: Promise.resolve(10),
    },
  },
  {
    id: '2',
    attributes: {
      name: 'Cluster 2',
      kafkaVersion: '2.7.1',
      namespace: 'development',
    },
    meta: {
      configured: true,
    },
    extra: {
      nodes: Promise.resolve({
        online: 2,
        count: 4,
      }),
      consumerGroupsCount: Promise.resolve(5),
    },
  },
];

export const Default: Story = {
  args: {
    clusters: clustersData
  },
};



