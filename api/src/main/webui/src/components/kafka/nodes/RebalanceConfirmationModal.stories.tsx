import type { Meta, StoryObj } from '@storybook/react';
import { RebalanceConfirmationModal } from './RebalanceConfirmationModal';

const meta = {
  component: RebalanceConfirmationModal,
  args: {
    isOpen: true,
    action: 'approve',
    onConfirm: () => {},
    onCancel: () => {},
  },
  argTypes: {
    action: {
      control: 'radio',
      options: ['approve', 'stop', 'refresh'],
    },
  },
} satisfies Meta<typeof RebalanceConfirmationModal>;

export default meta;

type Story = StoryObj<typeof meta>;

export const Approve: Story = {
  args: { action: 'approve' },
};

export const Stop: Story = {
  args: { action: 'stop' },
};

export const Refresh: Story = {
  args: { action: 'refresh' },
};
