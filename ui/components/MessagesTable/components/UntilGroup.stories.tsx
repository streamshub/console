import type { Meta, StoryObj } from "@storybook/nextjs";
import { expect, fn, within } from "storybook/test";
import { UntilGroup as Comp } from "./UntilGroup";

const meta: Meta<typeof Comp> = {
  component: Comp,
  args: {
    onLimitChange: fn(),
    onLive: fn(),
  },
};

export default meta;
type Story = StoryObj<typeof Comp>;

export const Limit: Story = {
  args: {
    limit: 50,
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    expect(canvas.getByText(args.limit, { exact: false }));
  },
};

export const Continuosly: Story = {
  args: {
    limit: "continuously",
  },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement);
    expect(canvas.findByText("Continuously"));
  },
};
