import type { Meta, StoryObj } from "@storybook/nextjs";

import { TopicsPartitionsCard } from "./TopicsPartitionsCard";

const meta: Meta<typeof TopicsPartitionsCard> = {
  component: TopicsPartitionsCard,
};

export default meta;
type Story = StoryObj<typeof TopicsPartitionsCard>;

export const WithData: Story = {
  args: {
    isLoading: false,
    topicsTotal: 999999,
    topicsReplicated: 400000,
    topicsUnderReplicated: 400000,
    partitions: 999999,
  },
};
export const Loading: Story = {
  args: {
    isLoading: true,
  },
};
