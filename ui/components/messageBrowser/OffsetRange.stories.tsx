import type { ComponentMeta, ComponentStory } from "@storybook/react";
import { OffsetRange } from "./OffsetRange";

export default {
  component: OffsetRange,
  args: {
    min: 0,
    max: 999,
  },
} as ComponentMeta<typeof OffsetRange>;

const Template: ComponentStory<typeof OffsetRange> = (args) => (
  <OffsetRange {...args}>todo</OffsetRange>
);

export const Example = Template.bind({});
Example.args = {};
