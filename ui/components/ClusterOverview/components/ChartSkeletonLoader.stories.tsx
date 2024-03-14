import type { Meta, StoryObj } from "@storybook/react";

import { ChartSkeletonLoader as Comp } from "./ChartSkeletonLoader";

export default {
  component: Comp,
  args: {},
  parameters: {
    backgrounds: {
      default: "Background color 100",
    },
  },
} as Meta<typeof Comp>;

type Story = StoryObj<typeof Comp>;

export const ChartSkeletonLoader: Story = {};
