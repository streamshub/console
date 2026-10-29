import type { Meta, StoryObj } from '@storybook/react-vite';
import { MemoryRouter } from 'react-router';
import { TopicsPartitionsCard } from './TopicsPartitionsCard';

const meta = {
  component: TopicsPartitionsCard,
  decorators: [
    (Story) => (
      <MemoryRouter>
        <div>
          <Story />
        </div>
      </MemoryRouter>
    ),
  ],
  args: {
    totalTopics: 12,
    totalPartitions: 48,
    fullyReplicated: 45,
    underReplicated: 2,
    offline: 1,
    isLoading: false,
  },
} satisfies Meta<typeof TopicsPartitionsCard>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const AllHealthy: Story = {
  args: { fullyReplicated: 48, underReplicated: 0, offline: 0 },
};

export const Loading: Story = {
  args: { isLoading: true },
};

export const NoData: Story = {
  args: {
    totalTopics: undefined,
    totalPartitions: undefined,
    fullyReplicated: undefined,
    underReplicated: undefined,
    offline: undefined,
  },
};
