import type { Meta, StoryObj } from "@storybook/nextjs";
import { ManagedTopicLabel } from "./ManagedTopicLabel";

const meta: Meta<typeof ManagedTopicLabel> = {
  component: ManagedTopicLabel,
};

export default meta;
type Story = StoryObj<typeof ManagedTopicLabel>;

export const Default: Story = {};
