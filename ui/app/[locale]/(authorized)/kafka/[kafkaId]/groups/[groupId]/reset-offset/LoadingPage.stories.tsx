import { Meta, StoryObj } from "@storybook/nextjs";
import { LoadingPage } from "./LoadingPage";

export default {
  component: LoadingPage,
  args: {},
} as Meta<typeof LoadingPage>;

type Story = StoryObj<typeof LoadingPage>;

export const Default: Story = {
  args: {},
};
