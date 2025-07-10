import type { Meta, StoryObj } from "@storybook/nextjs";
import { EmptyStateLoading as Comp } from "./EmptyStateLoading";

const meta: Meta<typeof Comp> = {
  component: Comp,
};

export default meta;
type Story = StoryObj<typeof Comp>;

export const EmptyStateLoading: Story = {};
