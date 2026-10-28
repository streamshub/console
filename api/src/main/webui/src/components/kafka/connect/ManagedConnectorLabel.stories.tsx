import type { Meta, StoryObj } from '@storybook/react';
import { ManagedConnectorLabel } from './ManagedConnectorLabel';

const meta: Meta<typeof ManagedConnectorLabel> = {
  component: ManagedConnectorLabel,
};

export default meta;

type Story = StoryObj<typeof ManagedConnectorLabel>;

export const Default: Story = {};
