import type { Meta, StoryObj } from "@storybook/react";
import { expect, fn, userEvent, within } from "@storybook/test";

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

export const Refreshing: Story = {
  args: {
    isRefreshing: true,
    tooltip: "Data is currently refreshing",
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await expect(canvas.getByRole("progressbar")).toBeTruthy();
  },
};

export const Disabled: Story = {
  args: {
    isDisabled: true,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    expect(canvas.getByLabelText("Refresh")).toBeDisabled();
    await userEvent.click(canvas.getByLabelText("Refresh"), {
      pointerEventsCheck: 0, // disable checking for the pointer event to allow clicking
    });
    await expect(args.onClick).toBeCalledTimes(0);
  },
};
