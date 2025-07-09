import type { Meta, StoryObj } from "@storybook/nextjs";
import { AlertTopicGone as Comp } from "./AlertTopicGone";

const meta: Meta<typeof Comp> = {
  component: Comp,
  args: {},
};

export default meta;
type Story = StoryObj<typeof Comp>;

export const AlertTopicGone: Story = {};
