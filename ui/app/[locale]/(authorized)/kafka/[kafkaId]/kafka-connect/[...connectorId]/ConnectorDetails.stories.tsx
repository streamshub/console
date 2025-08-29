import type { Meta, StoryObj } from "@storybook/nextjs";
import { ConnectorDetails } from "./ConnectorDetails";

const mockConfig = {
  "connector.class": "org.apache.kafka.connect.file.FileStreamSourceConnector",
  file: "/tmp/test.txt",
  topic: "test-topic",
};

const mockTasks = [
  {
    id: "1",
    type: "connectorTasks",
    attributes: {
      taskId: 1,
      state: "RUNNING",
      workerId: "worker-123",
    },
  },
  {
    id: "2",
    type: "connectorTasks",
    attributes: {
      taskId: 2,
      state: "FAILED",
      workerId: "worker-456",
    },
  },
];

const meta: Meta<typeof ConnectorDetails> = {
  component: ConnectorDetails,
};

export default meta;

type Story = StoryObj<typeof ConnectorDetails>;

export const Default: Story = {
  args: {
    workerId: "worker-1",
    className: "FileStreamSourceConnector",
    state: "RUNNING",
    type: "source",
    topics: ["topic-a", "topic-b"],
    maxTasks: 2,
    connectorTask: mockTasks,
    config: mockConfig,
  },
};

export const EmptyTasks: Story = {
  name: "Empty Tasks",
  args: {
    workerId: "worker-empty",
    className: "FileStreamSourceConnector",
    state: "PAUSED",
    type: "sink",
    topics: [],
    maxTasks: 0,
    connectorTask: [],
    config: mockConfig,
  },
};
