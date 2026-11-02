import type { Meta, StoryObj } from '@storybook/react-vite';
import { ReconciliationModal } from './ReconciliationModal';

const meta = {
  component: ReconciliationModal,
  args: {
    isOpen: true,
    isReconciliationPaused: false,
    isPending: false,
    onClose: () => {},
    onConfirm: () => {},
  },
} satisfies Meta<typeof ReconciliationModal>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Pause: Story = {};

export const Resume: Story = {
  args: { isReconciliationPaused: true },
};

export const Pending: Story = {
  args: { isPending: true },
};
