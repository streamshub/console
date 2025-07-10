import type { Meta, StoryObj } from "@storybook/nextjs";
import { expect, fn, userEvent, within } from "storybook/test";
import { LimitSelector } from "./LimitSelector";

const meta: Meta<typeof LimitSelector> = {
  component: LimitSelector,
  args: {
    value: 10,
    onChange: fn(),
  },
};

export default meta;
type Story = StoryObj<typeof LimitSelector>;

export const Default: Story = {
  play: async ({ args, canvasElement }) => {
    const canvas = within(canvasElement);
    const button = canvas.getByRole("button");
    await userEvent.click(button); // To open the select dropdown
    const option = await canvas.findByRole("option", { name: "25" });
    await userEvent.click(option);
    await expect(args.onChange).toHaveBeenCalledWith(25);
  },
};

export const InitializedWithDifferentValue: Story = {
  args: {
    value: 20, // Initial value set to 20 for this story
  },
  play: async ({ args, canvasElement }) => {
    const canvas = within(canvasElement);
    const button = canvas.getByRole("button");
    await userEvent.click(button); // To open the select dropdown
    const option = await canvas.findByRole("option", { name: "50" });
    await userEvent.click(option);
    await expect(args.onChange).toHaveBeenCalledWith(50);
  },
};
