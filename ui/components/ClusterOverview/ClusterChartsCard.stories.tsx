import type { Meta, StoryObj } from "@storybook/nextjs";
import { ClusterChartsCard } from "./ClusterChartsCard";

const brokers = ["broker1", "broker2"];

// Mock data for charts
const mockTimestamps = {
  "2024-01-01T00:00:00Z": 40,
  "2024-01-01T01:00:00Z": 50,
};

const sampleUsages = {
  broker1: { ...mockTimestamps },
  broker2: { ...mockTimestamps },
};

const sampleAvailable = {
  broker1: {
    "2024-01-01T00:00:00Z": 100,
    "2024-01-01T01:00:00Z": 100,
  },
  broker2: {
    "2024-01-01T00:00:00Z": 100,
    "2024-01-01T01:00:00Z": 100,
  },
};

const meta: Meta<typeof ClusterChartsCard> = {
  component: ClusterChartsCard,
};

export default meta;
type Story = StoryObj<typeof ClusterChartsCard>;

export const Default: Story = {
  args: {
    isLoading: false,
    usedDiskSpace: sampleUsages,
    availableDiskSpace: sampleAvailable,
    memoryUsage: sampleUsages,
    cpuUsage: sampleUsages,
    brokerList: brokers,
  },
};

export const Loading: Story = {
  args: {
    isLoading: true,
  },
};

export const Empty: Story = {
  args: {
    isLoading: false,
    usedDiskSpace: {},
    availableDiskSpace: {},
    memoryUsage: {},
    cpuUsage: {},
  },
};
