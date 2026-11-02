import type { Meta, StoryObj } from '@storybook/react-vite';
import { TopicChartsCardView } from './TopicChartsCard';
import { DurationOptions } from './utils/types';

// Static mock data: ISO timestamp -> bytes per second
const incoming = {
  '2025-01-15T10:00:00Z': 5000,
  '2025-01-15T10:05:00Z': 6000,
  '2025-01-15T10:10:00Z': 5500,
  '2025-01-15T10:15:00Z': 7000,
  '2025-01-15T10:20:00Z': 6500,
};

const outgoing = {
  '2025-01-15T10:00:00Z': 3000,
  '2025-01-15T10:05:00Z': 3500,
  '2025-01-15T10:10:00Z': 3200,
  '2025-01-15T10:15:00Z': 4000,
  '2025-01-15T10:20:00Z': 3800,
};

const topics = [
  { id: 'topic-1', name: 'orders' },
  { id: 'topic-2', name: 'payments' },
  { id: 'topic-3', name: 'shipments' },
];

const internalTopic = {
  id: 'topic-4',
  name: '__consumer_offsets',
  isInternal: true,
};

const meta = {
  component: TopicChartsCardView,
  parameters: { layout: 'padded' },
  decorators: [
    (Story) => (
      <div style={{ maxWidth: 900 }}>
        <Story />
      </div>
    ),
  ],
  args: {
    filteredTopics: topics,
    hasTopics: true,
    metricsAvailable: true,
    isVirtualKafkaCluster: false,
    showInternal: false,
    onShowInternalChange: () => {},
    selectedTopicId: null,
    selectedTopicName: 'All topics',
    onTopicChange: () => {},
    selectedDuration: DurationOptions.Last5minutes,
    onDurationChange: () => {},
    isLoadingMetrics: false,
    isError: false,
    incoming,
    outgoing,
  },
} satisfies Meta<typeof TopicChartsCardView>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const SelectedTopic: Story = {
  args: { selectedTopicId: 'topic-1', selectedTopicName: 'orders' },
};

export const WithInternalTopics: Story = {
  args: { showInternal: true, filteredTopics: [...topics, internalTopic] },
};

export const Loading: Story = {
  args: { isLoadingMetrics: true },
};

export const ErrorLoadingMetrics: Story = {
  args: { isError: true },
};

export const NoTopics: Story = {
  args: { hasTopics: false, filteredTopics: [] },
};

export const MetricsUnavailable: Story = {
  args: { metricsAvailable: false },
};
