import type { Meta, StoryObj } from '@storybook/react';
import { userEvent, within } from 'storybook/test'; // or '@storybook/test', matching your other stories
import type { Rebalance } from '@/api/types';
import { ProposalDetailCard } from './ProposalDetailCard';

const makeRebalance = (optimizationResult?: Record<string, unknown>) =>
  ({
    id: 'rebalance-1',
    attributes: {
      sessionId: 'session-abc-123',
      optimizationResult,
    },
  } as unknown as Rebalance);

const fullResult = {
  numReplicaMovements: 42,
  numLeaderMovements: 8,
  dataToMoveMB: 1536,
  recentWindows: 5,
  onDemandBalancednessScoreBefore: 72.4,
  onDemandBalancednessScoreAfter: 91.8,
  numIntraBrokerReplicaMovements: 3,
  intraBrokerDataToMoveMB: 256,
  excludedBrokersForReplicaMove: [3, 4],
  excludedBrokersForLeadership: [4],
  excludedTopics: ['__consumer_offsets', 'internal-events'],
  monitoredPartitionsPercentage: 100,
};

// Opens the collapsed section so the story shows the content right away
const expand = async ({ canvasElement }: { canvasElement: HTMLElement }) => {
  const canvas = within(canvasElement);
  await userEvent.click(canvas.getByRole('button', { name: /./ }));
};

const meta = {
  component: ProposalDetailCard,
  args: {
    rebalance: makeRebalance(fullResult),
  },
} satisfies Meta<typeof ProposalDetailCard>;

export default meta;
type Story = StoryObj<typeof meta>;

// Collapsed, as users first see it
export const Collapsed: Story = {};

export const Expanded: Story = {
  play: expand,
};

export const NoExclusions: Story = {
  args: {
    rebalance: makeRebalance({
      ...fullResult,
      excludedBrokersForReplicaMove: [],
      excludedBrokersForLeadership: [],
      excludedTopics: [],
    }),
  },
  play: expand,
};

export const PartialData: Story = {
  args: {
    rebalance: makeRebalance({
      numReplicaMovements: 10,
      dataToMoveMB: 0,
    }),
  },
  play: expand,
};

export const NoProposalData: Story = {
  args: { rebalance: makeRebalance(undefined) },
  play: expand,
};
