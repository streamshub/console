import { Meta, StoryObj } from "@storybook/react";
import { OffsetSelect } from "./OffsetSelect";

export default {
  component: OffsetSelect,
  args: {}
} as Meta<typeof OffsetSelect>;

type Story = StoryObj<typeof OffsetSelect>;

export const Default: Story = {
  args: {},
};
