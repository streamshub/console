import type { Meta, StoryObj } from "@storybook/nextjs";
import { expect, waitFor, within } from "storybook/test";
import { DateTime } from "./DateTime";

const meta: Meta<typeof DateTime> = {
  component: DateTime,
  args: {
    value: new Date(Date.UTC(2024, 11, 31, 23, 59, 59, 999)).toISOString(),
    empty: "-",
  },
  argTypes: {
    value: {
      control: "text",
      description: "The date value to be displayed",
    },
    utc: {
      options: [ true, false ],
      control: { type: "radio" },
      description: "Whether UTC or local timezone is used for display",
    },
    empty: {
      control: "text",
      description: "What to display if the value is falsy",
    },
  },
};

export default meta;
type Story = StoryObj<typeof DateTime>;

export const Default: Story = {};

// Tests assume the process is running with TZ='America/Los_Angeles'
export const DateTimeStringUTC: Story = {
  args: {
    value: "2025-04-01T00:00:00-07:00", // A static date value
    utc: true,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await waitFor(() => {
      // Check that the date was adjusted to UTC
      expect(canvas.getByText("2025-04-01 07:00:00Z")).toBeInTheDocument();
    });
  },
};

export const DateTimeStringLocal: Story = {
  args: {
    value: "2025-04-01T07:00:00Z", // A static date value
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await waitFor(() => {
      // Check that the date was adjusted to UTC
      expect(canvas.getByText("2025-04-01 00:00:00-07:00")).toBeInTheDocument();
    });
  },
};

export const EmptyValue: Story = {
  args: {
    value: undefined,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await waitFor(() => {
      expect(canvas.getByText("-")).toBeInTheDocument();
    });
  },
};
