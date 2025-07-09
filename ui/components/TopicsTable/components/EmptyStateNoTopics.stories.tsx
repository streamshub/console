import type { Meta, StoryObj } from "@storybook/nextjs";

import { EmptyStateNoTopics as Comp } from "./EmptyStateNoTopics";

export default {
  component: Comp,
} as Meta<typeof Comp>;
type Story = StoryObj<typeof Comp>;

export const Default: Story = {};

export const CanCreate: Story = {
  args: {
    canCreate: true,
    createHref: "#/sample",
    showLearningLinks: true,
  },
};
