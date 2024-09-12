import { Meta, StoryObj } from "@storybook/react";
import { LoadingPage } from "./LoadingPage";

export default {
  component: LoadingPage,
  args: {},
} as Meta<typeof LoadingPage>;

type Story = StoryObj<typeof LoadingPage>;

export const Default: Story = {
  args: {},
};
