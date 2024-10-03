import type { Meta, StoryObj } from "@storybook/react";
import { RebalancesCountCard } from "./RebalancesCountCard";

const meta: Meta<typeof RebalancesCountCard> = {
  component: RebalancesCountCard,
};

export default meta;
type Story = StoryObj<typeof RebalancesCountCard>;

export const stopRebalance: Story = {
  args: {
    TotalRebalancing: 10,
    proposalReady: 2,
    rebalancing: 5,
    ready: 0,
    stopped: 1,
  },
};
