import { Meta, StoryObj } from "@storybook/react";
import { DryrunSelect } from "./DryrunSelect";

export default {
  component: DryrunSelect,
  args: {},
} as Meta<typeof DryrunSelect>;

type Story = StoryObj<typeof DryrunSelect>;

export const Default: Story = {
  args: { cliCommand: "something" },
};
