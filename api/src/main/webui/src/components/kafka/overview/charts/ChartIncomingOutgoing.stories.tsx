import type { Meta, StoryObj } from '@storybook/react-vite';
import { ChartIncomingOutgoing } from './ChartIncomingOutgoing';
import { DurationOptions } from '@/components/kafka/overview/utils/types';

const incoming = {
  '2025-01-15T10:00:00Z': 5000,
  '2025-01-15T10:05:00Z': 6000,
  '2025-01-15T10:10:00Z': 5500,
  '2025-01-15T10:15:00Z': 7000,
  '2025-01-15T10:20:00Z': 6500,
  '2025-01-15T10:25:00Z': 6000,
};

const outgoing = {
  '2025-01-15T10:00:00Z': 3000,
  '2025-01-15T10:05:00Z': 3500,
  '2025-01-15T10:10:00Z': 3200,
  '2025-01-15T10:15:00Z': 4000,
  '2025-01-15T10:20:00Z': 3800,
  '2025-01-15T10:25:00Z': 3500,
};

const meta = {
  component: ChartIncomingOutgoing,
  decorators: [
    (Story) => (
      <div style={{ width: 800, height: 400 }}>
        <Story />
      </div>
    ),
  ],
  args: {
    incoming,
    outgoing,
    isVirtualKafkaCluster: false,
    duration: DurationOptions.Last24hours,
  },
} satisfies Meta<typeof ChartIncomingOutgoing>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const SelectedTopic: Story = {
  args: { selectedTopicName: 'orders' },
};

export const NoData: Story = {
  args: { incoming: {}, outgoing: {} },
};

export const VirtualCluster: Story = {
  args: { isVirtualKafkaCluster: true },
};
