import type { Meta, StoryObj } from '@storybook/react';
import { MemoryRouter } from 'react-router';
import { AppLayoutProvider } from '@/components/app/AppLayoutProvider';
import { KafkaClusterSidebar } from './KafkaClusterSidebar';

// Renders the sidebar at a given URL so the matching item is highlighted
const withRoute = (path: string) => (Story: React.ComponentType) =>
  (
    <MemoryRouter initialEntries={[path]}>
      <AppLayoutProvider>
        <Story />
      </AppLayoutProvider>
    </MemoryRouter>
  );

const meta = {
  component: KafkaClusterSidebar,
} satisfies Meta<typeof KafkaClusterSidebar>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Overview: Story = {
  decorators: [withRoute('/kafka/kafka-1/overview')],
};

export const Topics: Story = {
  decorators: [withRoute('/kafka/kafka-1/topics')],
};

export const TopicDetail: Story = {
  // startsWith matching keeps "Topics" active on nested pages
  decorators: [withRoute('/kafka/kafka-1/topics/topic-1/partitions')],
};

export const Nodes: Story = {
  decorators: [withRoute('/kafka/kafka-1/nodes')],
};

export const Connect: Story = {
  decorators: [withRoute('/kafka/kafka-1/connect/connectors')],
};

export const Users: Story = {
  decorators: [withRoute('/kafka/kafka-1/users')],
};

export const Groups: Story = {
  decorators: [withRoute('/kafka/kafka-1/groups')],
};
