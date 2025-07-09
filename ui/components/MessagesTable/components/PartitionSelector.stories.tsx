import type { Meta, StoryObj } from "@storybook/nextjs";
import { expect, fn, within } from "storybook/test";
import { PartitionSelector as Comp } from "./PartitionSelector";

const meta: Meta<typeof Comp> = {
  component: Comp,
  args: {
    onChange: fn(),
    partitions: 2,
  },
};

export default meta;
type Story = StoryObj<typeof Comp>;

export const DefaultValue: Story = {
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    expect(canvas.getByText("All partitions"));
  },
};

export const WithInitialValue: Story = {
  args: {
    value: 1,
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement);
    expect(canvas.getByText("Partition 1"));
  },
};
