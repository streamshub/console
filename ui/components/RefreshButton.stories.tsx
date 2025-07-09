import type { Meta, StoryObj } from "@storybook/nextjs";
import { expect, fn, userEvent, within } from "storybook/test";

import { RefreshButton } from "./RefreshButton";

export default {
  component: RefreshButton,
  args: {
    onClick: fn(),
  },
} as Meta<typeof RefreshButton>;
type Story = StoryObj<typeof RefreshButton>;

export const Default: Story = {
  args: {
    tooltip: "Reload contents",
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await userEvent.click(canvas.getByLabelText("Refresh"));
    await expect(args.onClick).toBeCalledTimes(1);
  },
};
