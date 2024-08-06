import type { Meta, StoryObj } from "@storybook/react";

import { SignInPage } from "./SignInPage";

const meta: Meta<typeof SignInPage> = {
  component: SignInPage
};

export default meta;
type Story = StoryObj<typeof SignInPage>;

export const Defaul: Story = {
  args: {}
};
