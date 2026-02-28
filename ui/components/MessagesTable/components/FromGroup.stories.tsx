import type { Meta, StoryObj } from "@storybook/nextjs";
import { expect, fn, userEvent, waitFor, within } from "storybook/test";
import { FromGroup } from "./FromGroup";

const meta: Meta<typeof FromGroup> = {
  component: FromGroup,
  args: {
    onEpochChange: fn(),
    onLatest: fn(),
    onOffsetChange: fn(),
    onTimestampChange: fn(),
  },
};

export default meta;
type Story = StoryObj<typeof FromGroup>;

export const Default: Story = {
  args: {},
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await expect(canvas.getByText("Latest messages")).toBeInTheDocument();
  },
};

export const WithOffset: Story = {
  args: {
    offset: 100,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await waitFor(() => canvas.getByText("From offset"));
    const input = canvas.getByDisplayValue("100");
    await expect(input).toBeInTheDocument();
    await userEvent.type(input, "1");
    // TODO: figure out why this isn't working
    // await expect(args.onOffsetChange).toBeCalledWith("1001");
  },
};

export const WithEpoch: Story = {
  args: {
    epoch: 1650931200,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await waitFor(() => canvas.getByText("From Unix timestamp"));
    expect(canvas.getByDisplayValue(args.epoch)).toBeVisible();
  },
};

export const WithTimestamp: Story = {
  args: {
    timestamp: "2024-03-06T09:13:18.662Z",
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    await waitFor(() => canvas.getByText("From timestamp"));
    expect(canvas.getByDisplayValue("2024-03-06")).toBeVisible();
    expect(canvas.getByDisplayValue(/\d{2}:\d{2}:\d{2}/)).toBeVisible();
  },
};
