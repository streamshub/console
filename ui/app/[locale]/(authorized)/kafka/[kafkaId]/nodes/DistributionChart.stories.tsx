import type { Meta, StoryObj } from "@storybook/react";
import { DistributionChart } from "./DistributionChart";

const sampleData = {
  1: { leaders: 20, followers: 80 },
  2: { leaders: 30, followers: 60 },
  3: { leaders: 45, followers: 65 },
};

const sampleNodesCount = {
  totalNodes: 10,
  totalBrokers: 7,
  totalControllers: 3,
  leadControllerId: "1",
};

const meta: Meta<typeof DistributionChart> = {
  component: DistributionChart,
};

export default meta;
type Story = StoryObj<typeof DistributionChart>;

export const Default: Story = {
  args: {
    data: sampleData,
    nodesCount: sampleNodesCount,
  },
};
