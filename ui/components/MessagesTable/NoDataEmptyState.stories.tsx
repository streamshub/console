import type { Meta, StoryObj } from "@storybook/nextjs";
import { NoDataEmptyState } from "./NoDataEmptyState";

const meta: Meta<typeof NoDataEmptyState> = {
  component: NoDataEmptyState,
};

export default meta;
type Story = StoryObj<typeof NoDataEmptyState>;

export const Default: Story = {};
