import type { Meta, StoryObj } from "@storybook/nextjs";
import { expect, waitFor, within } from "storybook/test";

import { Bytes } from "./Bytes";

const meta: Meta<typeof Bytes> = {
  component: Bytes,
  args: {
    value: 1024, // Default args for stories
  },
  argTypes: {
    value: {
      control: "text",
      description: "The value to be formatted",
      table: {
        type: { summary: "string | number | null | undefined" },
        defaultValue: { summary: "null" },
      },
    },
  },
};

export default meta;
type Story = StoryObj<typeof Bytes>;

export const Default: Story = {
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await waitFor(() => expect(canvas.getByText("1.00 KiB")).toBeInTheDocument());
  },
};

export const BytesValue: Story = {
  args: {
    value: 1, // 1 Byte
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await waitFor(() => expect(canvas.getByText("1 B")).toBeInTheDocument());
  },
};

export const KilobytesWithDecimal: Story = {
  args: {
    value: 1536, // 1.5 KiB
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await waitFor(() =>
      expect(canvas.getByText("1.50 KiB")).toBeInTheDocument(),
    );
  },
};

export const MegabytesWithDecimal: Story = {
  args: {
    value: 1048576 * 1.5, // 1.5 MiB
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await waitFor(() =>
      expect(canvas.getByText("1.50 MiB")).toBeInTheDocument(),
    );
  },
};

export const GigabytesWithDecimal: Story = {
  args: {
    value: 1073741824 * 1.5, // 1.5 GiB
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await waitFor(() =>
      expect(canvas.getByText("1.50 GiB")).toBeInTheDocument(),
    );
  },
};

export const TerabytesWithDecimal: Story = {
  args: {
    value: 1099511627776 * 1.5, // 1.5 TB
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await waitFor(() =>
      expect(canvas.getByText("1.50 TiB")).toBeInTheDocument(),
    );
  },
};

export const NullValue: Story = {
  args: {
    value: null,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await waitFor(() => expect(canvas.getByText("-")).toBeInTheDocument());
  },
};

export const UndefinedValue: Story = {
  args: {
    value: undefined,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await waitFor(() => expect(canvas.getByText("-")).toBeInTheDocument());
  },
};

export const InvalidStringValue: Story = {
  args: {
    value: "invalid",
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    await waitFor(() => expect(canvas.getByText("-")).toBeInTheDocument());
  },
};
