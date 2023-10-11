import type { ComponentMeta, ComponentStory } from "@storybook/react";
import { LimitSelector } from "./LimitSelector";

export default {
  component: LimitSelector,
  args: {
    value: 10,
  },
} as ComponentMeta<typeof LimitSelector>;

const Template: ComponentStory<typeof LimitSelector> = (args) => (
  <LimitSelector {...args}>todo</LimitSelector>
);

export const Example = Template.bind({});
Example.args = {};
