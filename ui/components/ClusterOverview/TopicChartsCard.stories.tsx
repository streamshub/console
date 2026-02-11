import type { Meta, StoryObj } from '@storybook/nextjs'
import { TopicChartsCard } from './TopicChartsCard'

const meta: Meta<typeof TopicChartsCard> = {
  component: TopicChartsCard,
  title: 'Components/TopicChartsCard',
}

export default meta
type Story = StoryObj<typeof TopicChartsCard>

// Helper to generate some dummy time-series data
const mockTimestamps = {
  '2024-01-01T00:00:00Z': 40,
  '2024-01-01T01:00:00Z': 50,
}

export const WithData: Story = {
  args: {
    isLoading: false,
    isVirtualKafkaCluster: false,
    kafkaId: 'mock-kafka-id',
    includeHidden: false,
    incoming: mockTimestamps,
    outgoing: mockTimestamps,
    topicList: [
      { id: '1', name: 'user-events', visibility: 'external' },
      { id: '2', name: 'system-logs', visibility: 'internal' },
      { id: '3', name: 'orders-topic', visibility: 'external' },
      { id: '4', name: '__consumer_offsets', visibility: 'internal' },
    ],
  },
}

export const WithInternalVisible: Story = {
  args: {
    isLoading: false,
    isVirtualKafkaCluster: false,
    kafkaId: 'mock-kafka-id',
    includeHidden: true,
    incoming: mockTimestamps,
    outgoing: mockTimestamps,
    topicList: [
      { id: '1', name: 'user-events', visibility: 'external' },
      { id: '2', name: 'system-logs', visibility: 'internal' },
      { id: '3', name: 'orders-topic', visibility: 'external' },
      { id: '4', name: '__consumer_offsets', visibility: 'internal' },
    ],
  },
}

export const Loading: Story = {
  args: {
    isLoading: true,
  },
}

export const EmptyMetrics: Story = {
  args: {
    isLoading: false,
    isVirtualKafkaCluster: false,
    kafkaId: 'mock-kafka-id',
    incoming: {},
    outgoing: {},
    topicList: [],
  },
}

export const VirtualCluster: Story = {
  args: {
    ...WithData.args,
    isLoading: false,
    isVirtualKafkaCluster: true,
  },
}
