import type { Meta, StoryObj } from "@storybook/react";
import { ChartDiskUsage } from "./ChartDiskUsage";

const meta: Meta<typeof ChartDiskUsage> = {
  component: ChartDiskUsage,
};

export default meta;
type Story = StoryObj<typeof ChartDiskUsage>;

// Mock data: TimeSeriesMetrics = number (based on usage)
const sampleUsages = {
  "1": {
    "2024-01-01T00:00:00Z": 50,
    "2024-01-01T01:00:00Z": 60,
  },
  "2": {
    "2024-01-01T00:00:00Z": 70,
    "2024-01-01T01:00:00Z": 80,
  },
};

const sampleAvailable = {
  "1": {
    "2024-01-01T00:00:00Z": 100,
    "2024-01-01T01:00:00Z": 100,
  },
  "2": {
    "2024-01-01T00:00:00Z": 100,
    "2024-01-01T01:00:00Z": 100,
  },
};

export const Default: Story = {
  args: {
    usages: sampleUsages,
    available: sampleAvailable,
  },
};

export const NoData: Story = {
  args: {
    usages: {},
    available: {},
  },
};
