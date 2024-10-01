import type { Meta, StoryObj } from "@storybook/react";
import { OptimizationProposal } from "./OptimizationProposal";

const meta: Meta<typeof OptimizationProposal> = {
  component: OptimizationProposal,
};

export default meta;
type Story = StoryObj<typeof OptimizationProposal>;

export const OptimizationProposalModal: Story = {
  args: {
    isModalOpen: true,
    dataToMoveMB: 0,
    monitoredPartitionsPercentage: 100,
    numIntraBrokerReplicaMovements: 0,
    numLeaderMovements: 24,
    numReplicaMovements: 25,
    onDemandBalancednessScoreAfter: 82.65544,
    onDemandBalancednessScoreBefore: 82.6543,
    recentWindows: 5,
  },
};
