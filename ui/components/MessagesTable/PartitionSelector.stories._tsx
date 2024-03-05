import type { ComponentMeta, ComponentStory } from "@storybook/react";
import { PartitionSelector } from "./PartitionSelector";

export default {
  component: PartitionSelector,
  args: {
    isDisabled: false,
    partitions: 9999,
    value: 3,
  },
} as ComponentMeta<typeof PartitionSelector>;

const Template: ComponentStory<typeof PartitionSelector> = (args) => (
  <PartitionSelector {...args}>todo</PartitionSelector>
);

export const Example = Template.bind({});
Example.args = {};

export const All = Template.bind({});
All.args = {
  value: undefined,
};

export const Disabled = Template.bind({});
Disabled.args = {
  isDisabled: true,
};
