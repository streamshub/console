import type { Meta, StoryObj } from "@storybook/nextjs";
import { ConnectorsTable } from "./ConnectorsTable";

const allStates = ["UNASSIGNED", "RUNNING", "PAUSED", "FAILED", "RESTARTING"];

const mockConnectors = allStates.map((state, index) => ({
  id: `connector-${index + 1}`,
  type: "connectors",
  connectClusterName: `cluster-${index + 1}`,
  attributes: {
    name: `Connector-${state}`,
    namespace: null,
    creationTimestamp: "2023-01-01T00:00:00Z",
    type: index % 2 === 0 ? "source" : "sink",
    state: state,
    trace: index,
    workerId: `worker-${index + 1}.cluster.local:8083`,
  },
  replicas: index + 1,
}));

const meta: Meta<typeof ConnectorsTable> = {
  component: ConnectorsTable,
  args: {
    page: 1,
    perPage: 10,
    total: mockConnectors.length,
    connectors: mockConnectors,
  },
};

export default meta;
type Story = StoryObj<typeof ConnectorsTable>;

export const Default: Story = {};
