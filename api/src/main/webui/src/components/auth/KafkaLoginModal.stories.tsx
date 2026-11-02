import type { Meta, StoryObj } from '@storybook/react';
import { fn } from 'storybook/test'; // or '@storybook/test', matching your other stories
import { KafkaLoginModal } from './KafkaLoginModal';

const meta = {
  component: KafkaLoginModal,
  args: {
    isOpen: true,
    clusterName: 'production-kafka',
    authMethod: 'basic',
    onClose: fn(),
    onLogin: fn(async () => {}),
  },
} satisfies Meta<typeof KafkaLoginModal>;

export default meta;
type Story = StoryObj<typeof meta>;

export const Basic: Story = {};

export const OAuth: Story = {
  args: { authMethod: 'oauth' },
};

export const WithError: Story = {
  args: { error: 'Invalid username or password' },
};
