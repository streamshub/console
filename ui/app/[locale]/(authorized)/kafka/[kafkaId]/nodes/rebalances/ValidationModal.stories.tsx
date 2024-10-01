import type { Meta, StoryObj } from "@storybook/react";
import { ValidationModal } from "./ValidationModal";

const meta: Meta<typeof ValidationModal> = {
  component: ValidationModal,
};

export default meta;
type Story = StoryObj<typeof ValidationModal>;

export const stopRebalance: Story = {
  args: {
    isModalOpen: true,
  },
};

export const ApproveRebalance: Story = {
  args: {
    isModalOpen: true,
    status: "approve",
  },
};

export const RefreshRebalance: Story = {
  args: {
    isModalOpen: true,
    status: "approve",
  },
};
