import type { Meta, StoryObj } from "@storybook/nextjs";
import { NoResultsEmptyState as Comp } from "./NoResultsEmptyState";

export default {
  component: Comp,
} as Meta<typeof Comp>;

type Story = StoryObj<typeof Comp>;

export const Default: Story = {};
