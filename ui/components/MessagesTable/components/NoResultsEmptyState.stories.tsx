import type { Meta, StoryObj } from "@storybook/nextjs";
import { expect, fn, userEvent, within } from "storybook/test";
import { NoResultsEmptyState as Comp } from "./NoResultsEmptyState";

const meta: Meta<typeof Comp> = {
  component: Comp,
  args: {
    onReset: fn(),
  },
};

export default meta;
type Story = StoryObj<typeof Comp>;

export const NoResultsEmptyState: Story = {
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    expect(canvas.getByText("No messages data"));
    await userEvent.click(canvas.getByText("Show latest messages"));
    await expect(args.onReset).toHaveBeenCalled();
  },
};
