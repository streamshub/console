import type { Meta, StoryObj } from "@storybook/react";
import { expect, fn, userEvent, within } from "@storybook/test";
import { AlertContinuousMode as Comp } from "./AlertContinuousMode";

const meta: Meta<typeof Comp> = {
  component: Comp,
  args: {
    onToggle: fn(),
  },
};

export default meta;
type Story = StoryObj<typeof Comp>;

export const Paused: Story = {
  args: {
    isPaused: true,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await userEvent.click(canvas.getByText("Continue"));
    expect(args.onToggle).toHaveBeenCalledWith(!args.isPaused);
  },
};

export const NotPaused: Story = {
  args: {
    isPaused: false,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await userEvent.click(canvas.getByText("Pause"));
    expect(args.onToggle).toHaveBeenCalledWith(!args.isPaused);
  },
};
