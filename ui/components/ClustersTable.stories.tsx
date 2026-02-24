import type { Meta, StoryObj } from "@storybook/nextjs";
import { ClustersTable } from "./ClustersTable";
import { ClusterDrawerContext } from "./ClusterDrawerContext";

const meta: Meta<typeof ClustersTable> = {
  component: ClustersTable,
  decorators: [
    (Story) => (
      <ClusterDrawerContext.Provider
        value={{
          open: () => {},
          close: () => {},
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
    id: "1",
    attributes: {
      name: "Kafka1",
      kafkaVersion: "3.5.0",
      namespace: "eyefloaters-dev",
    },
    meta: {
      configured: true,
    },
    extra: {
      nodes: Promise.resolve({
        online: 1,
        count: 3,
      }),
      groupsCount: Promise.resolve(10),
    },
  },
  {
    id: "2",
    attributes: {
      name: "kafka2",
      kafkaVersion: "3.6.0",
      namespace: "eyefloaters-dev",
    },
    meta: {
      configured: false,
    },
    extra: {
      nodes: Promise.resolve({
        online: 2,
        count: 4,
      }),
      groupsCount: Promise.resolve(5),
    },
  },
];

export const Default: Story = {
  args: {
    clusters: clustersData,
  },
};

export const Authenticated: Story = {
  args: {
    clusters: clustersData,
    authenticated: true,
  },
};
