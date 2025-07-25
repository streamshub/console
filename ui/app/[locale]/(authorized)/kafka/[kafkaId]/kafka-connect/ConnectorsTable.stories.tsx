import type { Meta, StoryObj } from "@storybook/nextjs";
import { ConnectorsTable } from "./ConnectorsTable";

const mockConnectors = [
  {
    id: "connector-1",
    type: "connectors",
    attributes: {
      name: "MirrorSourceConnector",
      namespace: null,
      creationTimestamp: "2023-01-01T00:00:00Z",
      type: "source",
      state: "RUNNING",
      trace: 1,
      workerId: "worker-1.cluster.local:8083",
    },
  },
  {
    id: "connector-2",
    type: "connectors",
    attributes: {
      name: "MirrorCheckpointConnector",
      namespace: null,
      creationTimestamp: "2023-01-01T00:00:00Z",
      type: "sink",
      state: "FAILED",
      trace: 2,
      workerId: "worker-2.cluster.local:8083",
    },
  },
];

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
