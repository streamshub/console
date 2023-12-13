import type { ComponentStory, ComponentMeta } from "@storybook/react";

import { ChartSkeletonLoader } from "./ChartSkeletonLoader";

export default {
  component: ChartSkeletonLoader,
  args: {},
  parameters: {
    backgrounds: {
      default: "Background color 100",
    },
  },
} as ComponentMeta<typeof ChartSkeletonLoader>;

const Template: ComponentStory<typeof ChartSkeletonLoader> = (args) => (
  <ChartSkeletonLoader {...args} />
);

export const Story = Template.bind({});
Story.args = {};
