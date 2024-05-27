import type { Meta, StoryObj } from "@storybook/react";
import { DistributionChart } from "./DistributionChart";

const sampleData = {
  1: { leaders: 23, followers: 47 },
  2: { leaders: 24, followers: 46 },
  3: { leaders: 50, followers: 20 },
};

const meta: Meta<typeof DistributionChart> = {
  component: DistributionChart,
};

export default meta;
type Story = StoryObj<typeof DistributionChart>;

export const Default: Story = {
  args: {
    data: sampleData,
  },
};
