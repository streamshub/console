import type { Meta, StoryObj } from '@storybook/react';
import { WhereSelector } from './whereSelector';

const meta: Meta<typeof WhereSelector> = {
  component: WhereSelector,
};

export default meta;
type Story = StoryObj<typeof WhereSelector>;

export const Default: Story = {
  args: {
    where: undefined,
    onWhereChange: () => {},
  },
};
