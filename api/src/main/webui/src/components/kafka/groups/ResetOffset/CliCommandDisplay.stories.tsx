import type { Meta, StoryObj } from '@storybook/react';
import { CliCommandDisplay } from './CliCommandDisplay';

const meta: Meta<typeof CliCommandDisplay> = {
  component: CliCommandDisplay,
  parameters: {
    layout: 'padded',
  },
  args: {
    command:
      'kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group my-consumer-group --reset-offsets --to-earliest --execute',
    isExpanded: false,
  },
};

export default meta;

type Story = StoryObj<typeof meta>;

export const Default: Story = {};

export const Expanded: Story = {
  args: {
    isExpanded: true,
  },
};

export const LongCommand: Story = {
  args: {
    isExpanded: true,
    command:
      'kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group orders-consumer-group --topic orders --reset-offsets --to-datetime 2026-10-29T10:00:00.000 --execute',
  },
};
