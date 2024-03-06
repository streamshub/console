import type { Meta, StoryObj } from "@storybook/react";
import { DateTimePicker } from "./DateTimePicker";

export default {
  component: DateTimePicker,
} as Meta<typeof DateTimePicker>;

type Story = StoryObj<typeof DateTimePicker>;

export const Example: Story = {};

export const WithInitialValue: Story = {
  args: {
    value: "2023-01-01T00:00:00Z",
  },
};
