import type { Meta, StoryObj } from "@storybook/nextjs";

import { ErrorsAndWarnings as Comp } from "./ErrorsAndWarnings";

export default {
  component: Comp,
  args: {
    dangers: 999,
    warnings: 999,
  },
} as Meta<typeof Comp>;

type Story = StoryObj<typeof Comp>;

export const ErrorsAndWarnings: Story = {};
