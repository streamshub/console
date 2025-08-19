import type { Meta, StoryObj } from "@storybook/nextjs";
import { ConnectClustersTable } from "./ConnectClustersTable";

export const mockConnectClusters = [
  {
    id: "connect-1",
    type: "connects",
    meta: {
      managed: true,
    },
    attributes: {
      name: "Cluster A",
      version: "3.7.0",
      replicas: 3,
    },
    relationships: {
      connectors: {
        data: [
          { type: "connectors", id: "connector-1" },
          { type: "connectors", id: "connector-2" },
        ],
      },
    },
  },
  {
    id: "connect-2",
    type: "connects",
    meta: {
      managed: false,
    },
    attributes: {
      name: "Cluster B",
      version: "3.6.1",
      replicas: 2,
    },
    relationships: {
      connectors: {
        data: [],
      },
    },
  },
];

const meta: Meta<typeof ConnectClustersTable> = {
  component: ConnectClustersTable,
  args: {
    page: 1,
    perPage: 10,
    total: mockConnectClusters.length,
    connectClusters: mockConnectClusters,
  },
};

export default meta;
type Story = StoryObj<typeof ConnectClustersTable>;

export const Default: Story = {};
