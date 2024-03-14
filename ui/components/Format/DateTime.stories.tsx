import type { Meta, StoryObj } from "@storybook/react";
import { expect, waitFor, within } from "@storybook/test";
import { DateTime } from "./DateTime";

const meta: Meta<typeof DateTime> = {
  component: DateTime,
  args: {
    value: new Date(1709561281).toISOString(),
    tz: "local",
    empty: "-",
  },
  argTypes: {
    value: {
      control: "text",
      description: "The date value to be displayed",
    },
    dateStyle: {
      options: ["full", "long", "medium", "short"],
      control: { type: "select" },
      description: "Controls the date format",
    },
    timeStyle: {
      options: ["full", "long", "medium", "short"],
      control: { type: "select" },
      description: "Controls the time format",
    },
    tz: {
      options: ["UTC", "local"],
      control: { type: "radio" },
      description: "The timezone to be used",
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

export const FullDateLongTime: Story = {
  args: {
    dateStyle: "full",
    timeStyle: "long",
  },
};

export const MediumDateShortTime: Story = {
  args: {
    dateStyle: "medium",
    timeStyle: "short",
  },
};

export const ShortDateMediumTime: Story = {
  args: {
    dateStyle: "short",
    timeStyle: "medium",
  },
};

export const LongDateFullTime: Story = {
  args: {
    dateStyle: "long",
    timeStyle: "full",
  },
};

export const DateTimeString: Story = {
  args: {
    value: "2023-04-01T12:00:00Z", // A static date value
    dateStyle: "full",
    timeStyle: "long",
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await waitFor(() => {
      // Check for parts of the date and time that are likely to appear regardless of the locale.
      // This is a more flexible approach that can accommodate different locales and time zones.
      const expectedDateParts = ["2023", "April", "1"];
      expectedDateParts.forEach((part) => {
        expect(canvas.getByText(new RegExp(part, "i"))).toBeInTheDocument();
      });

      // This looks for a pattern like "12:00" without specifying AM/PM or 24-hour format,
      // which might vary by locale.
      expect(canvas.getByText(/\d{1,2}:\d{2}/)).toBeInTheDocument();
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
