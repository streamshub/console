import { Meta, StoryObj } from "@storybook/react";
import { ReconciliationModal } from "./ReconciliationModal";

export default {
  component: ReconciliationModal,
} as Meta<typeof ReconciliationModal>;

type Story = StoryObj<typeof ReconciliationModal>;

export const ReconcilationPaused: Story = {
  args: {
    isReconciliationPaused: false,
    isModalOpen: true,
  },
};

export const ReconcilationResumed: Story = {
  args: {
    isReconciliationPaused: true,
    isModalOpen: true,
  },
};
