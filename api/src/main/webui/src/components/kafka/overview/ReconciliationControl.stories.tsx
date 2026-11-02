import type { Meta, StoryObj } from '@storybook/react-vite';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReconciliationControls } from './ReconciliationControls';
import type { KafkaCluster } from '@/api/types';

const makeCluster = (meta: Record<string, unknown>) =>
  ({
    id: 'cluster-1',
    type: 'kafkas',
    attributes: { name: 'my-kafka-cluster' },
    meta: {
      managed: true,
      reconciliationPaused: false,
      privileges: ['UPDATE'],
      ...meta,
    },
  } as unknown as KafkaCluster);

const meta = {
  component: ReconciliationControls,
  decorators: [
    (Story) => (
      <QueryClientProvider client={new QueryClient()}>
        <Story />
      </QueryClientProvider>
    ),
  ],
} satisfies Meta<typeof ReconciliationControls>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Paused: Story = {
  args: { cluster: makeCluster({ reconciliationPaused: true }) },
};

export const PausedReadOnly: Story = {
  args: {
    cluster: makeCluster({ reconciliationPaused: true, privileges: [] }),
  },
};
