import type { Meta, StoryObj } from "@storybook/react";
import { expect, within } from "@storybook/test";

import { Number } from "./Number";

const meta: Meta<typeof Number> = {
  component: Number,
  args: {
    value: 0, // Default value
  },
  argTypes: {
    value: {
      control: "text",
      description: "The value to be formatted",
      table: {
        type: { summary: "string | number | undefined" },
        defaultValue: { summary: "undefined" },
      },
    },
  },
};

export default meta;
type Story = StoryObj<typeof Number>;

export const Default: Story = {
  args: {
    value: 1234,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(canvas.getByText(/1,234/)).toBeInTheDocument();
  },
};

export const ZeroIsANumber: Story = {
  args: {
    value: 0,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(canvas.getByText(/0/)).toBeInTheDocument();
  },
};
export const LargeNumber: Story = {
  args: {
    value: 1000000,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(canvas.getByText(/1,000,000/)).toBeInTheDocument();
  },
};

export const NotANumber: Story = {
  args: {
    value: "definitely not a number",
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(canvas.getByText(/-/)).toBeInTheDocument();
  },
};

export const StringNumber: Story = {
  args: {
    value: "5678",
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(canvas.getByText(/5,678/)).toBeInTheDocument();
  },
};

export const UndefinedValue: Story = {
  args: {
    value: undefined,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(canvas.getByText(/-/)).toBeInTheDocument();
  },
};
